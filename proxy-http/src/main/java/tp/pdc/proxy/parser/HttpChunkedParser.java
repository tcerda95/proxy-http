package tp.pdc.proxy.parser;

import java.nio.ByteBuffer;

import tp.pdc.proxy.exceptions.ParserFormatException;
import tp.pdc.proxy.parser.interfaces.HttpBodyParser;
import tp.pdc.proxy.parser.interfaces.l33tEncoder;
import tp.pdc.proxy.parser.encoders.*;
import tp.pdc.proxy.parser.utils.ParseUtils;
import static tp.pdc.proxy.parser.utils.AsciiConstants.*;
import static tp.pdc.proxy.parser.utils.DecimalConstants.*;

public class HttpChunkedParser implements HttpBodyParser {
	
    private ParserState parserState;
    private ChunkSizeState chunkSizeState;
    private ChunkState chunkState;
        
    private int chunkSize;
    private boolean chunkSizeFound;
    
    private l33tEncoder l33tEncoder;
    private boolean l33tFlag;
    
    public HttpChunkedParser(boolean l33tFlag) {
    	parserState = ParserState.READ_CHUNK_SIZE;
    	chunkSizeState = ChunkSizeState.START;
    	chunkState = ChunkState.NOT_READ_YET;
   	 
    	chunkSize = 0;
    	chunkSizeFound = false;
    	this.l33tFlag = l33tFlag;
    	l33tEncoder = new l33tEncoderImpl();
    }
	
    private enum ParserState {
        READ_CHUNK_SIZE,
        READ_CHUNK,
 
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
    
    private enum ChunkState {	
    	NOT_READ_YET,
    	START,
        END_LINE_CR,
    	END_OK,
    	ERROR,
    }
    
    @Override
	public boolean parse(ByteBuffer input, ByteBuffer output) throws ParserFormatException {
    	
    	while (input.hasRemaining() && output.hasRemaining()) {
    		byte c = input.get();
    		
    		switch(parserState) {
	    		case READ_CHUNK_SIZE:	    			
	    			parseChunkSize(c);
	    			break;
	    		
	    		case READ_CHUNK:
	    			parseChunk(c);
	    			
	    			if (l33tFlag)
	    				c = l33tEncoder.encodeByte(c);
	  
	    			break;
	    			
	    		default:
	    			handleParserError();
    		}
			output.put(c);
    	}
    	
    	return hasFinished();
	}
    
    private void parseChunkSize(byte c) throws ParserFormatException {
    	
    	switch(chunkSizeState) {
	    	case START:
	    		if (c == LF.getValue() || (!ParseUtils.isHexadecimal(c) && c != CR.getValue()))
	    			handleChunkSizeError();
	    		
	    		if (c == CR.getValue()) {
	    			if (!chunkSizeFound)
						handleChunkSizeError();					
					
					chunkSizeState = ChunkSizeState.END_LINE_CR;
				}		
				else {
					chunkSize = chunkSize*HEXA_BASE_VALUE.getValue() + c 
							- (byte) (ParseUtils.isDigit(c) ? '0' : 'A' - A_DECIMAL_VALUE.getValue());
					chunkSizeFound = true;
				}
					
				break;
				
			case END_LINE_CR:
				
				if (c == LF.getValue()) {
					
					chunkSizeState = chunkSizeisZero() ? ChunkSizeState.CHUNKSIZE_IS_ZERO : 
						ChunkSizeState.END_OK;
					
					parserState = ParserState.READ_CHUNK;
					chunkState = ChunkState.START;
					chunkSizeFound = false;
				}
				else
					handleChunkSizeError();
				
				break;
				
			default:
				handleChunkSizeError();
    	}
    }
    
    private void parseChunk(byte c) throws ParserFormatException {

    	switch(chunkState) {
    		case START:
    			
    			if (chunkSize > 0) {
    				chunkState = ChunkState.START;
    		    	chunkSize--;
    			}
    			
    			else if (chunkSize == 0) {
    				    				
    				if (c != CR.getValue())
    					handleChunkError();
    				
    				chunkState = ChunkState.END_LINE_CR;
    			}
    			
    			break;
    			
    		case END_LINE_CR:
    			
    			if (c != LF.getValue())
    				handleChunkError();
    			
    			if (chunkSizeState == ChunkSizeState.CHUNKSIZE_IS_ZERO)
    				chunkState = ChunkState.END_OK;
    			else {
    				parserState = ParserState.READ_CHUNK_SIZE;
    				chunkSizeState = ChunkSizeState.START;
    				chunkState = ChunkState.NOT_READ_YET;
    			}
    		
    			break;
    			
			default:
				handleChunkError();
    	}
    }

	@Override
	public boolean hasFinished() {
		return parserState == ParserState.READ_CHUNK && chunkState == ChunkState.END_OK
				&& chunkSizeState == ChunkSizeState.CHUNKSIZE_IS_ZERO;
	}	
	
	private boolean chunkSizeisZero() {
		return chunkSize == 0;
	}
	
    private void handleParserError() throws ParserFormatException {
        parserState = ParserState.ERROR;
        throw new ParserFormatException("Error while parsing");
    }

    private void handleChunkSizeError() throws ParserFormatException {
        chunkSizeState = ChunkSizeState.ERROR;
        throw new ParserFormatException("Error while parsing chunk size");
    }
    
    private void handleChunkError() throws ParserFormatException {
        chunkState = ChunkState.ERROR;
        throw new ParserFormatException("Error while parsing body");
    }

	public void reset() {
		parserState = ParserState.READ_CHUNK_SIZE;
    	chunkSizeState = ChunkSizeState.START;
    	chunkState = ChunkState.NOT_READ_YET;
	}
}
