package tp.pdc.proxy.parser.body;

import tp.pdc.proxy.exceptions.ParserFormatException;
import tp.pdc.proxy.parser.interfaces.HttpBodyParser;

import java.nio.ByteBuffer;

public class HttpNoBodyParser implements HttpBodyParser {

	private static final HttpNoBodyParser INSTANCE = new HttpNoBodyParser();

	private HttpNoBodyParser () {
	}

	public static final HttpNoBodyParser getInstance () {
		return INSTANCE;
	}

	@Override
	public boolean parse (ByteBuffer input, ByteBuffer output) throws ParserFormatException {
		return true;
	}

	@Override
	public boolean hasFinished () {
		return true;
	}

}
