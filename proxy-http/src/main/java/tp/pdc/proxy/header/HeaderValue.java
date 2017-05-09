package tp.pdc.proxy.header;

import tp.pdc.proxy.ProxyProperties;

public enum HeaderValue {
	CHUNKED("chunked"),
	CLOSE("close");
	
	private byte[] value;
	
	private HeaderValue(String str) {
		this.value = str.getBytes(ProxyProperties.getInstance().getCharset());
	}
	
	public byte[] getValue() {
		return value;
	}
}
