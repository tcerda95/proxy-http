package tp.pdc.proxy.parser.interfaces;

import tp.pdc.proxy.header.Method;

/**
 * Parses the first line of a request
 */
public interface HttpRequestLineParser extends HttpVersionParser {
	/**
	 * Gets the first line of the request
	 * @return the first line of the request
     */
	byte[] getWholeRequestLine ();

	/**
	 * Checks if the request has a valid method
	 * @return true if has a valid method, false if not
     */
	boolean hasMethod ();

	/**
	 * Gets the request method
	 * @return the request method
     */
	Method getMethod ();

	/**
	 * Checks if the request has host in te first line
	 * @return true if the request has host, false if not
     */
	boolean hasHost ();

	/**
	 * Gets the host value
	 * @return bytes of the host value
     */
	byte[] getHostValue ();
}
