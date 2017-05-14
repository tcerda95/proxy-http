package tp.pdc.proxy.parser.protocol;

import static tp.pdc.proxy.parser.utils.AsciiConstants.*;
import static tp.pdc.proxy.parser.utils.DecimalConstants.DECIMAL_BASE_VALUE;

import java.nio.ByteBuffer;
import tp.pdc.proxy.ProxyProperties;
import tp.pdc.proxy.exceptions.ParserFormatException;
import tp.pdc.proxy.header.Method;
import tp.pdc.proxy.header.protocol.CrazyProtocolHeader;
import tp.pdc.proxy.parser.interfaces.CrazyProtocolParser;
import tp.pdc.proxy.parser.utils.ParseUtils;


public class CrazyProtocolParserImpl implements CrazyProtocolParser {
	
    private enum ParserState {
        READ_HEADER,
        READ_ARGUMENT_COUNT,
        READ_CONTENT,
        END_OK,
 
        /* Error states */
        ERROR,
    }
	
	
	private enum HeaderState {
		START,
		END_LINE_CR,
		END_OK,
		
		/* Error states */
		ERROR,
	}
	
	private enum ArgumentCountState {
		NOT_READ_YET,
		START,
		ASTERISK, 
		END_LINE_CR,
		END_OK,
		
		/*Error states */
		ERROR,
	}
	
	private enum ContentState {
		NOT_READ_YET,
		START,
		END_LINE_CR,
		END_OK,
		
		/*Error states */
		ERROR,
	}
	
	private static final int MAX_ARG_COUNT = 18;
	private static final int HTTP_STATUSCODE_LEN = 3;
	private static final int MAX_MOST_SIGNIFICATIVE_STATUSCODE_DIGIT = 5;
	private static final int MAX_METHOD_LEN = 7;
	private static final int MAX_CRAZYPROTOCOL_HEADER_LEN = 20;
	
    private ParserState parserState;
    private HeaderState headerState;
    private ArgumentCountState argumentCountState;
    private ContentState contentState;
    
	private ByteBuffer headerName;
	private ByteBuffer methodName;
	
	private CrazyProtocolHeader currentHeader;

	private int argumentCount;
	private int currentStatusCode;
	private int statusCodeLen;
	
	private CrazyProtocolOutputGenerator outputGenerator;
	
	//TODO: no necesariamente el mismo máximo que el de los headers HTTP
    private static final int HEADER_NAME_SIZE = ProxyProperties.getInstance().getHeaderNameBufferSize();
    private static final int HEADER_CONTENT_SIZE = ProxyProperties.getInstance().getHeaderContentBufferSize();
    
    public CrazyProtocolParserImpl() {
    	parserState = ParserState.READ_HEADER;
    	headerState = HeaderState.START;
    	argumentCountState = ArgumentCountState.NOT_READ_YET;
    	contentState = ContentState.NOT_READ_YET;
    	headerName = ByteBuffer.allocate(HEADER_NAME_SIZE);
    	methodName = ByteBuffer.allocate(HEADER_CONTENT_SIZE);
    	
    	argumentCount = 0;
    	currentStatusCode = 0;
    	statusCodeLen = 0;
    	
    	outputGenerator = new CrazyProtocolOutputGenerator();
    }

    @Override
	public boolean parse(ByteBuffer input, ByteBuffer output) throws ParserFormatException {
    	
    	while (input.hasRemaining() && output.hasRemaining()) {
    		byte c = input.get();
    		
    		switch(parserState) {
	    		case READ_HEADER:	    			
	    			parseHeader(c, output);
	    			break;
	    		
	    		case READ_ARGUMENT_COUNT:
	    			parseArgumentCount(c, output);	  
	    			break;
	    			
	    		case READ_CONTENT:
	    			parseContent(c, output);
	    			break;
	    			
	    		default:
	    			handleParserError();
    		}
    	}
    	
    	return hasFinished();
	}
    
