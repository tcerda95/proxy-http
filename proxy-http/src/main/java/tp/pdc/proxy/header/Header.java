package tp.pdc.proxy.header;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import tp.pdc.proxy.ProxyProperties;

public enum Header {
	HOST("host"),
	CONNECTION("connection");
	
	private static final Map<byte[], Header> BYTES_MAP = new HashMap<>();

	static {
		for (Header header : values())
			BYTES_MAP.put(header.bytes, header);

	}
	
	private final byte[] bytes;
	
	public static boolean isRelevantHeader(byte[] bytes) {
		return BYTES_MAP.containsKey(bytes);
	}

	public static Header getHeaderFromBytes(byte[] bytes) {
		return BYTES_MAP.get(bytes);
	}
	
	private Header(String header) {
		bytes = header.getBytes(ProxyProperties.getInstance().getCharset());
	}
	
	public byte[] getBytes() {
		return bytes;
	}
}
