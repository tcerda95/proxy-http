package tp.pdc.proxy;

import java.nio.ByteBuffer;

public class HttpVersionParserImpl implements HttpVersionParser {

    private int minorVersion, majorVersion;

    private HttpVersionState state;

    private enum HttpVersionState {
        NOT_READ_YET, H, T_1, T_2, P, MAJOR_VERSION, MINOR_VERSION, READ_OK, ERROR,
    }

    public HttpVersionParserImpl () {
        majorVersion = minorVersion = -1;
        state = HttpVersionState.NOT_READ_YET;
    }

    @Override public boolean hasError () {
        return state == HttpVersionState.ERROR;
    }

    @Override public boolean hasEndedOk () {
        return state == HttpVersionState.READ_OK;
    }

    @Override public boolean readMinorVersion () {
        return minorVersion != -1;
    }

    @Override public boolean readMajorVersion () {
        return majorVersion != -1;
    }

    @Override public int getMajorHttpVersion () {
        if (!readMajorVersion())
            throw new IllegalStateException(); //TODO: unread
        return majorVersion;
    }

    @Override public int getMinorHttpVersion () {
        if (!readMinorVersion())
            throw new IllegalStateException(); //TODO: unread
        return minorVersion;
    }

    @Override public boolean parse (ByteBuffer inputBuffer, ByteBuffer outputBuffer) {
        while (inputBuffer.hasRemaining()) {
            if (parse(inputBuffer.get(), outputBuffer)) {
                return true;
            }
        }
        return false;
    }

    @Override public boolean parse (byte b, ByteBuffer outputBuffer) {
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
                if (ParseUtils.isDigit(b) && b != (byte) '0') {
                    majorVersion *= 10;
                    majorVersion += b - (byte) '0';
                    outputBuffer.put(b);
                } else if (b == (byte) '.' && majorVersion != 0) {
                    state = HttpVersionState.MINOR_VERSION;
                    outputBuffer.put(b);
                } else {
                    state = HttpVersionState.ERROR;
                }
                break;

            case MINOR_VERSION:
                if (ParseUtils.isDigit(b)) {
                    minorVersion *= 10;
                    minorVersion += b - (byte) '0';
                    outputBuffer.put(b);
                } else if (b == '\r') { //TODO: interfaz de constantes
                    state = HttpVersionState.READ_OK;
//                    httpParse = ParserState.CR_FIRST_LINE; TODO: el parser exterior pregunta si terminó para cambiar de estado
                    outputBuffer.put(b);
                } else {
                    state = HttpVersionState.ERROR;
                }
                break;

            case READ_OK:
                // No debería recibir nada más
                state = HttpVersionState.ERROR;
                break;
        }

        if (state == HttpVersionState.ERROR)
            throw new IllegalStateException();

        return state == HttpVersionState.READ_OK;
    }

    private void expectByteAndOutput(byte read, byte expected, ByteBuffer out, HttpVersionState next) {
        if (read == expected) {
            out.put(read);
            state = next;
        } else {
            state = HttpVersionState.ERROR;
        }
    }
}
