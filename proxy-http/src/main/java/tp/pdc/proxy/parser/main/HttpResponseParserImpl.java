package tp.pdc.proxy.parser.main;

import tp.pdc.proxy.exceptions.ParserFormatException;
import tp.pdc.proxy.header.Header;
import tp.pdc.proxy.parser.component.HttpHeadersParserImpl;
import tp.pdc.proxy.parser.component.HttpResponseLineParserImpl;
import tp.pdc.proxy.parser.interfaces.HttpHeaderParser;
import tp.pdc.proxy.parser.interfaces.HttpResponseLineParser;
import tp.pdc.proxy.parser.interfaces.HttpResponseParser;

import java.nio.ByteBuffer;
import java.util.*;


public class HttpResponseParserImpl implements HttpResponseParser {

    private HttpResponseLineParser lineParser;
    private HttpHeaderParser headerParser;

    public HttpResponseParserImpl (Map<Header, byte[]> toAdd, Set<Header> toRemove, Set<Header> toSave) {
        lineParser = new HttpResponseLineParserImpl();
        headerParser = new HttpHeadersParserImpl(toAdd, toRemove, toSave);
    }

    @Override public int getStatusCode () {
        if (!hasStatusCode()) {
            throw new NoSuchElementException("No status code read");
        }
        return lineParser.getStatusCode();
    }

    @Override public boolean hasStatusCode () {
        return lineParser.hasStatusCode();
    }

    @Override public boolean parse (ByteBuffer input, ByteBuffer output)
        throws ParserFormatException {
        while (input.hasRemaining() && output.hasRemaining()) {
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

    @Override public void reset () {
        lineParser.reset();
        headerParser.reset();
    }

    @Override public boolean hasHeaderValue (Header header) {
        return headerParser.hasHeaderValue(header);
    }

    @Override public byte[] getHeaderValue (Header header) {
        return headerParser.getHeaderValue(header);
    }
}
