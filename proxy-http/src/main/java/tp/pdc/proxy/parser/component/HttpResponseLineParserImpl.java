package tp.pdc.proxy.parser.component;

import static tp.pdc.proxy.parser.utils.AsciiConstants.*;
import tp.pdc.proxy.exceptions.ParserFormatException;
import tp.pdc.proxy.parser.interfaces.HttpResponseLineParser;
import tp.pdc.proxy.parser.interfaces.HttpVersionParser;
import tp.pdc.proxy.parser.utils.ParseUtils;

import java.nio.ByteBuffer;
import java.util.NoSuchElementException;

public class HttpResponseLineParserImpl implements HttpResponseLineParser {

    private HttpVersionParser versionParser;
    private int statusCode;
    private ResponseLineState state;

    @Override public boolean readMinorVersion () {
        return versionParser.readMinorVersion();
    }

    @Override public boolean readMajorVersion () {
        return versionParser.readMajorVersion();
    }

    @Override public int getMajorHttpVersion () {
        return versionParser.getMajorHttpVersion();
    }

    @Override public int getMinorHttpVersion () {
        return versionParser.getMinorHttpVersion();
    }

    @Override public byte[] getWholeVersionBytes () {
        return versionParser.getWholeVersionBytes();
    }

    private enum ResponseLineState {
        HTTP_VERSION, STATUS_CODE, REASON_PHRASE, CR_END, READ_OK, ERROR,
    }

    public HttpResponseLineParserImpl () {
        versionParser = new HttpVersionParserImpl(SP.getValue());
        statusCode = 0;
        state = ResponseLineState.HTTP_VERSION;
    }

    @Override
    public int getStatusCode() {
        if (hasStatusCode())
            return statusCode;
        throw new NoSuchElementException("Status code not read");
    }

    @Override
    public boolean hasStatusCode() {
        return statusCode != 0;
    }

    public boolean parse(ByteBuffer inputBuffer, ByteBuffer outputBuffer)
        throws ParserFormatException {
        byte b;
        while (inputBuffer.hasRemaining() && outputBuffer.hasRemaining()) {
            switch (state) {
                case HTTP_VERSION:
                    boolean ended = versionParser.parse(inputBuffer, outputBuffer);
                    if (ended)
                        state = ResponseLineState.STATUS_CODE;
                    break;

                case STATUS_CODE:
                    b = inputBuffer.get();
                    if (ParseUtils.isDigit(b)) {
                        statusCode *= 10;
                        statusCode += b - '0';
                        outputBuffer.put(b);
                    } else if (b == ' ') {
                        state = ResponseLineState.REASON_PHRASE;
                        outputBuffer.put(b);
                    } else {
                        handleError();
                    }
                    break;

                case REASON_PHRASE:
                    b = inputBuffer.get();
                    if (ParseUtils.isText(b)) {
                        outputBuffer.put(b);
                    } else if (b == CR.getValue()) {
                    	outputBuffer.put(b);
                        state = ResponseLineState.CR_END;
                    } else {
                        handleError();
                    }
                    break;

                case CR_END:
                	b = inputBuffer.get();
                    if (b == LF.getValue()) {
                    	outputBuffer.put(b);
                        state = ResponseLineState.READ_OK;
                        return true;
                    }
                    else
                        handleError();
                    break;
                    
                case READ_OK: case ERROR:
                	handleError();
            }
        }
        
        return false;
    }

    private void handleError() throws ParserFormatException {
        state = ResponseLineState.ERROR;
        throw new ParserFormatException("Error while parsing response first line");
    }

	@Override
	public boolean hasFinished() {
		return state == ResponseLineState.READ_OK;
	}

    @Override public void reset () {
        statusCode = 0;
        state = ResponseLineState.HTTP_VERSION;
        versionParser.reset();
    }
}
