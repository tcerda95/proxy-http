package tp.pdc.proxy.parser.factory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import tp.pdc.proxy.header.Header;
import tp.pdc.proxy.header.HeaderValue;
import tp.pdc.proxy.parser.interfaces.HttpRequestParser;
import tp.pdc.proxy.parser.mainParsers.HttpRequestParserImpl;

public class HttpRequestParserFactory {
	private static final HttpRequestParserFactory INSTANCE = new HttpRequestParserFactory();
	
	private final Set<Header> toRemove;
	private final Map<Header, byte[]> toAdd;
    private final Set<Header> toSave;

	private HttpRequestParserFactory() {
		toRemove = new HashSet<>();
		toAdd = new HashMap<>();
		toSave = new HashSet<>();
		
		toRemove.add(Header.PROXY_CONNECTION);
        toAdd.put(Header.CONNECTION, HeaderValue.CLOSE.getValue());
        toSave.add(Header.HOST);
        toSave.add(Header.CONTENT_LENGTH);
        toSave.add(Header.TRANSFER_ENCODING);
	}
	
	public static HttpRequestParserFactory getInstance() {
		return INSTANCE;
	}
	
	public HttpRequestParser getRequestParser() {
		return new HttpRequestParserImpl(toAdd, toRemove, toSave);
	}
}
