package tp.pdc.proxy;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tp.pdc.proxy.exceptions.ParserFormatException;
import tp.pdc.proxy.header.Header;
import tp.pdc.proxy.header.Method;

public class HeadersParserImpl implements HeadersParser, AsciiConstants {

    private static final Logger LOGGER = LoggerFactory.getLogger(HeadersParserImpl.class);


    @Override public boolean hasHeaderValue (Header header) {
        return headersParser.hasHeaderValue(header);
    }

    @Override public byte[] getHeaderValue (Header header) {
        return headersParser.getRelevantHeader(header);
    }

    @Override public boolean hasFinished () {
        return httpParse == ParserState.READ_HEADERS && headersParser.hasFinished();
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

    private ParserState httpParse;
    private HttpVersionState versionParse;
    private int httpMajorVersion;
    private int httpMinorVersion;
    private Method method;

    private final ByteBuffer methodName;
    private final ByteBuffer httpURI;

    private ByteBuffer output;

    private HttpHeadersParser headersParser;

    public HeadersParserImpl () {
        httpParse = ParserState.REQUEST_START;
        versionParse = HttpVersionState.NOT_READ_YET;
        methodName = ByteBuffer.allocate(16);
        httpURI = ByteBuffer.allocate(256);
        headersParser = new HttpHeadersParser();

    }

    // Receives buffer in read state
    public void parse(final ByteBuffer inputBuffer, final ByteBuffer outputBuffer) throws ParserFormatException {
        output = outputBuffer;
        while (inputBuffer.hasRemaining()) {
            byte c = inputBuffer.get();

            switch (httpParse) {
                case REQUEST_START:
                    if (ParseUtils.isAlphabetic(c)) {
                        httpParse = ParserState.METHOD_READ;
                        methodName.put(c);
                    } else {
                        handleError(httpParse);
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
                        handleError(httpParse);
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
                        handleError(httpParse);
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
                        handleError(httpParse);
                    }
                    break;

                case READ_HEADERS:
                    headersParser.parseHeaders(c,output);
                    break;

                default:
                    handleError(httpParse);
                    return;
            }

            if (hasParseErrors())
                throw new IllegalStateException(); // TODO: Custom Exception
        }
    }

    private void parseHttpVersion(byte c) throws ParserFormatException {
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
                    handleError(versionParse);
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
                    handleError(versionParse);
                }
                break;

            case READ_OK:
                // No debería recibir nada más
                handleError(versionParse);
                break;
        }
    }

    private void expectByteAndOutput(byte read, byte expected, HttpVersionState next) throws ParserFormatException {
        if (read == expected) {
            output.put(read);
            versionParse = next;
        } else {
            handleError(versionParse);
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
            || headersParser.hasError();

    }


    private void handleError(HttpVersionState versionState) throws ParserFormatException {
        versionState = HttpVersionState.ERROR;
        throw new ParserFormatException("Error while parsing version");
    }

    private void handleError(ParserState parserState) throws ParserFormatException {
        parserState = ParserState.ERROR;
        throw new ParserFormatException("Error while parsing");
    }

    //test
/*    @Override public String toString () {
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
    }*/

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

            for (Map.Entry<Header, byte[]> e: parser.headersParser.getRelevantHeaders().entrySet()) {
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
}
