package tp.pdc.proxy.parser;

import static tp.pdc.proxy.parser.utils.AsciiConstants.*;
import tp.pdc.proxy.exceptions.ParserFormatException;
import tp.pdc.proxy.parser.interfaces.HttpVersionParser;
import tp.pdc.proxy.parser.utils.ParseUtils;

import java.nio.ByteBuffer;

public class HttpResponseLineParserImpl {

    private HttpVersionParser versionParser;
    private int statusCode;
    private ResponseLineState state;

    private enum ResponseLineState {
        HTTP_VERSION, VERSION_END, SP_1, STATUS_CODE, SP_2, REASON_PHRASE, CR_END, READ_OK, ERROR,
    }

    public HttpResponseLineParserImpl () {
        versionParser = new HttpVersionParserImpl(SP.getValue()); // TODO: interfaz de constantes
        statusCode = 0;
        state = ResponseLineState.HTTP_VERSION;
    }

    public int getStatusCode() {
        if (hasStatusCode())
            return statusCode;
        throw new IllegalStateException(); //TODO
    }

    public boolean hasStatusCode() {
        return statusCode != 0;
    }

    public boolean parse(ByteBuffer inputBuffer, ByteBuffer outputBuffer)
        throws ParserFormatException {
        byte b;
        while (inputBuffer.hasRemaining()) {
            switch (state) {
                case HTTP_VERSION:
                    boolean ended = versionParser.parse(inputBuffer, outputBuffer);
                    if (ended)
                        state = ResponseLineState.SP_1;
                    break;

                case STATUS_CODE:
                    b = inputBuffer.get();
                    if (ParseUtils.isDigit(b)) {
                        statusCode *= 10;
                        statusCode += b - '0';
                        outputBuffer.put(b);
                    } else if (b == ' ') {
                        state = ResponseLineState.SP_2;
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
                        state = ResponseLineState.CR_END;
                    } else {
                        handleError();
                    }
                    break;

                case CR_END:
                    if (inputBuffer.get() == LF.getValue()) {
                        state = ResponseLineState.READ_OK;
                        return true;
                    }
                    else
                        handleError();
                    break;
            }
        }
        return false;
    }

    private void handleError() throws ParserFormatException {
        state = ResponseLineState.ERROR;
        throw new ParserFormatException("Error while parsing response first line");
    }
}
