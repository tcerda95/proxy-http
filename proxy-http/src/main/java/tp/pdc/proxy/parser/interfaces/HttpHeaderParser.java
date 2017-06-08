package tp.pdc.proxy.parser.interfaces;

import tp.pdc.proxy.header.Header;

public interface HttpHeaderParser extends Parser, Reseteable {
	boolean hasHeaderValue (Header header);

	byte[] getHeaderValue (Header header);
}
