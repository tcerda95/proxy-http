package tp.pdc.proxy.parser.interfaces;

public interface HttpRequestParser extends HttpHeaderParser, HttpRequestLineParser {
	public boolean hasRequestLineFinished ();

	public boolean hasHeadersFinished ();
}
