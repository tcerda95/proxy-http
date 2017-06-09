package tp.pdc.proxy.header;

import tp.pdc.proxy.ProxyProperties;

public enum HeaderValue {
	CHUNKED("chunked"),
	CLOSE("close"),
	TEXT_PLAIN("text/plain"),
	KEEP_ALIVE("keep-alive"),
	IDENTITY("identity");

	private byte[] value;

	private HeaderValue (String str) {
		this.value = str.getBytes(ProxyProperties.getInstance().getCharset());
	}

	public byte[] getValue () {
		return value;
	}
}
