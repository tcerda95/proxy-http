package tp.pdc.proxy.parser.protocol;

import static tp.pdc.proxy.parser.utils.AsciiConstants.AS;
import static tp.pdc.proxy.parser.utils.AsciiConstants.CR;
import static tp.pdc.proxy.parser.utils.AsciiConstants.LF;
import static tp.pdc.proxy.parser.utils.AsciiConstants.US;
import static tp.pdc.proxy.parser.utils.DecimalConstants.DECIMAL_BASE_VALUE;

import java.nio.ByteBuffer;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

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
	private static final int MAX_CRAZYPROTOCOL_HEADER_LEN = 24;
	
    private ParserState parserState;
    private HeaderState headerState;
    private ArgumentCountState argumentCountState;
    private ContentState contentState;
    
	private ByteBuffer headerName;
	private ByteBuffer methodName;
	// stores the current argument received from METHOD_COUNT
	
	private CrazyProtocolHeader currentHeader;
	
	private Set<Integer> statusCodesFound;
	private Set<Method> methodsFound;

	private int argumentCount;
	private int currentStatusCode;
	private int statusCodeLen;
	
	//TODO: cambiar size que no sea el mismo máximo que el de los headers HTTP
    private static final int HEADER_NAME_SIZE = ProxyProperties.getInstance().getHeaderNameBufferSize();
    private static final int HEADER_CONTENT_SIZE = ProxyProperties.getInstance().getHeaderContentBufferSize();
    
    public CrazyProtocolParserImpl() {
    	parserState = ParserState.READ_HEADER;
    	headerState = HeaderState.START;
    	argumentCountState = ArgumentCountState.NOT_READ_YET;
    	contentState = ContentState.NOT_READ_YET;
    	headerName = ByteBuffer.allocate(HEADER_NAME_SIZE);
    	methodName = ByteBuffer.allocate(HEADER_CONTENT_SIZE);
    	
    	// creates an empty enum set
    	methodsFound = EnumSet.noneOf(Method.class);
    	statusCodesFound = new HashSet<Integer>();
    	
    	argumentCount = 0;
    	currentStatusCode = 0;
    	statusCodeLen = 0;
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
    			
    			if (!ParseUtils.isAlphaNumerical(c) && c != CR.getValue() && c != US.getValue())
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
    				
    			else if (headerReceivesArguments(currentHeader)) {
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
		
	//only called by headers that receive arguments
	public void parseContent(byte c) throws ParserFormatException {
		
		switch(contentState) {
			
			case START:
				contentStartCase(c);
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
	
	private void contentStartCase(byte c) throws ParserFormatException {
		
		switch(currentHeader) {
		
			case STATUS_CODE_COUNT:
				
				if (!ParseUtils.isDigit(c) && c != CR.getValue())
					handleContentError();
				
				if (c == CR.getValue()) {						
					methodName.flip();
	    	        int argumentLen = methodName.remaining();
	    	        
	    	        if (argumentLen != HTTP_STATUSCODE_LEN)
	    	        	handleContentError();
	    	        
					if (!statusCodesFound.contains(currentStatusCode))
						statusCodesFound.add(currentStatusCode);
					
					statusCodeLen = 0;
					currentStatusCode = 0;
					
					contentState = ContentState.END_LINE_CR;
				}
				else {
					statusCodeLen++;
					
					currentStatusCode = currentStatusCode*DECIMAL_BASE_VALUE.getValue() - '0';
					
					if (statusCodeLen == 1 && currentStatusCode > MAX_MOST_SIGNIFICATIVE_STATUSCODE_DIGIT)
						handleContentError();
					
					//to avoid buffer overflow
					if (statusCodeLen > HTTP_STATUSCODE_LEN + 1)
						handleContentError();
				}
				
				break;
	
			
			case METHOD_COUNT: 
				
				if (!ParseUtils.isAlphabetic(c) && c != CR.getValue())
					handleContentError();
				
				if (c == CR.getValue()) {						
					methodName.flip();
	    	        int argumentLen = methodName.remaining();
	    	        
	    	        Method currentMethod = Method.getByBytes(methodName,
	    	        		argumentLen);
	    	        
	    	        if (currentMethod == null)
	    	        	handleContentError();
					
	    	        if (!methodsFound.contains(currentMethod))
						methodsFound.add(currentMethod);
					
					methodName.clear();
					
					contentState = ContentState.END_LINE_CR;
				}
				else {
					methodName.put((byte) Character.toLowerCase(c));	
					
					//to avoid buffer overflow
					if (methodName.position() > MAX_METHOD_LEN + 1)
						handleContentError();
				}
	
				break;
		
			default:
				handleContentError();
		}
		
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
		methodName.clear();
		argumentCount = 0;
		statusCodeLen = 0;
		currentStatusCode = 0;
	}
	
	private boolean headerReceivesArguments(CrazyProtocolHeader header) {
		return (header == CrazyProtocolHeader.METHOD_COUNT ||
				header == CrazyProtocolHeader.STATUS_CODE_COUNT);
	}
	
	private void generateOutput() {
		
	}
	
	private void CleanCurrentHeader() {
		currentHeader = null;
		headerName.clear();
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
