package tp.pdc.proxy.parser.interfaces;

import tp.pdc.proxy.header.Method;

/**
 * Parses an HTTP response
 */
public interface HttpResponseParser extends HttpResponseLineParser, HttpHeaderParser {
	/**
	 * Checks if there are no more headers
	 * @return true if there are no more headers, false if not
     */
	boolean hasHeadersFinished ();

	/**
	 * Sets the method the client sent
	 * @param clientMethod client method to set
     */
	void setClientMethod (Method clientMethod);
}
