package tp.pdc.proxy.header;

import java.nio.ByteBuffer;
import tp.pdc.proxy.ProxyProperties;

public enum Header {
	HOST("host"), CONNECTION("connection"), CONTENT_LENGTH("content-length"),
	TRANSFER_ENCODING("transfer-encoding");

	private String headerName;
	private byte[] headerBytes;

	public static boolean isRelevantHeader(ByteBuffer bytes, int length) {
		return getByBytes(bytes, length) != null;
	}

	public static Header getByBytes(ByteBuffer bytes, int length) {
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
	
}
