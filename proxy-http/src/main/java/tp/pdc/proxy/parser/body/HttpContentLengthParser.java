package tp.pdc.proxy.parser.body;

import java.nio.ByteBuffer;

import tp.pdc.proxy.exceptions.ParserFormatException;
import tp.pdc.proxy.header.BytesUtils;
import tp.pdc.proxy.parser.interfaces.HttpBodyParser;

public class HttpContentLengthParser implements HttpBodyParser{

	private int contentLength;
		
	public HttpContentLengthParser(int contentLength) {
		this.contentLength = contentLength;
	}

	@Override
	public boolean parse(ByteBuffer input, ByteBuffer output) throws ParserFormatException {
		
		if (output.remaining() >= input.remaining() && input.remaining() <= contentLength) {
			contentLength -= input.remaining();
			output.put(input);
		}
		else if (output.remaining() < input.remaining() && output.remaining() <= contentLength) {
			lengthPut(input, output, output.remaining());
		}
		else if (output.remaining() >= input.remaining() && input.remaining() > contentLength) {
			lengthPut(input, output, contentLength);
		}
		else if (output.remaining() < input.remaining() && output.remaining() > contentLength) {
			lengthPut(input, output, contentLength);
		}
		else {
			throw new IllegalStateException("Parser in inconsistent state");
		}
		
		return hasFinished();
	}

	private void lengthPut(ByteBuffer input, ByteBuffer output, int length) {
		BytesUtils.lengthPut(input, output, length);
		contentLength -= length;
	}
	
	@Override
	public boolean hasFinished() {
		return contentLength == 0;
	}

}
