package tp.pdc.proxy.parser.protocol;

import java.nio.ByteBuffer;

import tp.pdc.proxy.exceptions.ParserFormatException;
import tp.pdc.proxy.parser.interfaces.ProtocolParser;

public class CrazyProtocolParser implements ProtocolParser {

	@Override
	public boolean parse(ByteBuffer input, ByteBuffer output) throws ParserFormatException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean hasFinished() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void reset() {
		// TODO Auto-generated method stub
		
	}

}
