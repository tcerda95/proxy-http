package tp.pdc.proxy.parser.body;

import tp.pdc.proxy.exceptions.ParserFormatException;
import tp.pdc.proxy.parser.interfaces.HttpBodyParser;

import java.nio.ByteBuffer;

/**
 * Body parser in case there is no body
 */
public class HttpNullBodyParser implements HttpBodyParser {

	private static final HttpNullBodyParser INSTANCE = new HttpNullBodyParser();

	private HttpNullBodyParser () {
	}

	public static final HttpNullBodyParser getInstance () {
		return INSTANCE;
	}

	@Override
	public boolean parse (ByteBuffer input, ByteBuffer output) throws ParserFormatException {
		return false;
	}

	@Override
	public boolean hasFinished () {
		return false;
	}

}
