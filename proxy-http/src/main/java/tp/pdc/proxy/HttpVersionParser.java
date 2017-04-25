package tp.pdc.proxy;

import java.nio.ByteBuffer;

public interface HttpVersionParser {

    boolean hasError();

    boolean hasEndedOk ();

    boolean readMinorVersion();

    boolean readMajorVersion();

    int getMajorHttpVersion();

    int getMinorHttpVersion();

    boolean parse(ByteBuffer inputBuffer, ByteBuffer outputBuffer);

    boolean parse(byte b, ByteBuffer outputBuffer);
}
