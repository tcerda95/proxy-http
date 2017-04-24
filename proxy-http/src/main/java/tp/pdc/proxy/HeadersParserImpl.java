package tp.pdc.proxy;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tp.pdc.proxy.header.Header;

public class HeadersParserImpl implements HeadersParser {

	private static final Logger LOGGER = LoggerFactory.getLogger(HeadersParserImpl.class);
	
    private static final char CR = (char) 13;
    private static final char LF = (char) 10;
    private static final char SP = (char) 32;

    @Override public boolean hasHeaderValue (Header header) {
        return relevantHeaders.containsKey(header);
    }

    @Override public byte[] getHeaderValue (Header header) {
        return relevantHeaders.get(header);
    }

    @Override public boolean hasFinished () {
        return httpParse == ParserState.READ_HEADERS && headersParse == HttpHeaderState.END_OK;
    }

    private enum ParserState {
        /* First Line */
        REQUEST_START, METHOD_READ, URI_READ, HTTP_VERSION, CR_END_LINE, CR_FIRST_LINE,

        READ_HEADERS,

        /* Error states */
        BAD_REQUEST_ERROR,
    }

    private enum HttpVersionState {
        NOT_READ_YET, H, T_1, T_2, P, MAJOR_VERSION, MINOR_VERSION, READ_OK, ERROR,
    }

    private enum HttpHeaderState {
        START, ERROR, END_LINE_CR, SECTION_END_CR, END_OK,

        NAME,
        /* Relevant headers */
        RELEVANT_COLON, RELEVANT_SPACE, RELEVANT_CONTENT,

        /* Other headers */
        COLON, SPACE, CONTENT,
    }

    private ParserState httpParse;
    private HttpVersionState versionParse;
    private HttpHeaderState headersParse;
    private int httpMajorVersion;
    private int httpMinorVersion;

    private final ByteBuffer methodName;
    private final ByteBuffer httpURI;

    private ByteBuffer headerName;
    private ByteBuffer headerValue;
    public Header currentRelevantHeader; // !!!!

    private final Map<Header, byte[]> relevantHeaders;

    public HeadersParserImpl () {
        httpParse = ParserState.REQUEST_START;
        versionParse = HttpVersionState.NOT_READ_YET;
        headersParse = HttpHeaderState.START;
        relevantHeaders = new HashMap<>();

        methodName = ByteBuffer.allocate(16);
        httpURI = ByteBuffer.allocate(256);

        headerName = ByteBuffer.allocate(128); //TODO: capacity
        headerValue = ByteBuffer.allocate(128);
    }

    // Receives buffer in read state
    public void parse(final ByteBuffer inputBuffer, final ByteBuffer outputBuffer) {

        while (inputBuffer.hasRemaining()) {
            char c = (char) inputBuffer.get();

            switch (httpParse) {
                case REQUEST_START:
                    if (isALPHA(c)) {
                        httpParse = ParserState.METHOD_READ;
                        methodName.put((byte) c);
                    } else {
                        httpParse = ParserState.BAD_REQUEST_ERROR;
                    }
                    break;

                case METHOD_READ:
                    //parseMethod();
                    if (isALPHA(c)) {
                        methodName.put((byte) c);
                    } else if (c == SP) {
                        httpParse = methodNameIsOk() ? ParserState.URI_READ : ParserState.BAD_REQUEST_ERROR;
                    } else {
                        httpParse = ParserState.BAD_REQUEST_ERROR;
                    }
                    break;

                case URI_READ:
                    //parseURI();
                    if (c == SP) {
                        httpParse = URIIsOk() ? ParserState.HTTP_VERSION : ParserState.BAD_REQUEST_ERROR;
                        httpURI.put((byte) c);
                    } else if (isURIComponent(c)) {
                        httpURI.put((byte) c);
                    } else {
                        httpParse = ParserState.BAD_REQUEST_ERROR;
                    }
                    break;

                case HTTP_VERSION:
                    parseHttpVersion(c);
                    break;

                case CR_FIRST_LINE:
                    httpParse = c == LF ? ParserState.READ_HEADERS : ParserState.BAD_REQUEST_ERROR;
                    break;

                case READ_HEADERS:
                    parseHeaders(c);
                    break;

                default:
                    httpParse = ParserState.BAD_REQUEST_ERROR;
                    return;
                // TODO: k c yo
            }
        }
    }

    private void parseHttpVersion(char c) {
        switch (versionParse) {
            case NOT_READ_YET:
                versionParse = c == 'H' ? HttpVersionState.H : HttpVersionState.ERROR;
                break;

            case H:
                versionParse = c == 'T' ? HttpVersionState.T_1 : HttpVersionState.ERROR;
                break;

            case T_1:
                versionParse = c == 'T' ? HttpVersionState.T_2 : HttpVersionState.ERROR;
                break;

            case T_2:
                versionParse = c == 'P' ? HttpVersionState.P : HttpVersionState.ERROR;
                break;

            case P:
                versionParse = c == '/' ? HttpVersionState.MAJOR_VERSION : HttpVersionState.ERROR;
                break;

            case MAJOR_VERSION:
                if (isNum(c) && c != '0') {
                    httpMajorVersion *= 10;
                    httpMajorVersion += c - '0';
                } else if (c == '.' && httpMajorVersion != 0) {
                    versionParse = HttpVersionState.MINOR_VERSION;
                } else {
                    versionParse = HttpVersionState.ERROR;
                }
                break;

            case MINOR_VERSION:
                if (isNum(c)) {
                    httpMinorVersion *= 10;
                    httpMinorVersion += c - '0';
                } else if (c == CR) {
                    versionParse = HttpVersionState.READ_OK;
                    httpParse = ParserState.CR_FIRST_LINE;
                } else {
                    versionParse = HttpVersionState.ERROR;
                }
                break;

            case READ_OK:
                // No debería recibir nada más
                versionParse = HttpVersionState.ERROR;
                break;
        }
    }

