package tp.pdc.proxy.parser.interfaces;


import tp.pdc.proxy.header.Method;

public interface HttpRequestLineParser extends Parser {

    byte[] getHostValue();

    boolean hasMethod(Method method);

    boolean hasHost();
}
