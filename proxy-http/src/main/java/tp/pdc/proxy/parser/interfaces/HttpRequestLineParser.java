package tp.pdc.proxy.parser.interfaces;

import tp.pdc.proxy.header.Method;

public interface HttpRequestLineParser extends HttpVersionParser {
    byte[] getWholeRequestLine ();
    boolean hasMethod();
    Method getMethod();
    boolean hasHost();
    byte[] getHostValue();
}
