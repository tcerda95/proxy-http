package tp.pdc.proxy.parser;

import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tp.pdc.proxy.exceptions.ParserFormatException;
import tp.pdc.proxy.header.Header;
import tp.pdc.proxy.header.Method;
import tp.pdc.proxy.parser.interfaces.HttpRequestLineParser;
import tp.pdc.proxy.parser.interfaces.HttpRequestParser;

public class HttpRequestParserImpl implements HttpRequestParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpRequestParserImpl.class);

    @Override public boolean hasHeaderValue (Header header) {
        return headersParser.hasRelevantHeaderValue(header);
    }

    @Override public byte[] getHeaderValue (Header header) {
        return headersParser.getRelevantHeaderValue(header);
    }

    @Override public byte[] getHostValue () {
        if (!hasHost())
            throw new IllegalStateException(); //TODO

        if (requestLineParser.hasHost())
            return requestLineParser.getHostValue();
        else
            return headersParser.getRelevantHeaderValue(Header.HOST);
    }

    @Override public boolean hasMethod (Method method) {
        return requestLineParser.hasMethod(method);
    }

    @Override public boolean hasHost () {
        return requestLineParser.hasHost() || headersParser.hasRelevantHeaderValue(Header.HOST);
    }

    @Override public boolean hasFinished () {
        return requestLineParser.hasFinished() && headersParser.hasFinished();
    }

    private HttpRequestLineParser requestLineParser;
    private HttpHeadersParserImpl headersParser;

    public HttpRequestParserImpl () {
        headersParser = new HttpHeadersParserImpl();
        requestLineParser = new HttpRequestLineParserImpl();
    }

    public boolean parse(final ByteBuffer input, final ByteBuffer output) throws ParserFormatException {
        while (input.hasRemaining()) {
            if (!requestLineParser.hasFinished()) {
                requestLineParser.parse(input, output);
            } else if (!headersParser.hasFinished()) {
                if (headersParser.parse(input, output))
                    return true;
            } else {
                throw new ParserFormatException("Already finished parsing.");
            }
        }
        return false;
    }

    //    A server
    //    SHOULD return 414 (Request-URI Too Long) status if a URI is longer
    //    than the server can handle
}
