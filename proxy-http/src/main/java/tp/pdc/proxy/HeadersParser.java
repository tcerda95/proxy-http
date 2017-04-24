package tp.pdc.proxy;

import java.nio.ByteBuffer;


public interface HeadersParser {

    boolean hasHeaderValue();

    byte[] getHeaderValue();

    void parse(ByteBuffer input, ByteBuffer output);

    boolean hasFinished();


}