	public void parseHeader(byte c, ByteBuffer output) throws ParserFormatException {
    		
		switch(headerState) {
			
    		case START:
    			
    			if (!ParseUtils.isAlphaNumerical(c) && c != CR.getValue() && c != US.getValue()) {
    				
    				if (headerName.position() == 0)
    					headerName.put(c);
    				
    				headerName.flip();
    				outputGenerator.generateOutput(headerName, CrazyProtocolInputError.NOT_VALID, output);
    				handleHeaderError();
    			}
    			
    			if (c == CR.getValue())
    				headerState = HeaderState.END_LINE_CR;
    			else {
    				headerName.put((byte) Character.toLowerCase(c));
    				
    				//to avoid buffer overflow
    				if (headerName.position() > MAX_CRAZYPROTOCOL_HEADER_LEN){
        				
    					headerName.flip();
    					outputGenerator.generateOutput(headerName, 
    							CrazyProtocolInputError.TOO_LONG, output);
    					handleHeaderError();
    				}
    			}
    			
    			break;
    		
    		case END_LINE_CR:
    			
    			if (c != LF.getValue()) {
    				headerName.flip();
    				outputGenerator.generateOutput(headerName, 
    						CrazyProtocolInputError.NOT_VALID, output);
    				handleParserError();
    			}
    			
    			headerName.flip();
    	        int nameLen = headerName.remaining();
    			currentHeader = CrazyProtocolHeader.getHeaderByBytes(headerName, nameLen);
    				
    			if (currentHeader == CrazyProtocolHeader.END)
    				parserState = ParserState.END_OK;
    			    				
    			else if (headerReceivesArguments(currentHeader)) {
    				headerState = HeaderState.END_OK;
    				argumentCountState = ArgumentCountState.START;
    				contentState = ContentState.NOT_READ_YET;
    				parserState = ParserState.READ_ARGUMENT_COUNT;
    			}
    			else		
    				headerState = HeaderState.START;
     			
    			if (currentHeader != null)
    				outputGenerator.generateOutput(currentHeader, output);
    			else
    				outputGenerator.generateOutput(headerName, CrazyProtocolInputError.NO_MATCH, 
    						output);
    				
    			if (!headerReceivesArguments(currentHeader))
    				clearCurrentHeader();
    			
    			break;
    			
    		default:
    			handleHeaderError();	
		}
	}
	
	//only called by headers that receive arguments
	public void parseArgumentCount(byte c, ByteBuffer output) throws ParserFormatException {

		switch(argumentCountState) {
		// solo va a aceptar numeros decimales por ahora
			
			case START:
				
				if (c != AS.getValue()) {
					byteOutputInputError(output, c, CrazyProtocolInputError.NOT_VALID);
					handleArgumentCountError();
				}
				
				argumentCountState = ArgumentCountState.ASTERISK;
				
				break;
				
			case ASTERISK:
				
				if (!ParseUtils.isDigit(c) && c != CR.getValue()) {
					byteOutputInputError(output, AS.getValue(), CrazyProtocolInputError.NOT_VALID);
					handleArgumentCountError();
				}
				
				if (c == CR.getValue()) {
					if (argumentCount == 0) {
						byteOutputInputError(output, AS.getValue(), CrazyProtocolInputError.NOT_VALID);
						handleArgumentCountError();
					}
					argumentCountState = ArgumentCountState.END_LINE_CR;
				}
				else {
					argumentCount = argumentCount*DECIMAL_BASE_VALUE.getValue() + c - '0';
					
					if (argumentCount > MAX_ARG_COUNT) {
						byteOutputInputError(output, AS.getValue(), CrazyProtocolInputError.TOO_LONG);
						handleArgumentCountError();
					}
				}
				
				break;
		
			case END_LINE_CR:
				
				if (c != LF.getValue()) {
					byteOutputInputError(output, AS.getValue(), CrazyProtocolInputError.NOT_VALID);
					handleArgumentCountError();
				}
				
				argumentCountState = ArgumentCountState.END_OK;
				contentState = ContentState.START;
				parserState = ParserState.READ_CONTENT;
				
				break;
				
			default:
				handleArgumentCountError();
		}
		
	}
		
	//only called by headers that receive arguments
	public void parseContent(byte c, ByteBuffer output) throws ParserFormatException {
		
		switch(contentState) {
			
			case START:
				contentStartCase(c, output);
				break;
				
			case END_LINE_CR:
				contentEndLineCase(c, output);
				break;					
					
			default:
				handleContentError();
		}
		
	}
	
