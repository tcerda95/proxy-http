package tp.pdc.proxy.parser.body;

import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tp.pdc.proxy.bytes.BytesUtils;
import tp.pdc.proxy.exceptions.ParserFormatException;
import tp.pdc.proxy.parser.interfaces.HttpBodyParser;

/**
 * Body parser for Content length and l33t flag not activated
 */
public class HttpContentLengthParserBugged implements HttpBodyParser {

	private static final Logger LOGGER = LoggerFactory.getLogger(HttpContentLengthParserBugged.class);
	private int contentLength;

	public HttpContentLengthParserBugged (int contentLength) {
		this.contentLength = contentLength;
	}

	@Override
	public boolean parse (ByteBuffer input, ByteBuffer output) throws ParserFormatException {

		LOGGER.debug("Content length before: {}", contentLength);
		
		if (output.remaining() >= input.remaining() && input.remaining() <= contentLength) {
			LOGGER.debug("output.remaining() >= input.remaining() && input.remaining() <= contentLength");
			
			contentLength -= input.remaining();
			output.put(input);
		} else if (output.remaining() < input.remaining() && output.remaining() <= contentLength) {
			LOGGER.debug("output.remaining() < input.remaining() && output.remaining() <= contentLength");
			lengthPut(input, output, output.remaining());
			
		} else if (output.remaining() >= input.remaining() && input.remaining() > contentLength) {
			LOGGER.debug("output.remaining() >= input.remaining() && input.remaining() > contentLength");

			lengthPut(input, output, contentLength);
		} else if (output.remaining() < input.remaining() && output.remaining() > contentLength) {
			LOGGER.debug("output.remaining() < input.remaining() && output.remaining() > contentLength");

			lengthPut(input, output, contentLength);
		} else {
			throw new IllegalStateException("Parser in inconsistent state");
		}

		LOGGER.debug("Content length after: {}", contentLength);
		
		return hasFinished();
	}

	private void lengthPut (ByteBuffer input, ByteBuffer output, int length) {
		LOGGER.debug("Length put of {}", length);
		BytesUtils.lengthPut(input, output, length);
		contentLength -= length;
	}

	@Override
	public boolean hasFinished () {
		return contentLength == 0;
	}
}
