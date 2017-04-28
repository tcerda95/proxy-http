package tp.pdc.proxy.parser.interfaces;

import tp.pdc.proxy.header.Header;
import tp.pdc.proxy.header.Method;

public interface HttpRequestParser extends Parser, HttpRequestLineParser {
    boolean hasHeaderValue(Header header);

    byte[] getHeaderValue(Header header);
}
