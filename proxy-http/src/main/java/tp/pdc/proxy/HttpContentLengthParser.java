package tp.pdc.proxy;

import java.nio.ByteBuffer;

import tp.pdc.proxy.exceptions.ParserFormatException;

public class HttpContentLengthParser implements HttpBodyParser{

	@Override
	public void parse(ByteBuffer input, ByteBuffer output) throws ParserFormatException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean hasFinished() {
		// TODO Auto-generated method stub
		return false;
	}

}
