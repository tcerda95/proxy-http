package tp.pdc.proxy.parser.factory;

import java.util.*;

import tp.pdc.proxy.header.Header;
import tp.pdc.proxy.header.HeaderValue;
import tp.pdc.proxy.parser.interfaces.HttpRequestParser;
import tp.pdc.proxy.parser.main.HttpRequestParserImpl;

public class HttpRequestParserFactory {
	private static final HttpRequestParserFactory INSTANCE = new HttpRequestParserFactory();
	
	private final Set<Header> toRemove;
	private final Map<Header, byte[]> toAdd;
    private final Set<Header> toSave;

	private HttpRequestParserFactory() {
		toRemove = EnumSet.of(Header.PROXY_CONNECTION);

		toAdd = new HashMap<>();
		toAdd.put(Header.CONNECTION, HeaderValue.CLOSE.getValue());

		toSave = EnumSet.of(Header.CONNECTION, Header.CONTENT_LENGTH, Header.TRANSFER_ENCODING);
	}
	
	public static HttpRequestParserFactory getInstance() {
		return INSTANCE;
	}
	
	public HttpRequestParser getRequestParser() {
		return new HttpRequestParserImpl(toAdd, toRemove, toSave);
	}
}
