package tp.pdc.proxy.parser.body;

import tp.pdc.proxy.exceptions.ParserFormatException;
import tp.pdc.proxy.parser.encoders.StaticL33tEncoder;
import tp.pdc.proxy.parser.interfaces.HttpBodyParser;

import java.nio.ByteBuffer;

public class HttpConnectionCloseL33tParser implements HttpBodyParser {

	private static final HttpConnectionCloseL33tParser INSTANCE =
		new HttpConnectionCloseL33tParser();

	private HttpConnectionCloseL33tParser () {
	}

	public static HttpConnectionCloseL33tParser getInstance () {
		return INSTANCE;
	}

	@Override
	public boolean parse (ByteBuffer input, ByteBuffer output) throws ParserFormatException {
		while (input.hasRemaining() && output.hasRemaining())
			output.put(StaticL33tEncoder.encodeByte(input.get()));

		return false;
	}

	@Override
	public boolean hasFinished () {
		return false;
	}
}
