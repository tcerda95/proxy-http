package tp.pdc.proxy.parser.interfaces;

import tp.pdc.proxy.header.Method;

public interface HttpRequestLineParser extends Parser {
    boolean hasMethod(Method method);
    boolean hasHost();
    byte[] getHostValue();
    Method getMethod();
}
