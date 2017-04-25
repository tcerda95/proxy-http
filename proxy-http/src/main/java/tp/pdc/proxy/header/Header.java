package tp.pdc.proxy.header;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import tp.pdc.proxy.ProxyProperties;

public enum Header {
	HOST("host"), CONNECTION("connection"), CONTENT_LENGTH("content-length"),
	TRANSFER_ENCODING("transfer-encoding");
	
	private static final Set<String> RELEVANT_HEADERS = new HashSet<>();

	static {
		for (Header header : values())
			RELEVANT_HEADERS.add(header.getHeaderName());

	}
	
	private String headerName;

	private String getHeaderName() {
		return this.headerName;
	}


	public static boolean isRelevantHeader(String h) {
		return RELEVANT_HEADERS.contains(h);
	}

	private Header(String header) {
		headerName = header;
	}

	public static Header getByName(String name) {
		for (Header header : values())
			if (name.equals(header.headerName))
				return header;
		return null;
	}

}
