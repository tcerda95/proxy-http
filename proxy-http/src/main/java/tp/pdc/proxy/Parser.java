package tp.pdc.proxy;

import java.nio.ByteBuffer;

public interface Parser {
    void parse(ByteBuffer input, ByteBuffer output);
    boolean hasFinished();
}
