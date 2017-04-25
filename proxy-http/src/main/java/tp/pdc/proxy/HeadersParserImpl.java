package tp.pdc.proxy;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tp.pdc.proxy.header.Header;

public class HeadersParserImpl implements HeadersParser { //TODO: tirar excepción si hay error.

	private static final Logger LOGGER = LoggerFactory.getLogger(HeadersParserImpl.class);

    private static final char CR = (char) 13;
    private static final char LF = (char) 10;
    private static final char SP = (char) 32;
    private static final char HT = (char) 9;

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
        ERROR,
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
    private Header currentRelevantHeader;

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
                    if (ParseUtils.isAlphabetic(c)) {
                        httpParse = ParserState.METHOD_READ;
                        methodName.put((byte) c);
                    } else {
                        httpParse = ParserState.ERROR;
                    }
                    break;

                case METHOD_READ:
                    //parseMethod();
                    if (ParseUtils.isAlphabetic(c)) {
                        methodName.put((byte) c);
                    } else if (c == SP) {
                        httpParse = methodNameIsOk() ? ParserState.URI_READ : ParserState.ERROR;
                    } else {
                        httpParse = ParserState.ERROR;
                    }
                    break;

                case URI_READ:
                    //parseURI();
                    if (c == SP) {
                        httpParse = URIIsOk() ? ParserState.HTTP_VERSION : ParserState.ERROR;
                        httpURI.put((byte) c);
                    } else if (isURIComponent(c)) {
                        httpURI.put((byte) c);
                    } else {
                        httpParse = ParserState.ERROR;
                    }
                    break;

                case HTTP_VERSION:
                    parseHttpVersion(c);
                    break;

                case CR_FIRST_LINE:
                    httpParse = c == LF ? ParserState.READ_HEADERS : ParserState.ERROR;
                    break;

                case READ_HEADERS:
                    parseHeaders(c);
                    break;

                default:
                    httpParse = ParserState.ERROR;
                    return;
            }

            if (hasParseErrors())
                throw new IllegalStateException(); // TODO: Custom Exception
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
                if (ParseUtils.isDigit(c) && c != '0') {
                    httpMajorVersion *= 10;
                    httpMajorVersion += c - '0';
                } else if (c == '.' && httpMajorVersion != 0) {
                    versionParse = HttpVersionState.MINOR_VERSION;
                } else {
                    versionParse = HttpVersionState.ERROR;
                }
                break;

            case MINOR_VERSION:
                if (ParseUtils.isDigit(c)) {
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
                else if (ParseUtils.isHeaderNameChar(c)) {
                    // Reset
                    headerName.clear();
                    headerValue.clear();
                    currentRelevantHeader = null;

                    headerName.put((byte) Character.toLowerCase(c));
                    headersParse = HttpHeaderState.NAME;
                }
                else
                    headersParse = HttpHeaderState.ERROR;
                break;

            case NAME:
                if (c == ':') {
                    headerName.flip();
                    int nameLen = headerName.remaining();
                    currentRelevantHeader = Header.getByBytes(headerName, nameLen);

                    if (currentRelevantHeader != null) {
                        relevantHeaders.put(currentRelevantHeader, new byte[0]);
                        headersParse = HttpHeaderState.RELEVANT_COLON;
                    } else {
                        headersParse = HttpHeaderState.COLON;
                    }
                } else if (ParseUtils.isHeaderNameChar(c)) {
                    headerName.put((byte) Character.toLowerCase(c));
                } else {
                    headersParse = HttpHeaderState.ERROR;
                }
                break;

            case RELEVANT_COLON:
                headersParse = c == SP ? HttpHeaderState.RELEVANT_SPACE : HttpHeaderState.ERROR;
                break;

            case RELEVANT_SPACE:
                if (ParseUtils.isHeaderNameChar(c)) {
                    headerValue.put((byte) Character.toLowerCase(c));
                    headersParse = HttpHeaderState.RELEVANT_CONTENT;
                } else {
                    headersParse = HttpHeaderState.ERROR;
                }
                break;

            case RELEVANT_CONTENT:
                if (c == CR) {
                    headersParse = HttpHeaderState.END_LINE_CR;
                    headerValue.flip();
                    byte[] headerAux = new byte[headerValue.remaining()];
                    headerValue.get(headerAux);
                    relevantHeaders.put(currentRelevantHeader, headerAux);
                } else if (ParseUtils.isHeaderContentChar(c)) {
                    headerValue.put((byte) Character.toLowerCase(c));
                }
                break;

            case COLON:
                headersParse = c == SP ? HttpHeaderState.SPACE : HttpHeaderState.ERROR;
                break;

            case SPACE:
                headersParse = ParseUtils.isHeaderContentChar(c) ? HttpHeaderState.CONTENT : HttpHeaderState.ERROR;
                break;

            case CONTENT:
                if (c == CR)
                    headersParse = HttpHeaderState.END_LINE_CR;
                else if (!ParseUtils.isHeaderContentChar(c))
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

    private boolean hasParseErrors() {
        return httpParse == ParserState.ERROR || versionParse == HttpVersionState.ERROR
            || headersParse == HttpHeaderState.ERROR;
    }

    //test


    @Override public String toString () {
        return "HeadersParserImpl{" +
            "httpParse=" + httpParse +
            ", versionParse=" + versionParse +
            ", headersParse=" + headersParse +
            ", httpMajorVersion=" + httpMajorVersion +
            ", httpMinorVersion=" + httpMinorVersion +
            ", methodName=" + methodName +
            ", httpURI=" + httpURI +
            ", headerName=" + headerName +
            ", headerValue=" + headerValue +
            ", currentRelevantHeader=" + currentRelevantHeader +
            ", relevantHeaders=" + relevantHeaders +
            '}';
    }

    public static void main (String[] args) throws Exception {
        String s = "GET / HTTP/1.1" + CR + LF
                    + "Host: google.com" + CR + LF
                    + "User-Agent: Internet Explorer 2" + CR + LF
                    + "Connection: Close" + CR + LF
                    + "Transfer-encoding: Chunked" + CR + LF
                    + "Connection-no: Close" + CR + LF
                    + "Transfer-size: Chunked" + CR + LF
                    + CR + LF;

        for (int i = 0; i < 1/*s.length()*/; i++) {
            System.out.println("ITERACION: " + i);

            String s1 = s.substring(0, i);
            String s2 = s.substring(i);

            HeadersParserImpl parser = new HeadersParserImpl();
            ByteBuffer buf1 = ByteBuffer.wrap(s1.getBytes(StandardCharsets.US_ASCII));
            ByteBuffer buf2 = ByteBuffer.wrap(s2.getBytes(StandardCharsets.US_ASCII));

            parser.parse(buf1, null);
            parser.parse(buf2, null);

            System.out.println(parser.toString());

            for (Map.Entry<Header, byte[]> e: parser.relevantHeaders.entrySet()) {
                System.out.println("KEY: " + new String(e. getKey().name()));
                System.out.println("VALUE: " + new String(e.getValue()));
            }
        }

    }
}
