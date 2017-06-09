package tp.pdc.proxy.header.protocol;

import tp.pdc.proxy.bytes.BytesUtils;
import tp.pdc.proxy.properties.ProxyProperties;

import java.nio.ByteBuffer;

/**
 * Enum containing every header accepted by the protocol
 */
public enum CrazyProtocolHeader {
	PROXY_BUF_SIZE("proxy_buf_size"),
	SET_PROXY_BUF_SIZE("set_proxy_buf_size"),
	L33TENABLE("l33t_enable"),
	L33TDISABLE("l33t_disable"),
	ISL33TENABLE("is_l33t_enabled"),
	CLIENT_BYTES_READ("client_bytes_read"),
	CLIENT_BYTES_WRITTEN("client_bytes_written"),
	CLIENT_CONNECTIONS("client_connections"),
	SERVER_BYTES_READ("server_bytes_read"),
	SERVER_BYTES_WRITTEN("server_bytes_written"),
	SERVER_CONNECTIONS("server_connections"),
	METHOD_COUNT("method_count"),
	STATUS_CODE_COUNT("status_code_count"),
	METRICS("metrics"),
	PING("ping"),
	END("end");

	private final String headerName;
	private final byte[] headerBytes;

	private CrazyProtocolHeader (String header) {
		headerName = header;
		headerBytes = header.getBytes(ProxyProperties.getInstance().getCharset());
	}

	public static CrazyProtocolHeader getHeaderByBytes (ByteBuffer bytes, int length) {
		for (CrazyProtocolHeader header : values())
			if (BytesUtils.equalsBytes(header.headerBytes, bytes, length))
				return header;
		return null;
	}

	public static int maxHeaderLen () {
		int maxLength = 0;
		for (CrazyProtocolHeader h : CrazyProtocolHeader.values()) {
			int currentLength = h.toString().length();

			if (currentLength > maxLength)
				maxLength = currentLength;
		}
		return maxLength;
	}

	@Override
	public String toString () {
		return headerName;
	}

	public byte[] getBytes () {
		return headerBytes;
	}
}
