package tp.pdc.proxy.parser.factory;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

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
		toRemove = Collections.emptySet(); // TODO Deberia ser proxy_connection

		toAdd = new EnumMap<>(Header.class);
		toAdd.put(Header.CONNECTION, HeaderValue.CLOSE.getValue());

		toSave = EnumSet.of(Header.PROXY_CONNECTION, Header.CONTENT_LENGTH, Header.TRANSFER_ENCODING, Header.CONTENT_TYPE);
	}
	
	public static HttpRequestParserFactory getInstance() {
		return INSTANCE;
	}
	
	public HttpRequestParser getRequestParser() {
		return new HttpRequestParserImpl(toAdd, toRemove, toSave);
	}
}
