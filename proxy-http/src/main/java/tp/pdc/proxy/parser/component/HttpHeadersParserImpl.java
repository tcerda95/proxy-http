package tp.pdc.proxy.parser.component;

import tp.pdc.proxy.ProxyProperties;
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
        NAME, RELEVANT_COLON, RELEVANT_SPACE, RELEVANT_CONTENT,
        COLON, SPACE, CONTENT, IGNORED_CONTENT, IGNORED_CR,
    }

    private static final int HEADER_NAME_SIZE = ProxyProperties.getInstance().getHeaderNameBufferSize();
    private static final int HEADER_CONTENT_SIZE = ProxyProperties.getInstance().getHeaderContentBufferSize();

    private HttpHeaderState state;
    private Header currentHeader;
    private ByteBuffer headerName, headerValue;
    private final Map<Header, byte[]> savedHeaders;

    private Set<Header> headersToSave;
    private Set<Header> headersToRemove;
    private Map<Header, byte[]> headersToAdd;
    private Iterator<Map.Entry<Header, byte[]>> headersToAddIter;

    private Map.Entry<Header, byte[]> nextToAdd;

    // Buffered bytes que todavía no se escribieron en outputBuffer
    private int buffered = 0;

    public HttpHeadersParserImpl(Map<Header, byte[]> toAdd, Set<Header> toRemove, Set<Header> toSave) {
        state = HttpHeaderState.ADD_HEADERS;
        headerName = ByteBuffer.allocate(HEADER_NAME_SIZE);
        headerValue = ByteBuffer.allocate(HEADER_CONTENT_SIZE);
        savedHeaders = new HashMap<>();

        this.headersToRemove = toRemove;
        this.headersToAdd = toAdd;
        this.headersToAddIter = toAdd.entrySet().iterator();
        this.headersToSave = toSave;
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
        while(inputBuffer.hasRemaining() && outputBuffer.remaining() > buffered)
            if (parse(inputBuffer.get(), outputBuffer))
                return true;
        return false;
    }

    private boolean parse(byte c, ByteBuffer output) throws ParserFormatException {
            switch (state) {
                case ADD_HEADERS:
                    addHeaders(output);
                    // Fallthrough

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

        buffered = headerName.position() + headerValue.position();
        return false;

    }

    private void handleHeaderName(byte c, ByteBuffer outputBuffer) {
        headerName.flip();
        int nameLen = headerName.remaining();
        currentHeader = Header.getHeaderByBytes(headerName, nameLen); //TODO: podría ser más optimo, pero no es determinante

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
        // Si no lo pude meter antes quedó acá guardado
        if (nextToAdd != null && headerFitsBuffer(nextToAdd, output))
            putHeaderInBuffer(nextToAdd, output);

        while (headersToAddIter.hasNext()) {
            nextToAdd = headersToAddIter.next();
            if (!headerFitsBuffer(nextToAdd, output)) { // 4: colon + SP + CR + LF
                return;
            }
            putHeaderInBuffer(nextToAdd, output);
            nextToAdd = null;
        }
    }

    private void putHeaderInBuffer(Map.Entry<Header, byte[]> header, ByteBuffer buffer) {
        buffer
            .put(header.getKey().getBytes())
            .put((byte) ':')
            .put(SP.getValue())
            .put(header.getValue())
            .put(CR.getValue()).put(LF.getValue());
    }

    private boolean headerFitsBuffer(Map.Entry<Header, byte[]> header, ByteBuffer buffer) {
        return buffer.remaining() >= header.getKey().getBytes().length + header.getValue().length + 4;
        // 4: colon + SP + CR + LF
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