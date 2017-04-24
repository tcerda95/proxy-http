package tp.pdc.proxy;

import java.nio.charset.Charset;

public enum HttpResponse {
	BAD_REQUEST_400("HTTP/1.1 400 Bad Request\n"),
	BAD_GATEWAY_502("HTTP/1.1 502 Bad Gateway\n");
	
	private byte[] response;
	
	private HttpResponse(String response) {
		Charset charset = ProxyProperties.getInstance().getCharset();
		this.response = response.getBytes(charset);
	}
	
	public byte[] getBytes() {
		return response;
	}
}
