package tp.pdc.proxy.parser;

import java.nio.ByteBuffer;

import static tp.pdc.proxy.parser.utils.AsciiConstants.*;
import tp.pdc.proxy.exceptions.ParserFormatException;
import tp.pdc.proxy.parser.interfaces.HttpBodyParser;

public class HttpContentLengthParser implements HttpBodyParser{

	private int contentLength;
	
	private ParserState parserState;
	
	public HttpContentLengthParser(int contentLength) {
		this.contentLength = contentLength;
		this.parserState = ParserState.START;
	}

	private enum ParserState {
        START,
        END_OK,
        /* Error states */
        ERROR,
    }
	
	@Override
	public boolean parse(ByteBuffer input, ByteBuffer output) throws ParserFormatException {
		
		int index = 1;
		
		while (input.hasRemaining() && output.hasRemaining()) {
			
			byte c = input.get();
    		output.put(c);
    		
			switch (parserState) {
			
			case START:
				
				if ( (index == contentLength-1 && c != CR.getValue()) 
						|| (index == contentLength && c != LF.getValue()))
					handleError(parserState);
				
				if (index == contentLength)
					parserState = ParserState.END_OK;
				break;
				
			default:
				handleError(parserState);
			}
			index++;
		}
		
		return hasFinished();
	}

	@Override
	public boolean hasFinished() {
		return parserState == ParserState.END_OK;
	}
	
    private void handleError(ParserState parserState) throws ParserFormatException {
        parserState = ParserState.ERROR;
        throw new ParserFormatException("Error while parsing");
    }

}
