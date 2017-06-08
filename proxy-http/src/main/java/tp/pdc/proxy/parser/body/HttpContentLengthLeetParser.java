package tp.pdc.proxy.parser.body;

import tp.pdc.proxy.exceptions.ParserFormatException;
import tp.pdc.proxy.parser.encoders.StaticL33tEncoder;
import tp.pdc.proxy.parser.interfaces.HttpBodyParser;

import java.nio.ByteBuffer;

public class HttpContentLengthLeetParser implements HttpBodyParser {

	private int contentLength;
	private int index;

	private ParserState parserState;

	public HttpContentLengthLeetParser (int contentLength) {
		this.contentLength = contentLength;
		this.parserState = ParserState.START;
		this.index = 1;
	}

	@Override
	public boolean parse (ByteBuffer input, ByteBuffer output) throws ParserFormatException {

		while (index <= contentLength && input.hasRemaining() && output.hasRemaining()) {

			byte c = input.get();

			output.put(StaticL33tEncoder.encodeByte(c));

			switch (parserState) {

				case START:

					if (index == contentLength) {
						parserState = ParserState.END_OK;
						return true;
					}
					break;

				default:
					handleParserError();
			}
			index++;
		}

		return hasFinished();
	}

	@Override
	public boolean hasFinished () {
		return parserState == ParserState.END_OK;
	}

	private void handleParserError () throws ParserFormatException {
		parserState = ParserState.ERROR;
		throw new ParserFormatException("Error while parsing");
	}

	// TODO: si esto no se usa, sacar. Hay Que solucionarlo contra alguna interfaz
	public void reset (int contentLength) {
		this.parserState = ParserState.START;
		this.contentLength = contentLength;
		this.index = 1;
	}

	private enum ParserState {
		START,
		END_OK,
		/* Error states */
		ERROR,
	}

}
