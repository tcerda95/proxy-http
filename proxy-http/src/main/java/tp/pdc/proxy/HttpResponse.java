package tp.pdc.proxy;

import java.nio.charset.Charset;

public enum HttpResponse {	
	BAD_REQUEST_400("400 Bad Request", "Request syntax errors\r\n"),
	METHOD_NOT_ALLOWED_405("405 Method not allowed", "Method not allowed"),
	BAD_GATEWAY_502("502 Bad Gateway", "Failed to connect to server\r\n");
	
	private final byte[] response;
	
	private HttpResponse(String errorCode, String body) {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("HTTP/1.1 ")
					 .append(errorCode)
					 .append("\r\n")
					 .append("Content-type: text/plain\r\n")
					 .append("Connection: close\r\n")
					 .append("Content-length: ")
					 .append(body.length())
					 .append("\r\n\r\n")
					 .append(body);
		
		Charset charset = ProxyProperties.getInstance().getCharset();		
		this.response = stringBuilder.toString().getBytes(charset);
	}
	
	public byte[] getBytes() {
		return response;
	}
}
