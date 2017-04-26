package tp.pdc.proxy.parser;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static tp.pdc.proxy.parser.utils.AsciiConstants.*;
import tp.pdc.proxy.exceptions.ParserFormatException;
import tp.pdc.proxy.header.Header;
import tp.pdc.proxy.header.Method;
import tp.pdc.proxy.parser.interfaces.HttpRequestParser;
import tp.pdc.proxy.parser.interfaces.HttpVersionParser;
import tp.pdc.proxy.parser.utils.ParseUtils;

public class HttpRequestParserImpl implements HttpRequestParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpRequestParserImpl.class);


    @Override public boolean hasHeaderValue (Header header) {
        return headersParser.hasRelevantHeaderValue(header);
    }

    @Override public byte[] getHeaderValue (Header header) {
        return headersParser.getRelevantHeaderValue(header);
    }

    @Override public boolean hasFinished () {
        return requestState == RequestParserState.READ_HEADERS && headersParser.hasFinished();
    }

    private enum RequestParserState {
        /* First Line */
        REQUEST_START, METHOD_READ, URI_READ, HTTP_VERSION, CR_END_LINE, CR_FIRST_LINE,

        READ_HEADERS,

        /* Error states */
        ERROR,
    }

    private RequestParserState requestState;
    private Method method;

    private final ByteBuffer methodName;
    private final ByteBuffer httpURI;

    private ByteBuffer output;

    private HttpHeadersParserImpl headersParser;
    private HttpVersionParser versionParser;

    public HttpRequestParserImpl () {
        requestState = RequestParserState.REQUEST_START;
        methodName = ByteBuffer.allocate(16);
        httpURI = ByteBuffer.allocate(256);
        headersParser = new HttpHeadersParserImpl();
        versionParser = new HttpVersionParserImpl(CR.getValue());
    }

    // Receives buffer in read state
    public void parse(final ByteBuffer inputBuffer, final ByteBuffer outputBuffer) throws ParserFormatException {
        output = outputBuffer;
        while (inputBuffer.hasRemaining()) {
//            inputBuffer.mark();
            byte c = inputBuffer.get();

            switch (requestState) {
                case REQUEST_START:
                    if (ParseUtils.isAlphabetic(c)) {
                        requestState = RequestParserState.METHOD_READ;
                        methodName.put(c);
                    } else {
                        handleError(requestState);
                    }
                    break;

                case METHOD_READ:
                    if (ParseUtils.isAlphabetic(c)) {
                        methodName.put(c);
                    } else if (c == SP.getValue() && processMethod()) {
                        requestState = RequestParserState.URI_READ;
                        output.put(methodName).put(c);
                    } else {
                        handleError(requestState);
                    }
                    break;

                case URI_READ:
                    //parseURI();
                    if (c == SP.getValue()) {
                        requestState = URIIsOk() ? RequestParserState.HTTP_VERSION : RequestParserState.ERROR;
                        httpURI.flip();
                        output.put(httpURI).put(c);
                    } else if (isURIComponent(c)) {
                        httpURI.put(c);
                    } else {
                        handleError(requestState);
                    }
                    break;

                case HTTP_VERSION:
                    if (versionParser.parse(c, outputBuffer)) {
                        requestState = RequestParserState.CR_FIRST_LINE;
                    }
                    break;

                case CR_FIRST_LINE:
                    if (c == LF.getValue()) {
                        requestState = RequestParserState.READ_HEADERS;
                        output.put(c);
                    } else {
                        handleError(requestState);
                    }
                    break;

                case READ_HEADERS:
//                    inputBuffer.reset();
                    headersParser.parse(c,output);
                    break;

                default:
                    handleError(requestState);
                    return;
            }

            if (hasParseErrors())
                throw new IllegalStateException(); // TODO: Custom Exception
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
        return requestState == RequestParserState.ERROR || versionParser.hasError()
            || headersParser.hasError();

    }

    private void handleError(RequestParserState parserState) throws ParserFormatException {
        parserState = RequestParserState.ERROR;
        throw new ParserFormatException("Error while parsing");
    }

    //test
/*    @Override public String toString () {
        return "HttpRequestParserImpl{" +
            "requestState=" + requestState +
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

            HttpRequestParserImpl parser = new HttpRequestParserImpl();
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
        new HttpRequestParserImpl().parse(input, output);

        System.out.println(input);
        System.out.println(output);
    }

	@Override
	public boolean hasMethod(Method method) {
		return method == this.method;
	}
}