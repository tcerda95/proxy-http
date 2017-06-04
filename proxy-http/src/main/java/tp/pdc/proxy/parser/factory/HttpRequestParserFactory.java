package tp.pdc.proxy.parser.factory;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import tp.pdc.proxy.L33tFlag;
import tp.pdc.proxy.header.Header;
import tp.pdc.proxy.header.HeaderValue;
import tp.pdc.proxy.parser.interfaces.HttpRequestParser;
import tp.pdc.proxy.parser.main.HttpRequestParserImpl;

public class HttpRequestParserFactory {
	private static final HttpRequestParserFactory INSTANCE = new HttpRequestParserFactory();
	private static final L33tFlag L33T_FLAG = L33tFlag.getInstance();
	
	private final Set<Header> toRemove;
	private final Map<Header, byte[]> toAdd;
    private final Set<Header> toSave;

    private final Set<Header> toRemoveL33t;
    
	private HttpRequestParserFactory() {
		toRemove = EnumSet.of(Header.PROXY_CONNECTION);
		
		toAdd = new EnumMap<>(Header.class);
		toAdd.put(Header.CONNECTION, HeaderValue.KEEP_ALIVE.getValue());
		
		toSave = EnumSet.of(Header.CONNECTION, Header.PROXY_CONNECTION, Header.CONTENT_LENGTH, Header.TRANSFER_ENCODING, 
				Header.CONTENT_TYPE, Header.USER_AGENT);
		
		toRemoveL33t = EnumSet.copyOf((EnumSet<Header>) toRemove);
		toRemoveL33t.add(Header.ACCEPT_ENCODING);
	}
	
	public static HttpRequestParserFactory getInstance() {
		return INSTANCE;
	}
	
	public HttpRequestParser getRequestParser() {
		return new HttpRequestParserImpl(toAdd, L33T_FLAG.isSet() ? toRemoveL33t : toRemove, toSave);
	}
}
