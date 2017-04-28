package tp.pdc.proxy.parser;

import java.nio.ByteBuffer;

import tp.pdc.proxy.exceptions.ParserFormatException;
import tp.pdc.proxy.header.Header;
import tp.pdc.proxy.header.Method;
import tp.pdc.proxy.parser.interfaces.HttpRequestParser;

public class MockHeaderParser implements HttpRequestParser {
	
	private boolean finished = false;

	@Override
	public boolean parse(ByteBuffer input, ByteBuffer output) throws ParserFormatException {
		output.put(input);
		this.finished = true;
		return hasFinished();
	}

	@Override
	public boolean hasFinished() {
		return this.finished;
	}

	@Override
	public boolean hasHeaderValue(Header header) {
		return false;
	}

	@Override
	public byte[] getHeaderValue(Header header) {
		return null;
	}

	@Override
	public boolean hasMethod(Method method) {
		return false;
	}
}
