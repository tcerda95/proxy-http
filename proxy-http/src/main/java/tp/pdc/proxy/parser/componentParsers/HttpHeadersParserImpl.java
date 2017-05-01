package tp.pdc.proxy.parser.componentParsers;

import tp.pdc.proxy.exceptions.ParserFormatException;
import tp.pdc.proxy.header.Header;
import tp.pdc.proxy.parser.interfaces.HttpHeaderParser;
import static tp.pdc.proxy.parser.utils.AsciiConstants.*;
import tp.pdc.proxy.parser.utils.ParseUtils;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class HttpHeadersParserImpl implements HttpHeaderParser {

    private enum HttpHeaderState {
        START, ERROR, END_LINE_CR, SECTION_END_CR, END_OK,

        NAME,
        /* Relevant headers */
        RELEVANT_COLON, RELEVANT_SPACE, RELEVANT_CONTENT,

        /* Other headers */
        COLON, SPACE, CONTENT;
    }

    private HttpHeaderState headerState;
    private final Map<Header, byte[]> relevantHeaders;
    private ByteBuffer headerName;
    private ByteBuffer headerValue;
    private Header currentRelevantHeader;


    public HttpHeadersParserImpl () {
        headerState = HttpHeaderState.START;
        headerName = ByteBuffer.allocate(128); //TODO: capacity
        headerValue = ByteBuffer.allocate(128);
        relevantHeaders = new HashMap<>();
    }

    private void expectByteAndOutput(byte read, byte expected, HttpHeaderState next, ByteBuffer outputBuffer) throws ParserFormatException {
        if (read == expected) {
            outputBuffer.put(read);
            headerState = next;

        } else {
            handleError();
        }
    }

    private void handleError() throws ParserFormatException {
        headerState = HttpHeaderState.ERROR;
        throw new ParserFormatException("Error while parsing header");
    }


    @Override public boolean parse(ByteBuffer inputBuffer, ByteBuffer outputBuffer) throws ParserFormatException {
        while(inputBuffer.hasRemaining())
            if (parse(inputBuffer.get(), outputBuffer))
                return true;
        return false;
    }

    @Override public boolean parse(byte c, ByteBuffer outputBuffer) throws ParserFormatException {
            switch (headerState) {
                case START:
                    if (c == CR.getValue()) {
                        headerState = HttpHeaderState.SECTION_END_CR;
                        outputBuffer.put(c);
                    } else if (ParseUtils.isHeaderNameChar(c)) {
                        // Reset
                        headerName.clear();
                        headerValue.clear();
                        currentRelevantHeader = null;

                        headerName.put((byte) Character.toLowerCase(c));
                        headerState = HttpHeaderState.NAME;
                    } else
                        handleError();
                    break;

                case NAME:
                    if (c == ':') {
                        headerName.flip();
                        int nameLen = headerName.remaining();
                        currentRelevantHeader = Header.getByBytes(headerName, nameLen);

                        // if !unwantedHeader
                        outputBuffer.put(headerName).put(c);

                        if (currentRelevantHeader != null) {
                            relevantHeaders.put(currentRelevantHeader, new byte[0]);
                            headerState = HttpHeaderState.RELEVANT_COLON;
                        } else {
                            headerState = HttpHeaderState.COLON;
                        }
                    } else if (ParseUtils.isHeaderNameChar(c)) {
                        headerName.put((byte) Character.toLowerCase(c));
                    } else {
                        handleError();
                    }
                    break;

                case RELEVANT_COLON:
                    expectByteAndOutput(c, SP.getValue(), HttpHeaderState.RELEVANT_SPACE, outputBuffer);
                    break;

                case RELEVANT_SPACE:
                    if (ParseUtils.isHeaderNameChar(c)) {
                        headerValue.put((byte) Character.toLowerCase(c));
                        headerState = HttpHeaderState.RELEVANT_CONTENT;
                    } else {
                        handleError();
                    }
                    break;

                case RELEVANT_CONTENT:
                    if (c == CR.getValue()) {
                        headerState = HttpHeaderState.END_LINE_CR;
                        headerValue.flip();
                        byte[] headerAux = new byte[headerValue.remaining()];
                        headerValue.get(headerAux);
                        outputBuffer.put(headerAux).put(CR.getValue());
                        relevantHeaders.put(currentRelevantHeader, headerAux);
                    } else if (ParseUtils.isHeaderContentChar(c)) {
                        headerValue.put((byte) Character.toLowerCase(c));
                    }
                    break;

                case COLON:
                    expectByteAndOutput(c, SP.getValue(), HttpHeaderState.SPACE, outputBuffer);
                    break;

                case SPACE:
                    if (ParseUtils.isHeaderContentChar(c)) {
                        headerState = HttpHeaderState.CONTENT;
                        outputBuffer.put(c);
                    } else {
                        handleError();
                    }
                    break;

                case CONTENT:
                    if (c == CR.getValue()) {
                        headerState = HttpHeaderState.END_LINE_CR;
                        outputBuffer.put(c);
                    } else if (ParseUtils.isHeaderContentChar(c)) {
                        outputBuffer.put(c);
                    } else {
                        handleError();
                    }
                    break;

                case END_LINE_CR:
                    expectByteAndOutput(c, LF.getValue(), HttpHeaderState.START, outputBuffer);
                    break;

                case SECTION_END_CR:
                    expectByteAndOutput(c, LF.getValue(), HttpHeaderState.END_OK, outputBuffer);
                    return true;

                default: // ERROR
                    throw new IllegalStateException();
            }

        return false;

    }

    public HttpHeaderState getState(){
        return this.headerState;
    }

    @Override public boolean hasFinished(){
        return this.headerState  == HttpHeaderState.END_OK;
    }

    @Override public byte[] getHeaderValue(Header header){
        return this.relevantHeaders.get(header);
    }

    @Override public boolean hasHeaderValue(Header header){
        return this.relevantHeaders.containsKey(header);
    }

    public Map<Header, byte[]> getRelevantHeaders() {
        return relevantHeaders;
    }
}