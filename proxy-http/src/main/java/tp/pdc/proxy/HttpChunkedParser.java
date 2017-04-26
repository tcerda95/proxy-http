package tp.pdc.proxy;

import java.nio.ByteBuffer;

import tp.pdc.proxy.exceptions.ParserFormatException;

public class HttpChunkedParser implements HttpBodyParser {

	private static final char CR = 13;
	private static final char LF = 10;
	
    private ParserState parserState;
    private ChunkSizeState chunkSizeState;
    private BodyState bodyState;
        
    private int chunkSize;
    private boolean chunkSizeFound;
    
    
    public HttpChunkedParser() {
    	parserState = ParserState.READ_CHUNK_SIZE;
    	chunkSizeState = ChunkSizeState.START;
    	bodyState = BodyState.NOT_READ_YET;
   	 
    	chunkSize = 0;
    	chunkSizeFound = false;
    }
	
    private enum ParserState {
        READ_CHUNK_SIZE,
        READ_BODY,
        
        /* Error states */
        ERROR,
    }
    
    private enum ChunkSizeState {
    	START,
    	END_LINE_CR,
    	END_OK,
    	CHUNKSIZE_IS_ZERO,
    	ERROR,
    }
    
    private enum BodyState {	
    	NOT_READ_YET,
    	START,
    	CHUNK_READ,
        END_LINE_CR,
    	END_OK,
    	ERROR,
    }
    
    @Override
	public void parse(ByteBuffer input, ByteBuffer output) throws ParserFormatException {
    	
    	while (input.hasRemaining() && !outputBufferisFull(output)) {
    		byte c = input.get();
    		output.put(c);
    		
    		switch(parserState) {
	    		case READ_CHUNK_SIZE:	    			
	    			parseChunkSize(c);
	    			break;
	    		
	    		case READ_BODY:
	    			parseBody(c);
	    			break;
	    			
	    		default:
	    			handleError(parserState);
    		}
    	}
	}
    
    private void parseChunkSize(byte c) throws ParserFormatException {
    	
    	switch(chunkSizeState) {
	    	case START:
	    		// TODO: considerar caso chunksize es hexa
	    		if (c == LF || ParseUtils.isAlphabetic(c))
	    			handleError(chunkSizeState);
	    		
	    		if (c == CR) {
	    			if (!chunkSizeFound)
						handleError(chunkSizeState);					
					
					chunkSizeState = ChunkSizeState.END_LINE_CR;
				}		
				else {
					chunkSize = chunkSize*10 + c - '0';
					chunkSizeFound = true;
				}
					
				break;
				
			case END_LINE_CR:
				
				if (c == LF) {
					
					chunkSizeState = chunkSizeisZero() ? ChunkSizeState.CHUNKSIZE_IS_ZERO : 
						ChunkSizeState.END_OK;
					
					parserState = ParserState.READ_BODY;
					bodyState = BodyState.START;
					chunkSizeFound = false;
				}
				else
					handleError(chunkSizeState);
				
				break;
				
			default:
				handleError(chunkSizeState);
    	}
    }
    
    private void parseBody(byte c) throws ParserFormatException {
    	
    	chunkSize--;
    	
    	switch(bodyState) {
    		
    		case START:
    			
    			if (chunkSize == 0)
    				bodyState = BodyState.CHUNK_READ;
    			
    			break;
    	
    		case CHUNK_READ:
    			
    			if (c != CR)
    				handleError(bodyState);
    			
    			bodyState = BodyState.END_LINE_CR;
    			
    			break;
    			
    		case END_LINE_CR:
    			
    			if (c != LF)
    				handleError(bodyState);
    			
    			if (chunkSizeState == ChunkSizeState.CHUNKSIZE_IS_ZERO)
    				bodyState = BodyState.END_OK;
    			else {
    				parserState = ParserState.READ_CHUNK_SIZE;
    				chunkSizeState = ChunkSizeState.START;
    				bodyState = BodyState.NOT_READ_YET;
    			}
    		
    			break;
    			
			default:
				handleError(bodyState);
    	}
    }
    

	@Override
	public boolean hasFinished() {
		return parserState == ParserState.READ_BODY && bodyState == BodyState.END_OK
				&& chunkSizeState == ChunkSizeState.CHUNKSIZE_IS_ZERO;
	}
	
	public boolean chunkSizeisZero() {
		return chunkSize == 0;
	}
	
	public boolean outputBufferisFull(ByteBuffer output) {
		return output.position() != output.capacity();
	}
	
    private void handleError(ParserState parserState) throws ParserFormatException {
        parserState = ParserState.ERROR;
        throw new ParserFormatException("Error while parsing");
    }

    private void handleError(ChunkSizeState chunkSizeState) throws ParserFormatException {
        chunkSizeState = ChunkSizeState.ERROR;
        throw new ParserFormatException("Error while parsing chunk size");
    }
    
    private void handleError(BodyState bodyState) throws ParserFormatException {
        bodyState = BodyState.ERROR;
        throw new ParserFormatException("Error while parsing body");
    }
}
