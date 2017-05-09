package tp.pdc.proxy.parser.factory;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import tp.pdc.proxy.header.Header;
import tp.pdc.proxy.parser.interfaces.HttpResponseParser;
import tp.pdc.proxy.parser.main.HttpResponseParserImpl;

public class HttpResponseParserFactory {
	private static final HttpResponseParserFactory INSTANCE = new HttpResponseParserFactory();
	
	private final Set<Header> toRemove;
	private final Map<Header, byte[]> toAdd;
    private final Set<Header> toSave;

	private HttpResponseParserFactory() {
		toRemove = Collections.emptySet();
		toAdd = Collections.emptyMap();
		toSave = EnumSet.of(Header.HOST, Header.CONTENT_LENGTH, Header.TRANSFER_ENCODING, Header.CONTENT_TYPE);
	}
	
	public static HttpResponseParserFactory getInstance() {
		return INSTANCE;
	}
	
	public HttpResponseParser getResponseParser() {
		return new HttpResponseParserImpl(toAdd, toRemove, toSave);
	}
}
