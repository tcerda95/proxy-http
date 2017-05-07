package tp.pdc.proxy.parser.body;

import java.nio.ByteBuffer;

import tp.pdc.proxy.exceptions.ParserFormatException;
import tp.pdc.proxy.parser.interfaces.HttpBodyParser;
import tp.pdc.proxy.parser.interfaces.l33tEncoder;
import tp.pdc.proxy.parser.encoders.*;

public class HttpContentLengthLeetParser implements HttpBodyParser{

	private int contentLength;
	private int index;
	
	private ParserState parserState;
	
    private l33tEncoder l33tEncoder;
	
	public HttpContentLengthLeetParser(int contentLength) {
		this.contentLength = contentLength;
		this.parserState = ParserState.START;
		this.index = 1;
		
		l33tEncoder = new L33tEncoderImpl();
	}

	private enum ParserState {
        START,
        END_OK,
        /* Error states */
        ERROR,
    }
	
	@Override
	public boolean parse(ByteBuffer input, ByteBuffer output) throws ParserFormatException {
				
		while (index < contentLength && input.hasRemaining() && output.hasRemaining()) {
			
			byte c = input.get();
    		
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
			
    		output.put(l33tEncoder.encodeByte(c));
			
			index++;
		}
		
		return hasFinished();
	}

	@Override
	public boolean hasFinished() {
		return parserState == ParserState.END_OK;
	}

	@Override public void reset () {
		//TODO
	}

	private void handleParserError() throws ParserFormatException {
        parserState = ParserState.ERROR;
        throw new ParserFormatException("Error while parsing");
    }

	public void reset(int contentLength) {
		this.parserState = ParserState.START;
		this.contentLength = contentLength;
		this.index = 1;
	}

}
