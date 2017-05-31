package tp.pdc.proxy.header;

import java.nio.ByteBuffer;
import tp.pdc.proxy.ProxyProperties;

public enum Header {
	HOST("host"), 
	CONNECTION("connection"), 
	CONTENT_LENGTH("content-length"),
	TRANSFER_ENCODING("transfer-encoding"), 
	USER_AGENT("user-agent"),
	PROXY_CONNECTION("proxy-connection"), 
	CONTENT_TYPE("content-type"), 
	ACCEPT_ENCODING("accept-encoding"), 
	SERVER("server");

	private String headerName;
	private byte[] headerBytes;

	public static Header getHeaderByBytes (ByteBuffer bytes, int length) {
		for (Header header : values())
			if (BytesUtils.equalsBytes(header.headerBytes, bytes, length))
				return header;
		return null;
	}
	
	private Header(String header) {
		headerName = header;
		headerBytes = header.getBytes(ProxyProperties.getInstance().getCharset());
	}

	@Override
	public String toString() {
		return headerName;
	}

	public byte[] getBytes() { return headerBytes; }
}
