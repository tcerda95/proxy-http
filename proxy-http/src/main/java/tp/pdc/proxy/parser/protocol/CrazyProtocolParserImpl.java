package tp.pdc.proxy.parser.protocol;

import java.nio.ByteBuffer;
import java.util.EnumSet;

import tp.pdc.proxy.ProxyProperties;
import tp.pdc.proxy.exceptions.ParserFormatException;
import tp.pdc.proxy.header.Method;
import tp.pdc.proxy.header.StatusCode;
import tp.pdc.proxy.header.protocol.CrazyProtocolHeader;
import tp.pdc.proxy.parser.interfaces.CrazyProtocolParser;
import tp.pdc.proxy.parser.utils.ParseUtils;
import static tp.pdc.proxy.parser.utils.AsciiConstants.*;
import static tp.pdc.proxy.parser.utils.DecimalConstants.*;


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
	private static final int HTTP_STATUS_CODE_LEN = 3;
	private static final int MAX_METHOD_LEN = 7;
	private static final int MAX_CRAZYPROTOCOL_HEADER_LEN = 24;
	
    private ParserState parserState;
    private HeaderState headerState;
    private ArgumentCountState argumentCountState;
    private ContentState contentState;
    
	private ByteBuffer headerName;
	private ByteBuffer argument;
	// stores the current argument received from METHOD_COUNT or STATUS_CODE_COUNT
	
	private CrazyProtocolHeader currentHeader;
	
	private EnumSet<StatusCode> statusCodesFound;
	private EnumSet<Method> methodsFound;

	private int argumentCount;
	
	//TODO: cambiar size que no sea el mismo máximo que el de los headers HTTP
    private static final int HEADER_NAME_SIZE = ProxyProperties.getInstance().getHeaderNameBufferSize();
    private static final int HEADER_CONTENT_SIZE = ProxyProperties.getInstance().getHeaderContentBufferSize();
    
    CrazyProtocolParserImpl() {
    	parserState = ParserState.READ_HEADER;
    	headerState = HeaderState.START;
    	argumentCountState = ArgumentCountState.NOT_READ_YET;
    	contentState = ContentState.NOT_READ_YET;
    	headerName = ByteBuffer.allocate(HEADER_NAME_SIZE);
    	argument = ByteBuffer.allocate(HEADER_CONTENT_SIZE);
    	
    	// creates an empty enum set
    	statusCodesFound = EnumSet.noneOf(StatusCode.class);
    	methodsFound = EnumSet.noneOf(Method.class);
    	
    	argumentCount = 0;
    }

    @Override
	public boolean parse(ByteBuffer input, ByteBuffer output) throws ParserFormatException {
    	
    	while (input.hasRemaining() && output.hasRemaining()) {
    		byte c = input.get();
    		
    		switch(parserState) {
	    		case READ_HEADER:	    			
	    			parseHeader(c);
	    			break;
	    		
	    		case READ_ARGUMENT_COUNT:
	    			parseArgumentCount(c);	  
	    			break;
	    			
	    		case READ_CONTENT:
	    			parseContent(c);
	    			break;
	    			
	    		default:
	    			handleParserError();
    		}
			output.put(c);
    	}
    	
    	return hasFinished();
	}
    
	public void parseHeader(byte c) throws ParserFormatException {
    		
		switch(headerState) {
			
    		case START:
    			
    			if (!ParseUtils.isAlphabetic(c) && c != CR.getValue() && c != US.getValue())
    				handleParserError();
    			
    			if (c == CR.getValue())
    				headerState = HeaderState.END_LINE_CR;
    			else {
    				headerName.put((byte) Character.toLowerCase(c));
    				
    				//to avoid buffer overflow
    				if (headerName.position() > MAX_CRAZYPROTOCOL_HEADER_LEN + 1)
    					handleHeaderError();
    			}
    			
    			break;
    		
    		case END_LINE_CR:
    			
    			if (c != LF.getValue())
    				handleParserError();
    			
    			headerName.flip();
    	        int nameLen = headerName.remaining();
    			currentHeader = CrazyProtocolHeader.getHeaderByBytes(headerName, nameLen);
    			
    			if (currentHeader == null)
    				handleParserError();
    			
    			if (currentHeader == CrazyProtocolHeader.END)
    				parserState = ParserState.END_OK;
    			
    			
    			else if (currentHeader == CrazyProtocolHeader.STATUS_CODE_COUNT || 
    					currentHeader == CrazyProtocolHeader.METHOD_COUNT)	{
    				
    				headerState = HeaderState.END_OK;
    				argumentCountState = ArgumentCountState.START;
    				contentState = ContentState.NOT_READ_YET;
    				parserState = ParserState.READ_ARGUMENT_COUNT;
    				
    			}
    			else {
    				headerState = HeaderState.START;
    				//generate the output
    				generateOutput();
    				CleanCurrentHeader();
    			}
    			
    			break;
    			
    		default:
    			handleHeaderError();	
		}
	}
	
	public void parseArgumentCount(byte c) throws ParserFormatException {
		// solo va a aceptar numeros decimales por ahora
		switch(argumentCountState) {
			
			case START:
				
				if (c != AS.getValue())
					handleArgumentCountError();
				
				argumentCountState = ArgumentCountState.ASTERISK;
				
				break;
				
			case ASTERISK:
				
				if (!ParseUtils.isDigit(c) && c != CR.getValue())
					handleArgumentCountError();
				
				if (c == CR.getValue()) {
					if (argumentCount == 0)
						handleArgumentCountError();
					
					argumentCountState = ArgumentCountState.END_LINE_CR;
				}
				else {
					argumentCount = argumentCount*DECIMAL_BASE_VALUE.getValue() + c - '0';
					
					if (argumentCount > MAX_ARG_COUNT)
						handleArgumentCountError();
				}
				
				break;
		
			case END_LINE_CR:
				
				if (c != LF.getValue())
					handleArgumentCountError();
				
				argumentCountState = ArgumentCountState.END_OK;
				contentState = ContentState.START;
				parserState = ParserState.READ_CONTENT;
				
				break;
				
			default:
				handleArgumentCountError();
		}
		
	}
	
	
	public void parseContent(byte c) throws ParserFormatException {
		
		switch(contentState) {
			
			case START:
				// only 2 headers that receive arguments
				if (currentHeader == CrazyProtocolHeader.STATUS_CODE_COUNT) {
					
					if (!ParseUtils.isDigit(c) && c != CR.getValue())
						handleContentError();
					
					if (c == CR.getValue()) {						
						argument.flip();
		    	        int argumentLen = argument.remaining();
		    	        
		    	        if (argumentLen != HTTP_STATUS_CODE_LEN)
		    	        	handleContentError();
		    	        
		    	        StatusCode currentStatusCode = StatusCode.getStatusCodeByBytes(argument, 
		    	        		HTTP_STATUS_CODE_LEN);
		    	        
		    	        if (currentStatusCode == null)
		    	        	handleContentError();
		    	        
						if (!statusCodesFound.contains(currentStatusCode))
							statusCodesFound.add(currentStatusCode);
						
						argument.clear();
						
						contentState = ContentState.END_LINE_CR;
					}
					else {
						argument.put(c);
						
						//to avoid buffer overflow
						if (argument.position() > HTTP_STATUS_CODE_LEN + 1)
							handleContentError();
					}
				}
				else {
					
					if (!ParseUtils.isAlphabetic(c) && c != CR.getValue())
						handleContentError();
					
					if (c == CR.getValue()) {						
						argument.flip();
		    	        int argumentLen = argument.remaining();
		    	        
		    	        Method currentMethod = Method.getByBytes(argument,
		    	        		argumentLen);
		    	        
		    	        if (currentMethod == null)
		    	        	handleContentError();
						
		    	        if (!methodsFound.contains(currentMethod))
							methodsFound.add(currentMethod);
						
						argument.clear();
						
						contentState = ContentState.END_LINE_CR;
					}
					else {
						argument.put((byte) Character.toLowerCase(c));	
						
						//to avoid buffer overflow
						if (argument.position() > MAX_METHOD_LEN + 1)
							handleContentError();
					}
				}
				
				break;
				
			case END_LINE_CR:
				
				if (c != LF.getValue())
					handleContentError();
				
				argumentCount--;
				
				// we have to generate the output
				if (argumentCount == 0) {
					generateOutput();
					
					contentState = ContentState.END_OK;
					headerState = HeaderState.START;
					
					resetSets();
				}
				else
					contentState = ContentState.START;
				
				break;
					
					
			default:
				handleContentError();
		}
		
	}

	private void generateOutput() {
		
	}
	
	private void CleanCurrentHeader() {
		currentHeader = null;
		headerName.clear();
	}
	
	@Override
	public boolean hasFinished() {
		return parserState == ParserState.END_OK;
	}

	@Override
	public void reset() {
		resetStates();
		resetSets();
		CleanCurrentHeader();
		argument.clear();
		argumentCount = 0;
	}
	
	private void resetSets() {
		statusCodesFound.clear();
		methodsFound.clear();
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
