package tp.pdc.proxy.parser;

import java.nio.ByteBuffer;

import tp.pdc.proxy.exceptions.ParserFormatException;
import tp.pdc.proxy.parser.interfaces.HttpBodyParser;

public class HttpNullBodyParser implements HttpBodyParser {

	private static final HttpNullBodyParser INSTANCE = new HttpNullBodyParser();
	
	private HttpNullBodyParser() {
	}
	
	public static final HttpNullBodyParser getInstance() {
		return INSTANCE;
	}
	
	@Override
	public boolean parse(ByteBuffer input, ByteBuffer output) throws ParserFormatException {
		return true;
	}

	@Override
	public boolean hasFinished() {
		return true;
	}

}
