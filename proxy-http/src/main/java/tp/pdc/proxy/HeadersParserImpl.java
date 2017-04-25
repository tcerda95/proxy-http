package tp.pdc.proxy;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tp.pdc.proxy.header.Header;
import tp.pdc.proxy.header.Method;

public class HeadersParserImpl implements HeadersParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(HeadersParserImpl.class);

    private static final char CR = 13;
    private static final char LF = 10;
    private static final char SP = 32;
    private static final char HT = 9;

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
    private Method method;

    private final ByteBuffer methodName;
    private final ByteBuffer httpURI;

    private ByteBuffer headerName;
    private ByteBuffer headerValue;
    private Header currentRelevantHeader;

    private final Map<Header, byte[]> relevantHeaders;

    private ByteBuffer output;

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
        output = outputBuffer;
        while (inputBuffer.hasRemaining()) {
            byte c = inputBuffer.get();

            switch (httpParse) {
                case REQUEST_START:
                    if (ParseUtils.isAlphabetic(c)) {
                        httpParse = ParserState.METHOD_READ;
                        methodName.put(c);
                    } else {
                        httpParse = ParserState.ERROR;
                    }
                    break;

                case METHOD_READ:
                    //parseMethod();
                    if (ParseUtils.isAlphabetic(c)) {
                        methodName.put(c);
                    } else if (c == SP && processMethod()) {
                        httpParse = ParserState.URI_READ;
                        output.put(methodName).put(c);
                    } else {
                        httpParse = ParserState.ERROR;
                    }
                    break;

                case URI_READ:
                    //parseURI();
                    if (c == SP) {
                        httpParse = URIIsOk() ? ParserState.HTTP_VERSION : ParserState.ERROR;
                        httpURI.flip();
                        output.put(httpURI).put(c);
                    } else if (isURIComponent(c)) {
                        httpURI.put(c);
                    } else {
                        httpParse = ParserState.ERROR;
                    }
                    break;

                case HTTP_VERSION:
                    parseHttpVersion(c);
                    break;

                case CR_FIRST_LINE:
                    if (c == LF) {
                        httpParse = ParserState.READ_HEADERS;
                        output.put(c);
                    } else {
                        httpParse = ParserState.ERROR;
                    }
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

    private void parseHttpVersion(byte c) {
        switch (versionParse) {
            case NOT_READ_YET:
                expectByteAndOutput(c, (byte) 'H', HttpVersionState.H);
                break;

            case H:
                expectByteAndOutput(c, (byte) 'T', HttpVersionState.T_1);
                break;

            case T_1:
                expectByteAndOutput(c, (byte) 'T', HttpVersionState.T_2);
                break;

            case T_2:
                expectByteAndOutput(c, (byte) 'P', HttpVersionState.P);
                break;

            case P:
                expectByteAndOutput(c, (byte) '/', HttpVersionState.MAJOR_VERSION);
                break;

            case MAJOR_VERSION:
                if (ParseUtils.isDigit(c) && c != (byte) '0') {
                    httpMajorVersion *= 10;
                    httpMajorVersion += c - (byte) '0';
                    output.put(c);
                } else if (c == (byte) '.' && httpMajorVersion != 0) {
                    versionParse = HttpVersionState.MINOR_VERSION;
                    output.put(c);
                } else {
                    versionParse = HttpVersionState.ERROR;
                }
                break;

            case MINOR_VERSION:
                if (ParseUtils.isDigit(c)) {
                    httpMinorVersion *= 10;
                    httpMinorVersion += c - (byte) '0';
                    output.put(c);
                } else if (c == CR) {
                    versionParse = HttpVersionState.READ_OK;
                    httpParse = ParserState.CR_FIRST_LINE;
                    output.put(c);
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

    private void expectByteAndOutput(byte read, byte expected, HttpVersionState next) {
        if (read == expected) {
            output.put(read);
            versionParse = next;
        } else {
            versionParse = HttpVersionState.ERROR;
        }
    }

    // TODO: repite código
    private void expectByteAndOutput(byte read, byte expected, HttpHeaderState next) {
        if (read == expected) {
            output.put(read);
            headersParse = next;
        } else {
            headersParse = HttpHeaderState.ERROR;
        }
    }

    private void parseHeaders(byte c) {
        switch (headersParse) {
            case START:
                if (c == CR){
                    headersParse = HttpHeaderState.SECTION_END_CR;
                    output.put(c);
                }
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

                    // if !unwantedHeader
                    output.put(headerName).put(c);

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
                expectByteAndOutput(c, (byte) SP, HttpHeaderState.RELEVANT_SPACE);
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
                    output.put(headerAux).put((byte) CR);
                    relevantHeaders.put(currentRelevantHeader, headerAux);
                } else if (ParseUtils.isHeaderContentChar(c)) {
                    headerValue.put((byte) Character.toLowerCase(c));
                }
                break;

            case COLON:
                expectByteAndOutput(c, (byte) SP, HttpHeaderState.SPACE);
                break;

            case SPACE:
                if (ParseUtils.isHeaderContentChar(c)) {
                    headersParse = HttpHeaderState.CONTENT;
                    output.put(c);
                } else {
                    headersParse = HttpHeaderState.ERROR;
                }
                break;

            case CONTENT:
                if (c == CR) {
                    headersParse = HttpHeaderState.END_LINE_CR;
                    output.put(c);
                } else if (ParseUtils.isHeaderContentChar(c)) {
                    output.put(c);
                } else {
                    headersParse = HttpHeaderState.ERROR;
                }
                break;

            case END_LINE_CR:
                expectByteAndOutput(c, (byte) LF, HttpHeaderState.START);
                break;

            case SECTION_END_CR:
                expectByteAndOutput(c, (byte) LF, HttpHeaderState.END_OK);
                break;

            default: // ERROR
                System.out.println(this.toString());
                throw new IllegalStateException();
        }
    }

    private boolean processMethod () {
        int strLen = methodName.position();
        methodName.flip();
        method = Method.getByBytes(methodName, strLen);
        LOGGER.debug("METHOD: {}", method);
        return method != null;
    }

    private boolean URIIsOk() {
        return true;
        //        httpURI.flip();
        //        LOGGER.debug("URI: {}", httpURI);
        //        return httpURI.toString().equals("/");
    }

    private boolean isURIComponent(final byte c) {
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

            parser.parse(buf1, ByteBuffer.allocate(4096));
            parser.parse(buf2, ByteBuffer.allocate(4096));

            System.out.println(parser.toString());

            for (Map.Entry<Header, byte[]> e: parser.relevantHeaders.entrySet()) {
                System.out.println("KEY: " + e. getKey().name());
                System.out.println("VALUE: " + new String(e.getValue()));
            }
        }

        ByteBuffer input = ByteBuffer.wrap(s.getBytes(StandardCharsets.US_ASCII));
        ByteBuffer output = ByteBuffer.allocate(input.capacity());
        new HeadersParserImpl().parse(input, output);

        System.out.println(input);
        System.out.println(output);
    }

	@Override
	public boolean hasMethod(Method method) {
		return this.method == method;
	}
}