	private void contentStartCase(byte c, ByteBuffer output) throws ParserFormatException {
		
		switch(currentHeader) {
		
			case STATUS_CODE_COUNT:
				
				if (!ParseUtils.isDigit(c) && c != CR.getValue()) {
					
					if (statusCodeLen == 0) {
						byteOutputInputError(output, c, CrazyProtocolInputError.NOT_VALID);
					}
					else {
						
						byte[] parsedBytes = ParseUtils.parseInt(currentStatusCode);
						outputGenerator.generateOutput(ByteBuffer.wrap(parsedBytes), 
								CrazyProtocolInputError.NOT_VALID, output);
					}
					handleContentError();
				}
				
				if (c == CR.getValue())
					contentState = ContentState.END_LINE_CR;
				else {
					statusCodeLen++;
					
					currentStatusCode = currentStatusCode*DECIMAL_BASE_VALUE.getValue() + c - '0';
					
					if (statusCodeLen == 1 && currentStatusCode > MAX_MOST_SIGNIFICATIVE_STATUSCODE_DIGIT) {
						byte[] parsedBytes = ParseUtils.parseInt(currentStatusCode);
						outputGenerator.generateOutput(ByteBuffer.wrap(parsedBytes), 
								CrazyProtocolInputError.NOT_VALID, output);
						handleContentError();
					}
					
					//to avoid buffer overflow
					if (statusCodeLen > HTTP_STATUSCODE_LEN) {
						byte[] parsedBytes = ParseUtils.parseInt(currentStatusCode);
						outputGenerator.generateOutput(ByteBuffer.wrap(parsedBytes), 
								CrazyProtocolInputError.TOO_LONG, output);
						handleContentError();
					}	
				}
				
				break;
	
			
			case METHOD_COUNT: 
				
				if (!ParseUtils.isAlphabetic(c) && c != CR.getValue()) {
					
    				if (methodName.position() == 0)
    					methodName.put(c);
    				
    				methodName.flip();
    				outputGenerator.generateOutput(methodName, 
    						CrazyProtocolInputError.NOT_VALID, output);
    				
					handleContentError();
				}
				
				if (c == CR.getValue())					
					contentState = ContentState.END_LINE_CR;
				else {
					methodName.put((byte) Character.toUpperCase(c));	
					
					//to avoid buffer overflow
					if (methodName.position() > MAX_METHOD_LEN + 1) {
	    				methodName.flip();
						outputGenerator.generateOutput(methodName, 
	    						CrazyProtocolInputError.TOO_LONG, output);
						handleContentError();
					}
				}
	
				break;
		
			default:
				handleContentError();
		}
		
	}
	
	private void contentEndLineCase(byte c, ByteBuffer output) throws ParserFormatException {
		
		switch (currentHeader) {
			
			case STATUS_CODE_COUNT:
				
				if (c != LF.getValue())
					handleContentError();
				
    	        if (statusCodeLen != HTTP_STATUSCODE_LEN)
    	        	handleContentError();
				
				outputGenerator.generateOutput(currentStatusCode, output);

				statusCodeLen = 0;
				currentStatusCode = 0;
				
				break;
			
			case METHOD_COUNT:
				
				methodName.flip();

				if (c != LF.getValue()) {
					outputGenerator.generateOutput(methodName, 
    						CrazyProtocolInputError.NOT_VALID, output);
					
					handleContentError();
				}
				
    	        int argumentLen = methodName.remaining();
    	        
    	        Method currentMethod = Method.getByBytes(methodName,
    	        		argumentLen);
    	        
    	        if (currentMethod == null)
    				outputGenerator.generateOutput(methodName, 
    						CrazyProtocolInputError.NO_MATCH, output);
    	        
    	        else
    	        	outputGenerator.generateOutput(currentMethod, output);
    	        
    	        methodName.clear();
			
				break;
				
			default:
				handleContentError();
		}
		
		argumentCount--;
		
		if (argumentCount == 0) {
			
			contentState = ContentState.END_OK;
			headerState = HeaderState.START;
			parserState = ParserState.READ_HEADER;
			
			clearCurrentHeader();
		}
		else
			contentState = ContentState.START;
		
		
	}

	
	private void byteOutputInputError(ByteBuffer output, byte c, CrazyProtocolInputError error) {
		ByteBuffer characterWrapped = ByteBuffer.allocate(1);
		characterWrapped.put(c);
		outputGenerator.generateOutput(characterWrapped, error, output);
	}
	
	@Override
	public boolean hasFinished() {
		return parserState == ParserState.END_OK && outputGenerator.hasFinished();
	}

	@Override
	public void reset() {
		resetStates();
		clearCurrentHeader();
		methodName.clear();
		outputGenerator.reset();
		
		argumentCount = 0;
		statusCodeLen = 0;
		currentStatusCode = 0;
	}
	
	private boolean headerReceivesArguments(CrazyProtocolHeader header) {
		return (header == CrazyProtocolHeader.METHOD_COUNT ||
				header == CrazyProtocolHeader.STATUS_CODE_COUNT);
	}
	
	private void clearCurrentHeader() {
		currentHeader = null;
		headerName.clear();
	}
	
	private void resetStates() {
		headerState = HeaderState.START;
	    parserState = ParserState.READ_HEADER;
	    argumentCountState = ArgumentCountState.NOT_READ_YET;
	    contentState = ContentState.NOT_READ_YET;
	}
	
    private void handleParserError() throws ParserFormatException {
        parserState = ParserState.ERROR;
        throw new ParserFormatException("Error while parsing");
    }

    private void handleHeaderError() throws ParserFormatException {
        headerState = HeaderState.ERROR;
        throw new ParserFormatException("Error while parsing header");
    }
    
    private void handleArgumentCountError() throws ParserFormatException {
        argumentCountState = ArgumentCountState.ERROR;
        throw new ParserFormatException("Error while parsing argument count");
    }
    
    private void handleContentError() throws ParserFormatException {
        contentState = ContentState.ERROR;
        throw new ParserFormatException("Error while parsing header content");
    }
}
