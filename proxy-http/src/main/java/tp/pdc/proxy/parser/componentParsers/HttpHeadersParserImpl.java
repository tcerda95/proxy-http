package tp.pdc.proxy.parser.componentParsers;

import tp.pdc.proxy.exceptions.ParserFormatException;
import tp.pdc.proxy.header.Header;
import tp.pdc.proxy.parser.interfaces.HttpHeaderParser;
import static tp.pdc.proxy.parser.utils.AsciiConstants.*;
import tp.pdc.proxy.parser.utils.ParseUtils;

import java.nio.ByteBuffer;
import java.util.*;

public class HttpHeadersParserImpl implements HttpHeaderParser {

    private enum HttpHeaderState {
        ADD_HEADERS, LINE_START, ERROR, END_LINE_CR, SECTION_END_CR, END_OK,

        NAME,

        /* Relevant headers */
        RELEVANT_COLON, RELEVANT_SPACE, RELEVANT_CONTENT,

        IGNORED_CONTENT, IGNORED_CR,

        /* Other headers */
        COLON, SPACE, CONTENT;
    }

    private HttpHeaderState state;
    private final Map<Header, byte[]> savedHeaders;
    private ByteBuffer headerName;
    private ByteBuffer headerValue;
    private Header currentHeader;

    private Set<Header> headersToSave;

    private Set<Header> headersToRemove;
    private Map<Header, byte[]> headersToAdd;

    public HttpHeadersParserImpl(Map<Header, byte[]> toAdd, Set<Header> toRemove, Set<Header> toSave) {
        this.headersToRemove = toRemove;
        this.headersToAdd = toAdd;
        this.headersToSave = toSave;

        state = HttpHeaderState.ADD_HEADERS;
        headerName = ByteBuffer.allocate(128); //TODO: capacity
        headerValue = ByteBuffer.allocate(128);
        savedHeaders = new HashMap<>();
    }

    private void expectByteAndOutput(byte read, byte expected, HttpHeaderState next, ByteBuffer outputBuffer) throws ParserFormatException {
        if (read == expected) {
            outputBuffer.put(read);
            state = next;
        } else {
            handleError();
        }
    }

    private void handleError() throws ParserFormatException {
        state = HttpHeaderState.ERROR;
        throw new ParserFormatException("Error while parsing header");
    }


    @Override public boolean parse(ByteBuffer inputBuffer, ByteBuffer outputBuffer) throws ParserFormatException {
        while(inputBuffer.hasRemaining())
            if (parse(inputBuffer.get(), outputBuffer))
                return true;
        return false;
    }

    @Override public boolean parse(byte c, ByteBuffer output) throws ParserFormatException {
            switch (state) {
                case ADD_HEADERS:
                    addHeaders(output);
                    // fallthrough

                case LINE_START:
                    if (c == CR.getValue()) {
                        state = HttpHeaderState.SECTION_END_CR;
                        output.put(c);
                    } else if (ParseUtils.isHeaderNameChar(c)) {
                        // Reset
                        headerName.clear();
                        headerValue.clear();
                        currentHeader = null;

                        headerName.put((byte) Character.toLowerCase(c));
                        state = HttpHeaderState.NAME;
                    } else
                        handleError();
                    break;

                case NAME:
                    if (c == ':') {
                        handleHeaderName(c, output);
                    } else if (ParseUtils.isHeaderNameChar(c)) {
                        headerName.put((byte) Character.toLowerCase(c));
                    } else {
                        handleError();
                    }
                    break;

                case RELEVANT_COLON:
                    expectByteAndOutput(c, SP.getValue(), HttpHeaderState.RELEVANT_SPACE, output);
                    break;

                case RELEVANT_SPACE:
                    if (ParseUtils.isHeaderNameChar(c)) {
                        headerValue.put((byte) Character.toLowerCase(c));
                        state = HttpHeaderState.RELEVANT_CONTENT;
                    } else {
                        handleError();
                    }
                    break;

                case RELEVANT_CONTENT:
                    if (c == CR.getValue()) {
                        state = HttpHeaderState.END_LINE_CR;
                        headerValue.flip();
                        byte[] headerAux = new byte[headerValue.remaining()];
                        headerValue.get(headerAux);
                        output.put(headerAux).put(CR.getValue());
                        savedHeaders.put(currentHeader, headerAux);
                    } else if (ParseUtils.isHeaderContentChar(c)) {
                        headerValue.put((byte) Character.toLowerCase(c));
                    } else {
                        handleError();
                    }
                    break;

                case IGNORED_CONTENT:
                    if (c == CR.getValue()) {
                        state = HttpHeaderState.IGNORED_CR;
                    } else if (!ParseUtils.isHeaderContentChar(c)) {
                        handleError();
                    }
                    break;

                case IGNORED_CR:
                    if (c == LF.getValue()) {
                        state = HttpHeaderState.LINE_START;
                    } else {
                        handleError();
                    }
                    break;

                case COLON:
                    expectByteAndOutput(c, SP.getValue(), HttpHeaderState.SPACE, output);
                    break;

                case SPACE:
                    if (ParseUtils.isHeaderContentChar(c)) {
                        state = HttpHeaderState.CONTENT;
                        output.put(c);
                    } else {
                        handleError();
                    }
                    break;

                case CONTENT:
                    if (c == CR.getValue()) {
                        state = HttpHeaderState.END_LINE_CR;
                        output.put(c);
                    } else if (ParseUtils.isHeaderContentChar(c)) {
                        output.put(c);
                    } else {
                        handleError();
                    }
                    break;

                case END_LINE_CR:
                    expectByteAndOutput(c, LF.getValue(), HttpHeaderState.LINE_START, output);
                    break;

                case SECTION_END_CR:
                    expectByteAndOutput(c, LF.getValue(), HttpHeaderState.END_OK, output);
                    return true;

                default: // ERROR
                    throw new IllegalStateException();
            }

        return false;

    }

    private void handleHeaderName(byte c, ByteBuffer outputBuffer) {
        headerName.flip();
        int nameLen = headerName.remaining();
        currentHeader = Header.getHeaderByBytes(headerName, nameLen);

        if (headersToRemove.contains(currentHeader) || headersToAdd.containsKey(currentHeader) /* gets changed */) {
            state = HttpHeaderState.IGNORED_CONTENT;
            return;
        }

        outputBuffer.put(headerName).put(c);

        if (headersToSave.contains(currentHeader)) {
            savedHeaders.put(currentHeader, new byte[0]);
            state = HttpHeaderState.RELEVANT_COLON;
        } else {
            state = HttpHeaderState.COLON;
        }
    }

    private void addHeaders(ByteBuffer output) {
        byte colon = (byte) ':';
        for (Map.Entry<Header, byte[]> e: headersToAdd.entrySet()) {
            output
                .put(e.getKey().getBytes())
                .put(colon)
                .put(SP.getValue())
                .put(e.getValue())
                .put(CR.getValue()).put(LF.getValue());
        }
    }

    @Override public boolean hasFinished() {
        return this.state == HttpHeaderState.END_OK;
    }

    @Override public void reset () {
        headerName.clear(); headerValue.clear();
        savedHeaders.clear();
        state = HttpHeaderState.ADD_HEADERS;
    }

    @Override public byte[] getHeaderValue(Header header) {
        return this.savedHeaders.get(header);
    }

    @Override public boolean hasHeaderValue(Header header) {
        return this.savedHeaders.containsKey(header);
    }

    public Map<Header, byte[]> getSavedHeaders () {
        return savedHeaders;
    }
}
