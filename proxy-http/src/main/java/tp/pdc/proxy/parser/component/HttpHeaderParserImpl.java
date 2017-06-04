package tp.pdc.proxy.parser.component;

import tp.pdc.proxy.HttpErrorCode;
import tp.pdc.proxy.ProxyProperties;
import tp.pdc.proxy.exceptions.ParserFormatException;
import tp.pdc.proxy.header.Header;
import tp.pdc.proxy.parser.interfaces.HttpHeaderParser;
import static tp.pdc.proxy.parser.utils.AsciiConstants.*;
import tp.pdc.proxy.parser.utils.ParseUtils;

import java.nio.ByteBuffer;
import java.util.*;

import org.apache.commons.lang3.ArrayUtils;

public class HttpHeaderParserImpl implements HttpHeaderParser {

    private enum HttpHeaderState {
        ADD_HEADERS, LINE_START, ERROR, END_LINE_CR, SECTION_END_CR, END_OK,
        NAME, RELEVANT_COLON, RELEVANT_SPACE, RELEVANT_CONTENT,
        COLON, SPACE, CONTENT,
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
    private Iterator<Map.Entry<Header, byte[]>> headersToAddIterator;

    private Map.Entry<Header, byte[]> nextToAdd;

    private boolean ignoring;
    // Buffered bytes que todavía no se escribieron en outputBuffer
    private int buffered = 0;

    public HttpHeaderParserImpl(Map<Header, byte[]> toAdd, Set<Header> toRemove, Set<Header> toSave) {
        state = HttpHeaderState.ADD_HEADERS;
        headerName = ByteBuffer.allocate(HEADER_NAME_SIZE);
        headerValue = ByteBuffer.allocate(HEADER_CONTENT_SIZE);
        savedHeaders = new HashMap<>();

        this.headersToRemove = toRemove;
        this.headersToAdd = toAdd;
        this.headersToAddIterator = toAdd.entrySet().iterator();
        this.headersToSave = toSave;
    }

    private void expectByteAndOutput(byte read, byte expected, HttpHeaderState next, ByteBuffer outputBuffer)
        throws ParserFormatException {
        if (read == expected) {
            putIfNotIgnored(read, outputBuffer);
            state = next;
        } else {
            handleError();
        }
    }

    private void handleError() throws ParserFormatException {
        state = HttpHeaderState.ERROR;
        throw new ParserFormatException("Error while parsing header");
    }


    @Override 
    public boolean parse(ByteBuffer inputBuffer, ByteBuffer outputBuffer) throws ParserFormatException {
        while(inputBuffer.hasRemaining() && outputBuffer.remaining() > buffered)
            if (parse(inputBuffer.get(), outputBuffer))
                return true;

        if (outputBuffer.remaining() <= buffered)
        	outputBuffer.limit(outputBuffer.position()); // Así se simula que el buffer está lleno

        assertBufferCapacity(outputBuffer);

        return false;
    }

