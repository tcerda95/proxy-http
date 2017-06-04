package tp.pdc.proxy.parser.main;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import tp.pdc.proxy.exceptions.ParserFormatException;
import tp.pdc.proxy.header.Header;
import tp.pdc.proxy.header.Method;
import tp.pdc.proxy.parser.body.HttpNullBodyParser;
import tp.pdc.proxy.parser.component.HttpHeaderParserImpl;
import tp.pdc.proxy.parser.component.HttpResponseLineParserImpl;
import tp.pdc.proxy.parser.factory.HttpBodyParserFactory;
import tp.pdc.proxy.parser.interfaces.HttpBodyParser;
import tp.pdc.proxy.parser.interfaces.HttpHeaderParser;
import tp.pdc.proxy.parser.interfaces.HttpResponseLineParser;
import tp.pdc.proxy.parser.interfaces.HttpResponseParser;

public class HttpResponseParserImpl implements HttpResponseParser {

	private static final HttpBodyParserFactory BODY_PARSER_FACTORY = HttpBodyParserFactory.getInstance();
	
    private HttpResponseLineParser lineParser;
    private HttpHeaderParser headerParser;
    private HttpBodyParser bodyParser = HttpNullBodyParser.getInstance();
    private Method clientMethod;

    public HttpResponseParserImpl (Map<Header, byte[]> toAdd, Set<Header> toRemove, Set<Header> toSave, Method clientMethod) {
        lineParser = new HttpResponseLineParserImpl();
        headerParser = new HttpHeaderParserImpl(toAdd, toRemove, toSave);
        this.clientMethod = clientMethod;
    }

    @Override 
    public int getStatusCode () {
        if (!hasStatusCode()) {
            throw new NoSuchElementException("No status code read");
        }
        return lineParser.getStatusCode();
    }

    @Override 
    public boolean hasStatusCode() {
        return lineParser.hasStatusCode();
    }

    @Override 
    public boolean parse(ByteBuffer input, ByteBuffer output) throws ParserFormatException {
        while (input.hasRemaining() && output.hasRemaining()) {
            if (!lineParser.hasFinished()) {
                lineParser.parse(input, output);
            } 
            else if (!headerParser.hasFinished()) {
            	
                if (headerParser.parse(input, output)) {
                    bodyParser = BODY_PARSER_FACTORY.getServerHttpBodyParser(this, clientMethod);
                    return bodyParser.parse(input, output);
                }
            } 
            else if (!bodyParser.hasFinished()) {
            	
            	if (bodyParser.parse(input, output))
            		return true;
            }
            else {
                throw new ParserFormatException("Already finished parsing.");
            }
        }
        
        return false;
    }

    @Override 
    public boolean hasFinished() {
        return lineParser.hasFinished() && headerParser.hasFinished() && bodyParser.hasFinished();
    }

    @Override 
    public void reset() {
        lineParser.reset();
        headerParser.reset();
        bodyParser = HttpNullBodyParser.getInstance();
        clientMethod = null;
    }

    @Override
    public boolean hasHeaderValue(Header header) {
        return headerParser.hasHeaderValue(header);
    }

    @Override
    public byte[] getHeaderValue(Header header) {
        return headerParser.getHeaderValue(header);
    }

    @Override 
    public boolean readMinorVersion() {
        return lineParser.readMinorVersion();
    }

    @Override 
    public boolean readMajorVersion() {
        return lineParser.readMajorVersion();
    }

    @Override 
    public int getMajorHttpVersion() {
        return lineParser.getMajorHttpVersion();
    }

    @Override 
    public int getMinorHttpVersion() {
        return lineParser.getMinorHttpVersion();
    }

    @Override 
    public byte[] getWholeVersionBytes() {
        return lineParser.getWholeVersionBytes();
    }

	@Override
	public boolean hasHeadersFinished() {
		return headerParser.hasFinished();
	}

	@Override
	public void setClientMethod(Method clientMethod) {
		this.clientMethod = clientMethod;
	}
}
