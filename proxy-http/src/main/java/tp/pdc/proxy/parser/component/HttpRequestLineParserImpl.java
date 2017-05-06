package tp.pdc.proxy.parser.component;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tp.pdc.proxy.ProxyProperties;
import tp.pdc.proxy.exceptions.ParserFormatException;
import tp.pdc.proxy.header.Method;
import tp.pdc.proxy.parser.interfaces.HttpRequestLineParser;
import tp.pdc.proxy.parser.interfaces.HttpVersionParser;
import tp.pdc.proxy.parser.main.HttpRequestParserImpl;
import tp.pdc.proxy.parser.utils.ParseUtils;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.NoSuchElementException;

import static tp.pdc.proxy.parser.utils.AsciiConstants.*;

public class HttpRequestLineParserImpl implements HttpRequestLineParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpRequestParserImpl.class);
    private static final int METHOD_NAME_SIZE = ProxyProperties.getInstance().getMethodBufferSize();
    private static final int URI_SIZE = ProxyProperties.getInstance().getURIBufferSize();

    private RequestLineParserState state;
    private Method method;
    private byte[] hostValue;

    private int buffered = 0;

    private final ByteBuffer methodName, URIHostBuf;

    private HttpVersionParser versionParser;

    private enum RequestLineParserState {
        START, METHOD_READ, HTTP_VERSION, CR,
        URI_READ, HOST_PROTOCOL, URI_HOST_ADDR, URI_NO_HOST, URI_HOST_SLASH,
        READ_OK, ERROR,
    }

    public HttpRequestLineParserImpl () {
        versionParser = new HttpVersionParserImpl(CR.getValue());
        state = RequestLineParserState.START;
        methodName = ByteBuffer.allocate(METHOD_NAME_SIZE);
        URIHostBuf = ByteBuffer.allocate(URI_SIZE);
    }

    @Override public byte[] getHostValue () {
        if (!hasHost())
            throw new NoSuchElementException("Host not read");
        return hostValue;
    }

    @Override public boolean hasMethod () {
        return method != null;
    }
    
    @Override public Method getMethod() {
        if (!hasMethod())
            throw new NoSuchElementException("Method not read");
		return method;
	}


    @Override public boolean hasHost () {
        return hostValue != null;
    }

    @Override public boolean parse (ByteBuffer input, ByteBuffer output)
        throws ParserFormatException {
        while (input.hasRemaining() && output.remaining() > buffered) {
            byte c = input.get();

            switch (state) {
                case START:
                    if (ParseUtils.isAlphabetic(c)) {
                        state = RequestLineParserState.METHOD_READ;
                        methodName.put(c);
                        buffered++;
                    } else {
                        handleError("Error while parsing method");
                    }
                    break;

                case METHOD_READ:
                    if (ParseUtils.isAlphabetic(c)) {
                        methodName.put(c);
                        buffered++;
                    } else if (c == SP.getValue() && processMethod()) {
                        state = RequestLineParserState.URI_READ;
                        output.put(methodName).put(c);
                        buffered = 0;
                    } else {
                        handleError("Error while parsing method");
                    }
                    break;

                case URI_READ:
                    if (c == '/') {
                        output.put(c);
                        state = RequestLineParserState.URI_NO_HOST;
                    } else if (Character.toLowerCase(c) == 'h') { // Has protocol so it has host.
                        output.put(c);
                        state = RequestLineParserState.HOST_PROTOCOL;
                    } else {
                        handleError("Error while parsing URI");
                    }
                    break;

                case URI_NO_HOST:
                    if (c == SP.getValue()) {
                        state = RequestLineParserState.HTTP_VERSION;
                        output.put(c);
                    } else if (ParseUtils.isUriCharacter(c)) {
                        output.put(c);
                    } else {
                        handleError("Error while parsing relative URI");
                    }
                    break;

                case HOST_PROTOCOL:
                    if (ParseUtils.isUriCharacter(c)) {
                        state = c != '/' ? state : RequestLineParserState.URI_HOST_SLASH;
                        output.put(c);
                    } else {
                        handleError("Error while parsing protocol");
                    }
                    break;

                case URI_HOST_SLASH:
                    if (c == '/') {
                        state = RequestLineParserState.URI_HOST_ADDR;
                        output.put(c);
                    } else {
                        handleError("Error while parsing relative URI");
                    }
                    break;

                case URI_HOST_ADDR:
                    if (c == SP.getValue() || c == '/') {
                        loadHostValue();
                        output.put(hostValue).put(c);
                        state = c == '/' ? RequestLineParserState.URI_NO_HOST :
                            RequestLineParserState.HTTP_VERSION;
                        buffered = 0;
                    } else if (ParseUtils.isUriCharacter(c)) {
                        URIHostBuf.put(c);
                        buffered++;
                    } else {
                        handleError("Error while parsing URI address");
                    }
                    break;

                case HTTP_VERSION:
                    if (versionParser.parse(c, output)) {
                        state = RequestLineParserState.CR;
                    }
                    break;

                case CR:
                    if (c == LF.getValue()) {
                        state = RequestLineParserState.READ_OK;
                        output.put(c);
                        return true;
                    } else {
                        handleError("Error while parsing CR first line");
                    }
                    break;

                default:
                    handleError("Error while parsing first line");
            }
        }
        return false;
    }

    private void loadHostValue() {
        if (hostValue != null)
            return; // already loaded

        URIHostBuf.flip();
        hostValue = new byte[URIHostBuf.remaining()];
        URIHostBuf.get(hostValue);
    }

    private boolean processMethod () {
        if (method != null)
            return true; // Already loaded

        int strLen = methodName.position();
        methodName.flip();
        method = Method.getByBytes(methodName, strLen);
        LOGGER.debug("METHOD: {}", method);
        return method != null; // Valid method
    }

    private void handleError(String message) throws ParserFormatException {
        state = RequestLineParserState.ERROR;
        throw new ParserFormatException(message);
    }

    @Override public boolean hasFinished () {
        return state == RequestLineParserState.READ_OK;
    }

    @Override public void reset () {
        versionParser.reset();
        state = RequestLineParserState.START;
        methodName.clear();
        URIHostBuf.clear();
        method = null; hostValue = null;
    }
}
