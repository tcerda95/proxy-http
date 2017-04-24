package tp.pdc.proxy.header;

import java.util.HashSet;
import java.util.Set;

import tp.pdc.proxy.ProxyProperties;

public enum Header {
	HOST("Host"),
	CONNECTION("Connection");
	
	private static final Set<byte[]> BYTES_SET = new HashSet<>();
	
	static {
		for (Header header : values())
			BYTES_SET.add(header.bytes);
	}
	
	private final byte[] bytes;
	
	public static boolean isRelevantHeader(byte[] bytes) {
		return BYTES_SET.contains(bytes);
	}
	
	private Header(String header) {
		bytes = header.getBytes(ProxyProperties.getInstance().getCharset());
	}
	
	public byte[] getBytes() {
		return bytes;
	}
}