        private boolean parse(byte c, ByteBuffer output) throws ParserFormatException {
            switch (state) {
                case ADD_HEADERS:
                    addHeaders(output);
                    // Fallthrough

                case LINE_START:
                    ignoring = false;
                    if (c == CR.getValue()) {
                        state = HttpHeaderState.SECTION_END_CR;
                        output.put(c);
                    } else if (ParseUtils.isHeaderNameChar(c)) {
                        // Reset
                        headerName.clear();
                        headerValue.clear();
                        currentHeader = null;
                        saveHeaderNameByte((byte) Character.toLowerCase(c));
                        state = HttpHeaderState.NAME;
                    } else
                        handleError();
                    break;

                case NAME:
                    if (c == ':') {
                        handleHeaderName(c, output);
                    } else if (ParseUtils.isHeaderNameChar(c)) {
                        saveHeaderNameByte((byte) Character.toLowerCase(c));
                    } else {
                        handleError();
                    }
                    break;

                case RELEVANT_COLON:
                    expectByteAndOutput(c, SP.getValue(), HttpHeaderState.RELEVANT_SPACE, output);
                    break;

                case RELEVANT_SPACE:
                    if (ParseUtils.isHeaderContentChar(c)) {
                        saveHeaderContentbyte((byte) Character.toLowerCase(c));
                        state = HttpHeaderState.RELEVANT_CONTENT;
                    } else {
                        handleError();
                    }
                    break;

                case RELEVANT_CONTENT:
                    if (c == CR.getValue()) {
                        headerValue.flip();
                        byte[] headerAux = new byte[headerValue.remaining()];
                        headerValue.get(headerAux);
                        savedHeaders.put(currentHeader, headerAux);

                        state = HttpHeaderState.END_LINE_CR;
                        if (!ignoring)
                            output.put(headerAux).put(CR.getValue());
                    } else if (ParseUtils.isHeaderContentChar(c)) {
                        saveHeaderContentbyte((byte) Character.toLowerCase(c));
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
                        putIfNotIgnored(c, output);
                    } else {
                        handleError();
                    }
                    break;

                case CONTENT:
                    if (c == CR.getValue()) {
                        state = HttpHeaderState.END_LINE_CR;
                        putIfNotIgnored(c, output);
                    } else if (ParseUtils.isHeaderContentChar(c)) {
                        putIfNotIgnored(c, output);
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

                default:
                    throw new IllegalStateException();
            }

        buffered = headerName.position() + headerValue.position();
        return false;
    }

    private void saveHeaderNameByte(byte b) throws ParserFormatException {
        if (!headerName.hasRemaining())
            throw new ParserFormatException("Header name too long", HttpErrorCode.HEADER_FIELD_TOO_LARGE_431);
        headerName.put(b);
    }

    private void saveHeaderContentbyte(byte b) throws ParserFormatException {
        if (!headerValue.hasRemaining())
            throw new ParserFormatException("Header content too long", HttpErrorCode.HEADER_FIELD_TOO_LARGE_431);
        headerValue.put(b);
    }

    private void handleHeaderName(byte c, ByteBuffer outputBuffer) {
        headerName.flip();
        int nameLen = headerName.remaining();
        currentHeader = Header.getHeaderByBytes(headerName, nameLen); //TODO: podría ser más optimo, pero no es determinante

        if (headersToRemove.contains(currentHeader) || headersToAdd.containsKey(currentHeader) /* gets changed */)
            ignoring = true;

        if (!ignoring)
            outputBuffer.put(headerName).put(c);

        if (headersToSave.contains(currentHeader) || currentHeader == Header.HOST) {
            savedHeaders.put(currentHeader, ArrayUtils.EMPTY_BYTE_ARRAY);
            state = HttpHeaderState.RELEVANT_COLON;
        } else {
            state = HttpHeaderState.COLON;
        }
    }

    private void assertBufferCapacity(ByteBuffer buffer) {
        if (buffer.capacity() < buffered)
            throw new IllegalArgumentException("Output buffer too small");
    }

    private void addHeaders(ByteBuffer output) {
        // Si no lo pude meter antes quedó acá guardado
        if (nextToAdd != null && headerFitsBuffer(nextToAdd.getKey(), nextToAdd.getValue(), output))
            putHeaderInBuffer(nextToAdd.getKey(), nextToAdd.getValue(), output);

        while (headersToAddIterator.hasNext()) {
            nextToAdd = headersToAddIterator.next();

            Header header = nextToAdd.getKey();
            byte[] value = nextToAdd.getValue();

            if (!headerFitsBuffer(header, value, output)) {
                return;
            }

            putHeaderInBuffer(header, value, output);
            nextToAdd = null;
        }
    }

    private void putHeaderInBuffer(Header header, byte[] value, ByteBuffer buffer) {
        buffer
            .put(header.getBytes())
            .put((byte) ':')
            .put(SP.getValue())
            .put(value)
            .put(CR.getValue()).put(LF.getValue());
    }

    private boolean headerFitsBuffer(Header header, byte[] value, ByteBuffer buffer) {
        return buffer.remaining() >= header.getBytes().length + value.length + 4;
        // 4: colon + SP + CR + LF
    }

    private void putIfNotIgnored(byte c, ByteBuffer output) {
        if (!ignoring)
            output.put(c);
    }

    @Override 
    public boolean hasFinished() {
        return this.state == HttpHeaderState.END_OK;
    }

    @Override 
    public void reset () {
        headerName.clear();
        headerValue.clear();
        savedHeaders.clear();
        state = HttpHeaderState.ADD_HEADERS;
        buffered = 0;
        ignoring = false;

        headersToAddIterator = headersToAdd.entrySet().iterator();
        nextToAdd = null;
    }

    @Override 
    public byte[] getHeaderValue(Header header) {
        return this.savedHeaders.get(header);
    }

    @Override 
    public boolean hasHeaderValue(Header header) {
        return this.savedHeaders.containsKey(header);
    }
}
