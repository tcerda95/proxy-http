package tp.pdc.proxy.parser.mainParsers;

import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tp.pdc.proxy.exceptions.ParserFormatException;
import tp.pdc.proxy.header.Header;
import tp.pdc.proxy.header.Method;
import tp.pdc.proxy.parser.componentParsers.HttpHeadersParserImpl;
import tp.pdc.proxy.parser.componentParsers.HttpRequestLineParserImpl;
import tp.pdc.proxy.parser.interfaces.HttpRequestLineParser;
import tp.pdc.proxy.parser.interfaces.HttpRequestParser;

public class HttpRequestParserImpl implements HttpRequestParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpRequestParserImpl.class);

    private HttpRequestLineParser requestLineParser;
    private HttpHeadersParserImpl headersParser;
    
    @Override public boolean hasHeaderValue (Header header) {
        return headersParser.hasHeaderValue(header);
    }

    @Override public byte[] getHeaderValue (Header header) {
        return headersParser.getHeaderValue(header);
    }

    @Override public byte[] getHostValue () {
        if (!hasHost())
            throw new IllegalStateException(); //TODO

        if (requestLineParser.hasHost())
            return requestLineParser.getHostValue();
        else
            return headersParser.getHeaderValue(Header.HOST);
    }

    @Override public boolean hasMethod (Method method) {
        return requestLineParser.hasMethod(method);
    }

    @Override public boolean hasHost () {
        return requestLineParser.hasHost() || headersParser.hasHeaderValue(Header.HOST);
    }

    @Override public boolean hasFinished () {
        return requestLineParser.hasFinished() && headersParser.hasFinished();
    }

    @Override public void reset () {
        headersParser.reset();
        requestLineParser.reset();
    }

    public HttpRequestParserImpl () {
        headersParser = new HttpHeadersParserImpl();
        requestLineParser = new HttpRequestLineParserImpl();
    }

    public boolean parse(final ByteBuffer input, final ByteBuffer output) throws ParserFormatException {
        while (input.hasRemaining() && output.hasRemaining()) {
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

	@Override
	public boolean parse(byte c, ByteBuffer output) throws ParserFormatException {
        throw new UnsupportedOperationException(); // TODO
	}

	@Override
	public Method getMethod() {
		return requestLineParser.getMethod();
	}
}
