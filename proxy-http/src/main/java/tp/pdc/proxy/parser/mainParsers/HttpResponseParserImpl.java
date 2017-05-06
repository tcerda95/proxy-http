package tp.pdc.proxy.parser.mainParsers;

import tp.pdc.proxy.exceptions.ParserFormatException;
import tp.pdc.proxy.header.Header;
import tp.pdc.proxy.parser.componentParsers.HttpHeadersParserImpl;
import tp.pdc.proxy.parser.componentParsers.HttpResponseLineParserImpl;
import tp.pdc.proxy.parser.interfaces.HttpHeaderParser;
import tp.pdc.proxy.parser.interfaces.HttpResponseLineParser;
import tp.pdc.proxy.parser.interfaces.HttpResponseParser;

import java.nio.ByteBuffer;
import java.util.*;


public class HttpResponseParserImpl implements HttpResponseParser {

    HttpResponseLineParser lineParser;
    HttpHeaderParser headerParser;

    public HttpResponseParserImpl () {
        // TODO por ahora esta esto aca
        Set<Header> toRemove = Collections.emptySet();
        Map<Header, byte[]> toAdd = new HashMap<>();
        toAdd.put(Header.CONNECTION, "close".getBytes());
        Set<Header> toSave = new HashSet<>();
        toSave.add(Header.CONTENT_LENGTH);
        toSave.add(Header.TRANSFER_ENCODING);
        //

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
