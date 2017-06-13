package tp.pdc.proxy.parser.factory;

import tp.pdc.proxy.header.Header;
import tp.pdc.proxy.header.HeaderValue;
import tp.pdc.proxy.header.Method;
import tp.pdc.proxy.parser.interfaces.HttpResponseParser;
import tp.pdc.proxy.parser.main.HttpResponseParserImpl;

import java.util.*;

/**
 * In charge of the creation of the {@link HttpResponseParser}
 */
public class HttpResponseParserFactory {
	private static final HttpResponseParserFactory INSTANCE = new HttpResponseParserFactory();

	private final Set<Header> toRemove;
	private final Map<Header, byte[]> toAdd;
	private final Set<Header> toSave;

	private HttpResponseParserFactory () {
		toRemove = Collections.emptySet();

		toAdd = new EnumMap<>(Header.class);
		toAdd.put(Header.CONNECTION, HeaderValue.KEEP_ALIVE.getValue());

		toSave = EnumSet.of(Header.CONNECTION, Header.CONTENT_LENGTH, Header.TRANSFER_ENCODING,
			Header.CONTENT_TYPE, Header.SERVER, Header.CONTENT_ENCODING);
	}

	public static HttpResponseParserFactory getInstance () {
		return INSTANCE;
	}

	public HttpResponseParser getResponseParser (Method clientMethod) {
		return new HttpResponseParserImpl(toAdd, toRemove, toSave, clientMethod);
	}
}
