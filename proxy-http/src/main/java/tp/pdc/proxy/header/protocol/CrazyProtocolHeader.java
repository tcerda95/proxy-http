package tp.pdc.proxy.header.protocol;

import java.nio.ByteBuffer;
import tp.pdc.proxy.ProxyProperties;
import tp.pdc.proxy.header.BytesUtils;

public enum CrazyProtocolHeader {
	L33TENABLE("l33tenable"), 
	CLIENT_BYTES_READ("client_bytes_read"),
	CLIENT_BYTES_WRITE("client_bytes_write"), 
	CLIENT_BYTES_TRANSFERRED("client_bytes_transferred"),
	CLIENT_CONNECTIONS("client-connections"),
	SERVER_BYTES_READ("server_bytes_read"),
	SERVER_BYTES_WRITE("server_bytes_write"), 
	SERVER_BYTES_TRANSFERRED("server_bytes_transferred"),
	SERVER_CONNECTIONS("server_connections"),
	METHOD_COUNT("method_count"),
	STATUS_CODE_COUNT("status_code_count"),
	METRICS("metrics"),
	END("end");

	private String headerName;
	private byte[] headerBytes;

	public static CrazyProtocolHeader getHeaderByBytes (ByteBuffer bytes, int length) {
		for (CrazyProtocolHeader header : values())
			if (BytesUtils.equalsBytes(header.headerBytes, bytes, length))
				return header;
		return null;
	}
	
	CrazyProtocolHeader(String header) {
		headerName = header;
		headerBytes = header.getBytes(ProxyProperties.getInstance().getCharset());
	}

	@Override
	public String toString() {
		return headerName;
	}

	public byte[] getBytes() { return headerBytes; }
}
