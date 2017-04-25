package tp.pdc.proxy.header;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import tp.pdc.proxy.ProxyProperties;

public enum Header {
	HOST("host"), CONNECTION("connection"), CONTENT_LENGTH("content-length"),
	TRANSFER_ENCODING("transfer-encoding");

	private static final Map<byte[], Header> RELEVANT_HEADERS = new HashMap<>();

	static {
		for (Header header : values())
			RELEVANT_HEADERS.put(header.headerName.getBytes(), header);
	}
	
	private String headerName;

	public static boolean isRelevantHeader(ByteBuffer bytes, int length) {
		return getByBytes(bytes, length) != null;
	}

	Header(String header) {
		headerName = header;
	}

	public static Header getByBytes(ByteBuffer bytes, int length) {
		boolean equals;

		for (Map.Entry<byte[], Header> e: RELEVANT_HEADERS.entrySet()) {
			equals = true;
			if (length != e.getKey().length)
				continue;

			bytes.mark();
			for (int i = 0; i < length && equals; i++) {
				if (bytes.get() != e.getKey()[i]) {
					equals = false;
				}
			}
			bytes.reset();

			if (equals)
				return e.getValue();
		}

		return null;
	}
}
