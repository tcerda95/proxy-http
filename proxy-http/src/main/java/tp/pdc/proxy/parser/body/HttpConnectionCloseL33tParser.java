package tp.pdc.proxy.parser.body;

import java.nio.ByteBuffer;

import tp.pdc.proxy.exceptions.ParserFormatException;
import tp.pdc.proxy.parser.encoders.StaticL33tEncoder;
import tp.pdc.proxy.parser.interfaces.HttpBodyParser;

public class HttpConnectionCloseL33tParser implements HttpBodyParser {

	private static final HttpConnectionCloseL33tParser INSTANCE = new HttpConnectionCloseL33tParser();
	
	public static HttpConnectionCloseL33tParser getInstance() {
		return INSTANCE;
	}
	
	private HttpConnectionCloseL33tParser() {
	}

	@Override
	public boolean parse(ByteBuffer input, ByteBuffer output) throws ParserFormatException {
		while (input.hasRemaining() && output.hasRemaining())
			output.put(StaticL33tEncoder.encodeByte(input.get()));

		return false;
	}

	@Override
	public boolean hasFinished() {
		return false;
	}
}
