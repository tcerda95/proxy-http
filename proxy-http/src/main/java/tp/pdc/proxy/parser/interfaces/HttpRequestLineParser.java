package tp.pdc.proxy.parser.interfaces;


import tp.pdc.proxy.header.Method;

public interface HttpRequestLineParser {

    boolean hasMethod(Method method);

    boolean hasHost();
}
