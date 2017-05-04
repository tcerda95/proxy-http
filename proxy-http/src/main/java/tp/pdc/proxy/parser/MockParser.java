package tp.pdc.proxy.parser;

import java.nio.ByteBuffer;

import tp.pdc.proxy.exceptions.ParserFormatException;
import tp.pdc.proxy.parser.interfaces.Parser;

public class MockParser implements Parser {
	
	private boolean finished = false;

	@Override
	public boolean parse(ByteBuffer input, ByteBuffer output) throws ParserFormatException {
		output.put(input);
		finished = true;
		return hasFinished();
	}

	@Override
	public boolean hasFinished() {
		return finished;
	}

	@Override public void reset () {
		//TODO
	}

}
