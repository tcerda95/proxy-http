package tp.pdc.proxy.parser;

import java.nio.ByteBuffer;

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
        return requestState == RequestParserState.READ_OK;
    }

    private enum RequestParserState {
        /* First Line */
        REQUEST_START, METHOD_READ, HTTP_VERSION, CR_FIRST_LINE,

        /* URI */
        URI_READ, HOST_PROTOCOL, URI_HOST_ADDR, URI_NO_HOST, URI_HOST_SLASH,

        READ_HEADERS, READ_OK,

        /* Error states */
        ERROR,
    }

    private RequestParserState requestState;
    private Method method;

    private final ByteBuffer methodName, URIHost;

    private boolean gotHostFromURI;

    private HttpHeadersParserImpl headersParser;
    private HttpVersionParser versionParser;

    public HttpRequestParserImpl () {
        requestState = RequestParserState.REQUEST_START;
        methodName = ByteBuffer.allocate(16);
        URIHost = ByteBuffer.allocate(256);
        headersParser = new HttpHeadersParserImpl();
        versionParser = new HttpVersionParserImpl(CR.getValue());
    }

    public boolean parse(final ByteBuffer input, final ByteBuffer output) throws ParserFormatException {
        while (input.hasRemaining()) {
            byte c = input.get();

            switch (requestState) {
                case REQUEST_START:
                    if (ParseUtils.isAlphabetic(c)) {
                        requestState = RequestParserState.METHOD_READ;
                        methodName.put(c);
                    } else {
                        handleError();
                    }
                    break;

                case METHOD_READ:
                    if (ParseUtils.isAlphabetic(c)) {
                        methodName.put(c);
                    } else if (c == SP.getValue() && processMethod()) {
                        requestState = RequestParserState.URI_READ;
                        output.put(methodName).put(c);
                    } else {
                        handleError();
                    }
                    break;

                case URI_READ:
                    if (c == '/') {
                        output.put(c);
                        requestState = RequestParserState.URI_NO_HOST;
                    } else if (Character.toLowerCase(c) == 'h') { // Has protocol so it has host.
                        URIHost.put(c);
                        requestState = RequestParserState.HOST_PROTOCOL;
                    } else {
                        handleError();
                    }
                    break;

                case URI_NO_HOST:
                    if (c == SP.getValue()) {
                        requestState = RequestParserState.HTTP_VERSION;
                        output.put(c);
                    } else if (ParseUtils.isUriCharacter(c)) {
                        output.put(c);
                    } else {
                        handleError();
                    }
                    break;

                case HOST_PROTOCOL:
                    if (c == '/') {
                        requestState = RequestParserState.URI_HOST_SLASH;
                        URIHost.put(c);
                    } else if (ParseUtils.isUriCharacter(c)) {
                        URIHost.put(c);
                    } else {
                        handleError();
                    }
                    break;

                case URI_HOST_SLASH:
                    if (c == '/') {
                        requestState = RequestParserState.URI_HOST_ADDR;
                        URIHost.put(c);
                    } else {
                        handleError();
                    }
                    break;

                case URI_HOST_ADDR:
                    if (c == SP.getValue() || c == '/') {
                        gotHostFromURI = true;
                        URIHost.flip();
                        output.put(URIHost).put(c);
                        requestState = c == '/' ? RequestParserState.URI_NO_HOST : RequestParserState.HTTP_VERSION;
                    } else if (ParseUtils.isUriCharacter(c)) {
                        URIHost.put(c);
                    } else {
                        handleError();
                    }
                    break;

                case HTTP_VERSION:
                    if (versionParser.parse(c, output)) {
                        requestState = RequestParserState.CR_FIRST_LINE;
                    }
                    break;

                case CR_FIRST_LINE:
                    if (c == LF.getValue()) {
                        requestState = RequestParserState.READ_HEADERS;
                        output.put(c);
                    } else {
                        handleError();
                    }
                    break;

                case READ_HEADERS:
                    if (headersParser.parse(c, output)) {
                        requestState = RequestParserState.READ_OK;
                        return true;
                    }
                    break;

                default:
                    handleError();
            }
        }
        return false;
    }

    private boolean processMethod () {
        int strLen = methodName.position();
        methodName.flip();
        method = Method.getByBytes(methodName, strLen);
        LOGGER.debug("METHOD: {}", method);
        return method != null;
    }

    //    A server
    //    SHOULD return 414 (Request-URI Too Long) status if a URI is longer
    //    than the server can handle

    private void handleError() throws ParserFormatException {
        requestState = RequestParserState.ERROR;
        throw new ParserFormatException("Error while parsing");
    }

    @Override
    public boolean hasMethod(Method method) {
        return method == this.method;
    }

    @Override
    public boolean hasHost () {
        return gotHostFromURI || hasHeaderValue(Header.HOST);
    }

    @Override
    public byte[] getHostValue() {
        if (!hasHost()) {
            throw new IllegalStateException(); //TODO
        }
        byte[] host;
        if (gotHostFromURI) {
            URIHost.flip();
            host = new byte[URIHost.remaining()];
            URIHost.get(host);
        } else {
            host = getHeaderValue(Header.HOST);
        }
        return host;
    }
}
