package tp.pdc.proxy.parser.interfaces;

import tp.pdc.proxy.exceptions.ParserFormatException;
import tp.pdc.proxy.header.Header;

import java.nio.ByteBuffer;

public interface HttpHeaderParser extends Parser, Reseteable {
    boolean hasHeaderValue(Header header);
    byte[] getHeaderValue(Header header);
}
