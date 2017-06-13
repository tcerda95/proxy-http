package tp.pdc.proxy.parser.body;

import tp.pdc.proxy.exceptions.ParserFormatException;
import tp.pdc.proxy.parser.interfaces.HttpBodyParser;

import java.nio.ByteBuffer;

/**
 * Body parser in case it is not known what type of body will be found. Avoids
 * working with null.
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
