package tp.pdc.proxy.parser.interfaces;

import tp.pdc.proxy.header.Header;

/**
 * Parses the headers
 */
public interface HttpHeaderParser extends Parser, Reseteable {
	/**
	 * Checks if a request or response has an specific header value
	 * @param header header to check
	 * @return true if it has the header, false if not
     */
	boolean hasHeaderValue (Header header);

	/**
	 * Gets an specific header value
	 * @param header header to get the value
	 * @return header value
     */
	byte[] getHeaderValue (Header header);
}
