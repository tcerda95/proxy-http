package tp.pdc.proxy.header;

import java.nio.ByteBuffer;

import tp.pdc.proxy.ProxyProperties;

public enum StatusCode {
	OK("200"),
	WHICH_ONES("???");

	private String statusCode;
	private byte[] statusCodeBytes;

	public static StatusCode getStatusCodeByBytes (ByteBuffer bytes, int length) {
		for (StatusCode statusCode : values())
			if (BytesUtils.equalsBytes(statusCode.statusCodeBytes, bytes, length))
				return statusCode;
		return null;
	}
	
	StatusCode(String statusCode) {
		this.statusCode = statusCode;
		statusCodeBytes = statusCode.getBytes(ProxyProperties.getInstance().getCharset());
	}

	@Override
	public String toString() {
		return statusCode;
	}

	public byte[] getBytes() { return statusCodeBytes; }
}