    private void parseHeaders(char c) {
        switch (headersParse) {
            case START:
                if (c == CR)
                    headersParse = HttpHeaderState.SECTION_END_CR;
                else if (isALPHA(c) || c == '-') {
                    headerName.put((byte) c);
                    headersParse = HttpHeaderState.NAME;
                }
                else
                    headersParse = HttpHeaderState.ERROR;
                break;

            case NAME:
                if (c == ':') {
                    currentRelevantHeader = Header.getHeaderFromBytes(headerName.array());
                    if (currentRelevantHeader != null) {
                        relevantHeaders.put(currentRelevantHeader, new byte[0]); // TODO: *cara sospechosa*
                        headersParse = HttpHeaderState.RELEVANT_COLON;
                    } else {
                        headersParse = HttpHeaderState.COLON;
                    }
                } else if (isALPHA(c) || c == '-') { // TODO: is not header name content
                    headerName.put((byte) Character.toLowerCase(c));
                } else {
                    headersParse = HttpHeaderState.ERROR;
                }
                break;

            case RELEVANT_COLON:
                headersParse = c == ' ' ? HttpHeaderState.RELEVANT_SPACE : HttpHeaderState.ERROR;
                break;

            case RELEVANT_SPACE:
                if (isALPHA(c)) { //TODO: ver que puede ser
                    headerValue.put((byte) Character.toLowerCase(c));
                    headersParse = HttpHeaderState.RELEVANT_CONTENT;
                } else {
                    headersParse = HttpHeaderState.ERROR;
                }
                break;

            case RELEVANT_CONTENT:
                if (isALPHA(c)) { //TODO
                    headerValue.put((byte) Character.toLowerCase(c));
                } else if (c == CR) {
                    headersParse = HttpHeaderState.END_LINE_CR;
                    relevantHeaders.put(currentRelevantHeader, headerValue.array());
                    headerName.clear();
                    headerValue.clear();
                    currentRelevantHeader = null;
                }
                break;

            case COLON:
                headersParse = c == ' ' ? HttpHeaderState.SPACE : HttpHeaderState.ERROR;
                break;

            case SPACE:
                headersParse = isALPHA(c) ? HttpHeaderState.CONTENT : HttpHeaderState.ERROR;
                break;

            case CONTENT:
                if (c == CR)
                    headersParse = HttpHeaderState.END_LINE_CR;
                else if (false)// TODO: if not header content
                    headersParse = HttpHeaderState.ERROR;
                break;

            case END_LINE_CR:
                headersParse = c == LF ? HttpHeaderState.START : HttpHeaderState.ERROR;
                break;

            case SECTION_END_CR:
                headersParse = c == LF ? HttpHeaderState.END_OK : HttpHeaderState.ERROR;
                break;

            default: // ERROR
                System.out.println(this.toString());
                throw new IllegalStateException();
        }
    }

    // TODO: Hacer bien estas cosas.
    private boolean methodNameIsOk() {
        int strLen = methodName.position(); //TODO: hacer con bytes
        String str = new String(methodName.array(), 0, strLen, ProxyProperties.getInstance().getCharset());
        methodName.flip();
        LOGGER.debug("METHOD: {}", str);
        return str.equals("GET") || str.equals("POST") || str.equals("HEAD");
    }

    private boolean URIIsOk() {
        return true;
//        httpURI.flip();
//        LOGGER.debug("URI: {}", httpURI);
//        return httpURI.toString().equals("/");
    }

    private boolean isURIComponent(final char c) {
        return c == '/';
    }

    private boolean isALPHA(final char c) {
        return Character.isLetter(c);
    }

    private boolean isNum(final char c) {
        return Character.isDigit(c);
    }

    //test
    @Override public String toString () {
        return "HeadersParser{" +
            "httpParse=" + httpParse +
            ", versionParse=" + versionParse +
            ", headersParse=" + headersParse +
            ", httpMajorVersion=" + httpMajorVersion +
            ", httpMinorVersion=" + httpMinorVersion +
            '}';
    }

    public static void main (String[] args) throws Exception {
        String s = "GET / HTTP/1.1" + CR + LF + "Host: google.com" + CR + LF
                    + "User-Agent: Internet Explorer 2" + CR + LF
                    + CR + LF;

        for (int i = 0; i < s.length(); i++) {
            System.out.println("ITERACION: " + i);

            String s1 = s.substring(0, i);
            String s2 = s.substring(i);

            HeadersParserImpl parser = new HeadersParserImpl();
            ByteBuffer buf1 = ByteBuffer.wrap(s1.getBytes(StandardCharsets.US_ASCII));
            ByteBuffer buf2 = ByteBuffer.wrap(s2.getBytes(StandardCharsets.US_ASCII));

            parser.parse(buf1, null);
            parser.parse(buf2, null);

            System.out.println(parser.toString());
            System.out.println(parser.relevantHeaders.toString());
        }

    }
}
