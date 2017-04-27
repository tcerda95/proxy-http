package tp.pdc.proxy.parser;

import tp.pdc.proxy.exceptions.ParserFormatException;
import tp.pdc.proxy.parser.interfaces.HttpHeaderParser;
import tp.pdc.proxy.parser.interfaces.HttpResponseLineParser;
import tp.pdc.proxy.parser.interfaces.HttpResponseParser;

import java.nio.ByteBuffer;


public class HttpResponseParserImpl implements HttpResponseParser {

    HttpResponseLineParser lineParser;
    HttpHeaderParser headerParser;

    public HttpResponseParserImpl () {
        lineParser = new HttpResponseLineParserImpl();
        headerParser = new HttpHeadersParserImpl();
    }

    @Override public int getStatusCode () {
        if (!hasStatusCode()) {
            throw new IllegalArgumentException(); //TODO: exception
        }
        return lineParser.getStatusCode();
    }

    @Override public boolean hasStatusCode () {
        return lineParser.hasStatusCode();
    }

    @Override public boolean parse (ByteBuffer input, ByteBuffer output)
        throws ParserFormatException {
        while (input.hasRemaining()) {
            if (!lineParser.hasFinished()) {
                lineParser.parse(input, output);
            } else if (!headerParser.hasFinished()) {
                if (headerParser.parse(input, output))
                    return true;
            } else {
                throw new ParserFormatException("Already finished parsing.");
            }
        }
        return false;
    }

    @Override public boolean hasFinished () {
        return lineParser.hasFinished() && headerParser.hasFinished();
    }
}
