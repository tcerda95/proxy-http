package tp.pdc.proxy;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HeadersParserImpl {

	private static final Logger LOGGER = LoggerFactory.getLogger(HeadersParserImpl.class);
	
    private static final char CR = (char) 13;
    private static final char LF = (char) 10;
    private static final char SP = (char) 32;

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
        START, OTHER_HEADER, OTHER_SPACE, OTHER_CONTENT, COLON, ERROR, END_LINE_CR, SECTION_END_CR, END_OK,

        /* Host Header */
        H, O, S, T, HOST_COLON, HOST_SPACE, HOST_CONTENT,
    }

    private ParserState httpParse;
    private HttpVersionState versionParse;
    private HttpHeaderState headersParse;
    private int httpMajorVersion;
    private int httpMinorVersion;

    private final CharBuffer methodName;
    private final CharBuffer httpURI;

    private final ByteBuffer hostname;
    private boolean hostRead;

    public HeadersParserImpl () {
        httpParse = ParserState.REQUEST_START;
        versionParse = HttpVersionState.NOT_READ_YET;
        headersParse = HttpHeaderState.START;
        hostRead = false;

        hostname = ByteBuffer.allocate(128);
        methodName = CharBuffer.allocate(16);
        httpURI = CharBuffer.allocate(256);
    }

    // Receives buffer in read state
    public void parse(final ByteBuffer readBuffer) {

        while (readBuffer.hasRemaining()) {
            char c = (char) readBuffer.get();

            switch (httpParse) {
                case REQUEST_START:
                    if (isALPHA(c)) {
                        httpParse = ParserState.METHOD_READ;
                        methodName.append(c);
                    } else {
                        httpParse = ParserState.BAD_REQUEST_ERROR;
                    }
                    break;

                case METHOD_READ:
                    //parseMethod();
                    if (isALPHA(c)) {
                        methodName.put(c);
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
                        httpURI.put(c);
                    } else if (isURIComponent(c)) {
                        httpURI.put(c);
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
                else
                    headerNameExpect(c, 'H', HttpHeaderState.H);
                break;

            case H:
                headerNameExpect(c, 'o', HttpHeaderState.O);
                break;

            case O:
                headerNameExpect(c, 's', HttpHeaderState.S);
                break;

            case S:
                headerNameExpect(c, 't', HttpHeaderState.T);
                break;

            case T:
                headerNameExpect(c, ':', HttpHeaderState.HOST_COLON);
                break;

            case HOST_COLON:
                headersParse = c == ' ' ? HttpHeaderState.HOST_SPACE : HttpHeaderState.ERROR;
                break;

            case HOST_SPACE:
                if (isALPHA(c)) {
                    headersParse = HttpHeaderState.HOST_CONTENT;
                    hostname.put((byte) c);
                } else {
                    headersParse = HttpHeaderState.ERROR;
                }
                break;

            case OTHER_HEADER:
                if (c == ':')
                    headersParse = HttpHeaderState.COLON;
                else if (!isALPHA(c) && c != '-') // TODO: is not header name content
                    headersParse = HttpHeaderState.ERROR;
                break;

            case COLON:
                headersParse = c == ' ' ? HttpHeaderState.OTHER_SPACE : HttpHeaderState.ERROR;
                break;

            case OTHER_SPACE:
                headersParse = isALPHA(c) ? HttpHeaderState.OTHER_CONTENT : HttpHeaderState.ERROR;
                break;

            case OTHER_CONTENT:
                if (c == CR)
                    headersParse = HttpHeaderState.END_LINE_CR;
                else if (false)// TODO: if not header content
                    headersParse = HttpHeaderState.ERROR;
                break;

            case HOST_CONTENT:
                if (c != CR) { // isALPHA(c)) { TODO: is host content
                    hostname.put((byte) c);
                } else if (c == CR) {
                    hostRead = true;
                    headersParse = HttpHeaderState.END_LINE_CR;
                } else {
                    headersParse = HttpHeaderState.ERROR;
                }
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

    private void headerNameExpect (char toProcess, char expected, HttpHeaderState nextState) {
        if (toProcess == expected)
            headersParse = nextState;
        else if (isALPHA(toProcess) || toProcess == '-') // TODO: is headerName char
            headersParse = HttpHeaderState.OTHER_HEADER;
        else
            headersParse = HttpHeaderState.ERROR;
    }

    public boolean readHost() {
        return hostRead;
    }

    //TODO test
    public String getHostName() {
        if (!readHost()) {
            throw new IllegalStateException(); // TODO: NoHostnameReadException
        }
        hostname.flip();
        return ProxyProperties.getInstance().getCharset().decode(hostname).toString();
    }

    public boolean hasError() {
        return httpParse == ParserState.BAD_REQUEST_ERROR;
    }

    // TODO: Hacer bien estas cosas.
    private boolean methodNameIsOk() {
        methodName.flip();
        LOGGER.debug("METHOD: {}", methodName.toString());
        return methodName.toString().equals("GET") || methodName.equals("POST") || methodName.equals("HEAD");
    }

    private boolean URIIsOk() {
        httpURI.flip();
        LOGGER.debug("URI: {}", httpURI);
        return httpURI.toString().equals("/");
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
            ", hostRead=" + hostRead +
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

            parser.parse(buf1);
            parser.parse(buf2);

            System.out.println(parser.toString());

            if (parser.readHost()) {
                System.out.println("HOST READ: " + parser.getHostName());
            }
        }

    }
}
