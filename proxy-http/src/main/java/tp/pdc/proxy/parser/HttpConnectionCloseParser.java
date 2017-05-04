package tp.pdc.proxy.parser;

import java.nio.ByteBuffer;

import tp.pdc.proxy.exceptions.ParserFormatException;
import tp.pdc.proxy.header.BytesUtils;
import tp.pdc.proxy.parser.interfaces.HttpBodyParser;

public class HttpConnectionCloseParser implements HttpBodyParser {
	
	private static final HttpConnectionCloseParser INSTANCE = new HttpConnectionCloseParser();
	
	private HttpConnectionCloseParser() {
	}
	
	public static final HttpConnectionCloseParser getInstance() {
		return INSTANCE;
	}
	
	@Override
	public boolean parse(ByteBuffer input, ByteBuffer output) throws ParserFormatException {
		if (output.remaining() >= input.remaining())
			output.put(input);
		else
			BytesUtils.lengthPut(input, output, output.remaining());
		return false;
	}

	@Override
	public boolean hasFinished() {
		return false;
	}

	@Override public void reset () {
		//TODO
	}

}
