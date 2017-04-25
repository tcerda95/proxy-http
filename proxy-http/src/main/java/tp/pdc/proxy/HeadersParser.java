package tp.pdc.proxy;

import tp.pdc.proxy.header.Header;
import tp.pdc.proxy.header.Method;


public interface HeadersParser extends Parser {
    boolean hasHeaderValue(Header header);
    byte[] getHeaderValue(Header header);
    boolean hasMethod(Method method);
}
