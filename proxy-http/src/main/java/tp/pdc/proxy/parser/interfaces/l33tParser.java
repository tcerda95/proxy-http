package tp.pdc.proxy.parser.interfaces;

import java.nio.ByteBuffer;

public interface l33tParser {

    void decode(ByteBuffer inputBuffer, ByteBuffer outputBuffer);

    void encode(ByteBuffer inputBuffer, ByteBuffer outputBuffer);
}
