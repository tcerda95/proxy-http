package tp.pdc.proxy;

import tp.pdc.proxy.header.Header;

import java.nio.ByteBuffer;


public interface HeadersParser {

    boolean hasHeaderValue(Header header);

    byte[] getHeaderValue(Header header);

    void parse(ByteBuffer input, ByteBuffer output);

    boolean hasFinished();
}
