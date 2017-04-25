package tp.pdc.proxy;

import tp.pdc.proxy.header.Header;


public interface HeadersParser extends Parser {
    boolean hasHeaderValue(Header header);
    byte[] getHeaderValue(Header header);
    boolean hasMethod();
    RequestMethod getMethod();
}
