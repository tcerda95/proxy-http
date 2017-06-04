package tp.pdc.proxy.parser.interfaces;

import tp.pdc.proxy.header.Method;

public interface HttpResponseParser extends HttpResponseLineParser, HttpHeaderParser {
	boolean hasHeadersFinished();
	void setClientMethod(Method clientMethod);
}
