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
        return requestState == RequestParserState.READ_OK;
    }

    private enum RequestParserState {
        /* First Line */
        REQUEST_START, METHOD_READ, URI_READ, HTTP_VERSION, CR_FIRST_LINE,

        READ_HEADERS, READ_OK,

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
    public boolean parse(final ByteBuffer inputBuffer, final ByteBuffer outputBuffer) throws ParserFormatException {
        output = outputBuffer;
        while (inputBuffer.hasRemaining()) {
            byte c = inputBuffer.get();

            switch (requestState) {
                case REQUEST_START:
                    if (ParseUtils.isAlphabetic(c)) {
                        requestState = RequestParserState.METHOD_READ;
                        methodName.put(c);
                    } else {
                        handleParserError();
                    }
                    break;

                case METHOD_READ:
                    if (ParseUtils.isAlphabetic(c)) {
                        methodName.put(c);
                    } else if (c == SP.getValue() && processMethod()) {
                        requestState = RequestParserState.URI_READ;
                        output.put(methodName).put(c);
                    } else {
                        handleParserError();
                    }
                    break;

                case URI_READ:
                    //parseURI();
                    if (c == SP.getValue()) {
                        requestState = RequestParserState.HTTP_VERSION;
                        httpURI.flip();
                        output.put(httpURI).put(c);
                    } else if (ParseUtils.isUriCharacter(c)) {
                        httpURI.put(c);
                    } else {
                        handleParserError();
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
                        handleParserError();
                    }
                    break;

                case READ_HEADERS:
                    if (headersParser.parse(c, output)) {
                        requestState = RequestParserState.READ_OK;
                        return true;
                    }
                    break;

                default:
                    handleParserError();
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

    private boolean URIIsOk() {
        return true;
        //        httpURI.flip();
        //        LOGGER.debug("URI: {}", httpURI);
        //        return httpURI.toString().equals("/");
    }

//    -- NOTAS RFC --
//
//    URI = "http:" "//" host [ ":" port ] [ abs_path [ "?" query ]]
//    1. If Request-URI is an absoluteURI, the host is part of the
//    Request-URI. Any Host header field value in the request MUST be
//    ignored.
//
//    2. If the Request-URI is not an absoluteURI, and the request includes
//    a Host header field, the host is determined by the Host header
//    field value.
//
//        3. If the host as determined by rule 1 or 2 is not a valid host on
//    the server, the response MUST be a 400 (Bad Request) error message.
//    A server
//    SHOULD return 414 (Request-URI Too Long) status if a URI is longer
//    than the server can handle

    // TODO: arreglar esto
    private void handleParserError() throws ParserFormatException {
        requestState = RequestParserState.ERROR;
        throw new ParserFormatException("Error while parsing");
    }

	@Override
	public boolean hasMethod(Method method) {
		return method == this.method;
	}

    @Override public boolean hasHost () {
        return hasHeaderValue(Header.HOST); // TODO: puede venir del URI
    }
}
