package tp.pdc.proxy.parser.component;

import tp.pdc.proxy.exceptions.ParserFormatException;
import tp.pdc.proxy.parser.interfaces.HttpVersionParser;
import tp.pdc.proxy.parser.utils.AsciiConstants;
import tp.pdc.proxy.parser.utils.ParseUtils;

import java.nio.ByteBuffer;
import java.util.NoSuchElementException;

public class HttpVersionParserImpl implements HttpVersionParser {

    private int minorVersion, majorVersion;
    private boolean readMinorVersion, readMajorVersion;
    private byte endByte;
    private HttpVersionState state;
    private ByteBuffer wholeVersionBuffer;

    private enum HttpVersionState {
        NOT_READ_YET, H, T_1, T_2, P, MAJOR_VERSION, MINOR_VERSION, READ_OK, ERROR,
    }

    public HttpVersionParserImpl (byte endByte) {
        majorVersion = minorVersion = 0;
        state = HttpVersionState.NOT_READ_YET;
        wholeVersionBuffer = ByteBuffer.allocate(16);

        if (endByte < 0)
            throw new IllegalArgumentException("Not a valid us-ascii character");
        this.endByte = endByte;
    }

    @Override public boolean hasFinished() {
        return state == HttpVersionState.READ_OK;
    }

    @Override public void reset () {
        majorVersion = minorVersion = 0;
        state = HttpVersionState.NOT_READ_YET;
        wholeVersionBuffer.clear();
    }

    @Override public boolean readMinorVersion () {
        return readMinorVersion;
    }

    @Override public boolean readMajorVersion () {
        return readMajorVersion;
    }

    @Override public int getMajorHttpVersion () {
        if (!readMajorVersion())
            throw new NoSuchElementException("Major Http Version not read");
        return majorVersion;
    }

    @Override public int getMinorHttpVersion () {
        if (!readMinorVersion())
            throw new NoSuchElementException("Minor Http Version not read");
        return minorVersion;
    }

    @Override public byte[] getWholeVersionBytes () {
        if (!hasFinished())
            throw new IllegalStateException("Version not read yet");

        wholeVersionBuffer.flip();
        byte[] wholeBytes = new byte[wholeVersionBuffer.remaining()];
        wholeVersionBuffer.get(wholeBytes);
        return wholeBytes;
    }

    @Override public boolean parse (ByteBuffer inputBuffer, ByteBuffer outputBuffer)
        throws ParserFormatException {
        while (inputBuffer.hasRemaining() && outputBuffer.hasRemaining()) {
            if (parse(inputBuffer.get(), outputBuffer)) {
                return true;
            }
        }
        return false;
    }

    public boolean parse (byte b, ByteBuffer outputBuffer) throws ParserFormatException {
        if (!wholeVersionBuffer.hasRemaining())
            throw new ParserFormatException("Http Version Section Too Long");
        
        if (b != AsciiConstants.CR.getValue())
        	wholeVersionBuffer.put(b);

        switch (state) {
            case NOT_READ_YET:
                expectByteAndOutput(b, (byte) 'H', outputBuffer, HttpVersionState.H);
                break;

            case H:
                expectByteAndOutput(b, (byte) 'T', outputBuffer, HttpVersionState.T_1);
                break;

            case T_1:
                expectByteAndOutput(b, (byte) 'T', outputBuffer, HttpVersionState.T_2);
                break;

            case T_2:
                expectByteAndOutput(b, (byte) 'P', outputBuffer, HttpVersionState.P);
                break;

            case P:
                expectByteAndOutput(b, (byte) '/', outputBuffer, HttpVersionState.MAJOR_VERSION);
                break;

            case MAJOR_VERSION:
                if (ParseUtils.isDigit(b)) {
                    if (readMajorVersion || b != (byte) '0') { // No es cero a la izquierda
                        readMajorVersion = true;
                        majorVersion *= 10;
                        majorVersion += (b - (byte) '0');
                        outputBuffer.put(b);
                    }
                } else if (b == (byte) '.' && majorVersion != 0) {
                    state = HttpVersionState.MINOR_VERSION;
                    outputBuffer.put(b);
                } else {
                    handleError();
                }
                break;

            case MINOR_VERSION:
                if (ParseUtils.isDigit(b)) {
                    if (readMinorVersion || b != (byte) '0') { // No es cero a la izquierda
                        readMinorVersion = true;
                        minorVersion *= 10;
                        minorVersion += (b - (byte) '0');
                        outputBuffer.put(b);
                    }
                } else if (b == endByte) {
                    state = HttpVersionState.READ_OK;
                    outputBuffer.put(b);
                } else {
                    handleError();
                }
                break;

            default:
                handleError(); // No debería recibir nada más
                break;
        }

        if (state == HttpVersionState.ERROR)
            throw new IllegalStateException();

        return state == HttpVersionState.READ_OK;
    }

    private void handleError() throws ParserFormatException {
        state = HttpVersionState.ERROR;
        throw new ParserFormatException("Error while parsing version");
    }

    private void expectByteAndOutput(byte read, byte expected, ByteBuffer out, HttpVersionState next)
        throws ParserFormatException {
        if (read == expected) {
            out.put(read);
            state = next;
        } else {
            handleError();
        }
    }
}
