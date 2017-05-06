package tp.pdc.proxy.parser.factory;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import tp.pdc.proxy.header.Header;
import tp.pdc.proxy.parser.interfaces.HttpResponseParser;
import tp.pdc.proxy.parser.mainParsers.HttpResponseParserImpl;

public class HttpResponseParserFactory {
	private static final HttpResponseParserFactory INSTANCE = new HttpResponseParserFactory();
	
	private final Set<Header> toRemove;
	private final Map<Header, byte[]> toAdd;
    private final Set<Header> toSave;

	private HttpResponseParserFactory() {
		toRemove = Collections.emptySet();
		toAdd = Collections.emptyMap();
		toSave = new HashSet<>();
		
        toSave.add(Header.HOST);
        toSave.add(Header.CONTENT_LENGTH);
        toSave.add(Header.TRANSFER_ENCODING);
	}
	
	public static HttpResponseParserFactory getInstance() {
		return INSTANCE;
	}
	
	public HttpResponseParser getResponseParser() {
		return new HttpResponseParserImpl(toAdd, toRemove, toSave);
	}
}
