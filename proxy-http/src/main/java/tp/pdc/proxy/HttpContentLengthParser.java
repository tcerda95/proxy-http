package tp.pdc.proxy;

import java.nio.ByteBuffer;

import tp.pdc.proxy.exceptions.ParserFormatException;
import tp.pdc.proxy.parser.HttpBodyParser;

public class HttpContentLengthParser implements HttpBodyParser{

	private int contentLength;
	
	private ParserState parserState;
	
	private static final char CR = 13;
	private static final char LF = 10;
	
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
	public void parse(ByteBuffer input, ByteBuffer output) throws ParserFormatException {
		
		int index = 1;
		
		while (input.hasRemaining() && !outputBufferisFull(output)) {
			
			byte c = input.get();
    		output.put(c);
    		
			switch (parserState) {
			
			case START:
				
				if ( (index == contentLength-1 && c != CR) 
						|| (index == contentLength && c != LF))
					handleError(parserState);
				
				if (index == contentLength)
					parserState = ParserState.END_OK;
				break;
				
			default:
				handleError(parserState);
			}
			index++;
		}
		
	}

	@Override
	public boolean hasFinished() {
		return parserState == ParserState.END_OK;
	}
	
	public boolean outputBufferisFull(ByteBuffer output) {
		return output.position() != output.capacity();
	}
	
    private void handleError(ParserState parserState) throws ParserFormatException {
        parserState = ParserState.ERROR;
        throw new ParserFormatException("Error while parsing");
    }

}
