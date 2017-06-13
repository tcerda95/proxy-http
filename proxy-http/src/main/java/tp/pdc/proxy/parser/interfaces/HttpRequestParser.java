package tp.pdc.proxy.parser.interfaces;

/**
 * Parses an HTTP request
 */
public interface HttpRequestParser extends HttpHeaderParser, HttpRequestLineParser {
	/**
	 * Checks is the first line of the request has finished
	 * @return true if it has finished, false if not
     */
	public boolean hasRequestLineFinished ();

	/**
	 * Checks if the headers have finished
	 * @return true if there are no more headers, false if not
     */
	public boolean hasHeadersFinished ();
}
