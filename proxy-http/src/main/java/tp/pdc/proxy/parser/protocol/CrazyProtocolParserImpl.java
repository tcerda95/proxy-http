package tp.pdc.proxy.parser.protocol;

import static tp.pdc.proxy.parser.utils.AsciiConstants.AS;
import static tp.pdc.proxy.parser.utils.AsciiConstants.CR;
import static tp.pdc.proxy.parser.utils.AsciiConstants.LF;
import static tp.pdc.proxy.parser.utils.AsciiConstants.US;
import static tp.pdc.proxy.parser.utils.DecimalConstants.DECIMAL_BASE_VALUE;

import java.nio.ByteBuffer;

import tp.pdc.proxy.ByteBufferFactory;
import tp.pdc.proxy.ProxyProperties;
import tp.pdc.proxy.exceptions.ParserFormatException;
import tp.pdc.proxy.header.Method;
import tp.pdc.proxy.header.protocol.CrazyProtocolHeader;
import tp.pdc.proxy.metric.interfaces.ClientMetric;
import tp.pdc.proxy.metric.interfaces.ServerMetric;
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
	private static final int MAX_FIRST_STATUSCODE_DIGIT = 5;
	private static final int MAX_METHOD_LEN = Method.maxMethodLen();
	private static final int MAX_CRAZYPROTOCOL_HEADER_LEN = CrazyProtocolHeader.maxHeaderLen();
	
    private ParserState parserState;
    private HeaderState headerState;
    private ArgumentCountState argumentCountState;
    private ContentState contentState;
    
	private ByteBuffer headerName;
	private ByteBuffer argumentCount;
	private ByteBuffer currentArgument;
	
	private CrazyProtocolHeader currentHeader;

	private int argumentNumber;
	private int bufferSize;
	private int statusCodeNumber;
	
	private CrazyProtocolOutputGenerator outputGenerator;
		
	private static final ProxyProperties PROPERTIES = ProxyProperties.getInstance();
	private static final int HEADER_NAME_SIZE = PROPERTIES.getProtocolHeaderNameBufferSize();
	private static final int HEADER_CONTENT_SIZE = PROPERTIES.getProtocolHeaderContentBufferSize();
    
	public CrazyProtocolParserImpl(ClientMetric clientMetrics, ServerMetric serverMetrics) {
		parserState = ParserState.READ_HEADER;
    	headerState = HeaderState.START;
    	argumentCountState = ArgumentCountState.NOT_READ_YET;
    	contentState = ContentState.NOT_READ_YET;
    	headerName = ByteBuffer.allocate(HEADER_NAME_SIZE);
    	argumentCount = ByteBuffer.allocate(HEADER_CONTENT_SIZE);
    	currentArgument = ByteBuffer.allocate(HEADER_CONTENT_SIZE);
    	
    	argumentNumber = 0;
    	bufferSize = 0;
    	statusCodeNumber = 0;
    	
    	outputGenerator = new CrazyProtocolOutputGenerator(clientMetrics, serverMetrics);
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
    			
    			if (c == CR.getValue())
    				headerState = HeaderState.END_LINE_CR;
    			else {
    				if (!ParseUtils.isAlphaNumerical(c) && c != US.getValue())
        				notRecoverableError(headerName, c, CrazyProtocolError.NOT_VALID, output);
    				
    				//to avoid buffer overflow
    				if (headerName.position() >= MAX_CRAZYPROTOCOL_HEADER_LEN)
        				notRecoverableError(headerName, c, CrazyProtocolError.TOO_LONG, output);

    				headerName.put((byte) Character.toLowerCase(c));
    			}
    			
    			break;
    		
    		case END_LINE_CR:
    			
    			if (c != LF.getValue()) {
    				headerName.put(CR.getValue());
    				notRecoverableError(headerName, c, CrazyProtocolError.NOT_VALID, output);
    			}
    			
    			headerName.flip();
    	        int headerLen = headerName.remaining();
    			currentHeader = CrazyProtocolHeader.getHeaderByBytes(headerName, headerLen);
    				
    			if (currentHeader != null) {
    				outputGenerator.generateOutput(currentHeader, output);
    				
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
        				clearCurrentHeader();
        			}
    			}
    			else {
    				
    				if (headerLen == 0)
        				NotRecoverableError(headerName, CrazyProtocolError.NOT_VALID, output);

    				outputGenerator.generateErrorOutput(headerName, CrazyProtocolError.NO_MATCH, output);
    				headerState = HeaderState.START;
    				clearCurrentHeader();
    			}
    			break;
    			
    		default:
    			handleHeaderError();	
		}
	}
	
	//only called by headers that receive arguments
	public void parseArgumentCount(byte c, ByteBuffer output) throws ParserFormatException {

		switch(argumentCountState) {
			
			case START:
				
				if (c != AS.getValue())
					notRecoverableError(argumentCount, c, CrazyProtocolError.NOT_VALID, output);
				
				argumentCount.put(c);

				argumentCountState = ArgumentCountState.ASTERISK;
				
				break;
				
			case ASTERISK:				
				
				if (c == CR.getValue()) 
					argumentCountState = ArgumentCountState.END_LINE_CR;
				else {
					
					if (!ParseUtils.isDigit(c))
						notRecoverableError(argumentCount, c, CrazyProtocolError.NOT_VALID, output);

					argumentNumber = argumentNumber*DECIMAL_BASE_VALUE.getValue() + c - '0';
					
					if (argumentNumber > MAX_ARG_COUNT || ( argumentNumber > 1 &&
							currentHeader == CrazyProtocolHeader.SET_PROXY_BUF_SIZE ))
						notRecoverableError(argumentCount, c, CrazyProtocolError.NOT_VALID, output);
					
					if (argumentNumber != 0)
						argumentCount.put(c);
				}
				
				break;
		
			case END_LINE_CR:
				
				if (argumentNumber == 0)
					NotRecoverableError(argumentCount, CrazyProtocolError.NOT_VALID, output);
				
				if (c != LF.getValue()) {
					argumentCount.put(CR.getValue());
					notRecoverableError(argumentCount, c, CrazyProtocolError.NOT_VALID, output);
				}
				argumentCount.flip();
				outputGenerator.generateOutput(argumentCount, output);
				clearCurrentArgumentCount();
				
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
		
			case SET_PROXY_BUF_SIZE:
			
			if (c == CR.getValue())
				contentState = ContentState.END_LINE_CR;
			else {
				
				if (!ParseUtils.isDigit(c))
					notRecoverableError(currentArgument, c, CrazyProtocolError.NOT_VALID, output);

				int digit = c - '0';
				
				bufferSize = bufferSize*10 + digit;
				
				if (bufferSize > ByteBufferFactory.MAX_PROXY_SIZE)
					notRecoverableError(currentArgument, c, CrazyProtocolError.NOT_VALID, output);
				
				if (bufferSize != 0)
					currentArgument.put(c);
			}
			
			break;
		
		
			case STATUS_CODE_COUNT:
							
				if (c == CR.getValue())
					contentState = ContentState.END_LINE_CR;
				else {
					
					if (!ParseUtils.isDigit(c))
						notRecoverableError(currentArgument, c, CrazyProtocolError.NOT_VALID, output);

					int digit = c - '0';
					
					int statusLen = currentArgument.position();
					
					if (statusLen >= HTTP_STATUSCODE_LEN)
						notRecoverableError(currentArgument, c, CrazyProtocolError.TOO_LONG, output);
										
					if (statusLen == 0 && digit > MAX_FIRST_STATUSCODE_DIGIT)
						notRecoverableError(currentArgument, c, CrazyProtocolError.NOT_VALID, output);
				
					statusCodeNumber = statusCodeNumber*10 + digit;
					
					if (statusCodeNumber != 0)
						currentArgument.put(c);
				}
				
				break;
	
			
			case METHOD_COUNT: 
					
				if (c == CR.getValue())					
					contentState = ContentState.END_LINE_CR;
				else {
					
					if (!ParseUtils.isAlphabetic(c))
						notRecoverableError(currentArgument, c, CrazyProtocolError.NOT_VALID, output);
					
					//to avoid buffer overflow
					if (currentArgument.position() >= MAX_METHOD_LEN)
						notRecoverableError(currentArgument, c, CrazyProtocolError.TOO_LONG, output);

					currentArgument.put((byte) Character.toUpperCase(c));	
				}
	
				break;
				

		
			default:
				handleContentError();
		}
		
	}
	
	private void contentEndLineCase(byte c, ByteBuffer output) throws ParserFormatException {
		
		switch (currentHeader) {
			
			case SET_PROXY_BUF_SIZE:
				
				if (c != LF.getValue()) {
					currentArgument.put(CR.getValue());
					notRecoverableError(currentArgument, c, CrazyProtocolError.NOT_VALID, output);
				}
				
				if (bufferSize < ByteBufferFactory.MIN_PROXY_SIZE)
					NotRecoverableError(currentArgument, CrazyProtocolError.NOT_VALID, output);
		       
		        outputGenerator.generateOutput(bufferSize, output, currentHeader);
	
				clearCurrentArgument();
				bufferSize = 0;
				
				break;
			
		
			case STATUS_CODE_COUNT:
				
				int statusLen = currentArgument.position();
				
				if (statusLen != HTTP_STATUSCODE_LEN)
					NotRecoverableError(currentArgument, CrazyProtocolError.NOT_VALID, output);
				
				if (c != LF.getValue()) {
					currentArgument.put(CR.getValue());
					notRecoverableError(currentArgument, c, CrazyProtocolError.NOT_VALID, output);
				}
    	       
    	        outputGenerator.generateOutput(statusCodeNumber, output, currentHeader);

				clearCurrentArgument();
				statusCodeNumber = 0;
				
				break;
			
			case METHOD_COUNT:
				
				if (c != LF.getValue()) {
					currentArgument.put(CR.getValue());
					notRecoverableError(currentArgument, c, CrazyProtocolError.NOT_VALID, output);
				}
				
				currentArgument.flip();
    	        int methodLen = currentArgument.remaining();    	        
    	        Method currentMethod = Method.getByBytes(currentArgument, methodLen);
    	        
    	        if (currentMethod == null) {
    				
    	        	if (methodLen == 0)
        				NotRecoverableError(headerName, CrazyProtocolError.NOT_VALID, output);
    	        	
    	        	outputGenerator.generateErrorOutput(currentArgument, CrazyProtocolError.NO_MATCH, output);
    	        }
    	        else
    	        	outputGenerator.generateOutput(currentMethod, output);
    	        
    	        clearCurrentArgument();
			
				break;
				
			default:
				handleContentError();
		}
		
		argumentNumber--;
		
		if (argumentNumber == 0) {
			
			contentState = ContentState.END_OK;
			headerState = HeaderState.START;
			parserState = ParserState.READ_HEADER;
			
			clearCurrentHeader();
		}
		else
			contentState = ContentState.START;
		
		
	}
	
	@Override
	public boolean hasFinished() {
		return parserState == ParserState.END_OK && outputGenerator.hasFinished();
	}

	@Override
	public void reset() {
		resetStates();
		clearCurrentHeader();
		clearCurrentArgumentCount();
		clearCurrentArgument();
		outputGenerator.reset();
		
		argumentNumber = 0;
		bufferSize = 0;
		statusCodeNumber = 0;
	}
	
	private void notRecoverableError(ByteBuffer input,byte c, CrazyProtocolError errorCode, 
			ByteBuffer output) 
			throws ParserFormatException {

		input.put(c);
		NotRecoverableError(input, errorCode, output);
	}

	private void NotRecoverableError(ByteBuffer input, CrazyProtocolError errorCode, 
			ByteBuffer output) 
			throws ParserFormatException {
		
		input.flip();
		outputGenerator.generateErrorOutput(input, errorCode, output);
		
		if (headerState != HeaderState.END_OK)
			handleHeaderError();
		
		if (argumentCountState != ArgumentCountState.END_OK)
			handleArgumentCountError();
		
		if (contentState != ContentState.END_OK)
			handleContentError();
		
	}
	
	
	private boolean headerReceivesArguments(CrazyProtocolHeader header) {
		return (header == CrazyProtocolHeader.METHOD_COUNT ||
				header == CrazyProtocolHeader.STATUS_CODE_COUNT ||
				header == CrazyProtocolHeader.SET_PROXY_BUF_SIZE);
	}
	
	private void clearCurrentHeader() {
		currentHeader = null;
		headerName.clear();
	}
		
	private void clearCurrentArgumentCount() {
		argumentCount.clear();
	}
	
	private void clearCurrentArgument() {
		currentArgument.clear();
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
