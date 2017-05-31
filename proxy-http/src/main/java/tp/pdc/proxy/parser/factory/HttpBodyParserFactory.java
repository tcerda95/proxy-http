package tp.pdc.proxy.parser.factory;

import java.util.List;

import tp.pdc.proxy.L33tFlag;
import tp.pdc.proxy.ProxyProperties;
import tp.pdc.proxy.exceptions.IllegalHttpHeadersException;
import tp.pdc.proxy.header.BytesUtils;
import tp.pdc.proxy.header.Header;
import tp.pdc.proxy.header.HeaderValue;
import tp.pdc.proxy.header.Method;
import tp.pdc.proxy.parser.CharsetParser;
import tp.pdc.proxy.parser.body.HttpChunkedParser;
import tp.pdc.proxy.parser.body.HttpConnectionCloseL33tParser;
import tp.pdc.proxy.parser.body.HttpConnectionCloseParser;
import tp.pdc.proxy.parser.body.HttpContentLengthLeetParser;
import tp.pdc.proxy.parser.body.HttpContentLengthParser;
import tp.pdc.proxy.parser.body.HttpNullBodyParser;
import tp.pdc.proxy.parser.interfaces.HttpBodyParser;
import tp.pdc.proxy.parser.interfaces.HttpHeaderParser;
import tp.pdc.proxy.parser.interfaces.HttpRequestParser;
import tp.pdc.proxy.parser.interfaces.HttpResponseParser;
import tp.pdc.proxy.parser.utils.ParseUtils;

public class HttpBodyParserFactory {
	
	private static final ProxyProperties PROPERTIES = ProxyProperties.getInstance();
	private static final HttpBodyParserFactory INSTANCE = new HttpBodyParserFactory();
	
	private final HttpNullBodyParser nullParser = HttpNullBodyParser.getInstance();
	private final L33tFlag l33tFlag = L33tFlag.getInstance();
	private final CharsetParser charsetParser = new CharsetParser();
	private final List<byte[]> acceptedCharsets = PROPERTIES.getAcceptedCharsets();
	
	private HttpBodyParserFactory() {
	}
	
	public static HttpBodyParserFactory getInstance() {
		return INSTANCE;
	}
	
	public HttpBodyParser getClientHttpBodyParser(HttpRequestParser headersParser) throws IllegalHttpHeadersException {
		if (headersParser.hasMethod() && headersParser.getMethod() == Method.POST)
			return buildClientBodyParser(headersParser);
		return nullParser;
	}

	private HttpBodyParser buildClientBodyParser(HttpHeaderParser headersParser) throws IllegalHttpHeadersException {
		HttpBodyParser parser = buildBodyParser(headersParser);
		
		if (parser == null)
			throw new IllegalHttpHeadersException("Missing content-length and tranfser-encoding: chunked headers");
			
		return parser;
	}
	
	public HttpBodyParser getServerHttpBodyParser(HttpResponseParser headersParser, Method method) throws IllegalHttpHeadersException {
		if (method != Method.HEAD && isBodyStatusCode(headersParser.getStatusCode()))
			return buildServerBodyParser(headersParser);
		return nullParser;
	}
	
	// No body responses: 1xx, 204 or 304
	private boolean isBodyStatusCode(int statusCode) {
		return statusCode / 100 != 1 && statusCode != 204 && statusCode != 304;
	}

	private HttpBodyParser buildServerBodyParser(HttpHeaderParser headersParser) throws IllegalHttpHeadersException {
		HttpBodyParser parser = buildBodyParser(headersParser);

		if (parser == null) {
			if (shouldL33t(headersParser))
				return HttpConnectionCloseL33tParser.getInstance();
			else
				return HttpConnectionCloseParser.getInstance();
		}
		
		return parser;
	}
	
	// It is imperative to prioritize Chunked over Content-Length parsing: RFC 2616 4.4.3
	private HttpBodyParser buildBodyParser(HttpHeaderParser headersParser) throws IllegalHttpHeadersException {
		try {
			if (shouldL33t(headersParser)) {
				if (hasChunked(headersParser))
					return new HttpChunkedParser(true);
				else if (hasContentLength(headersParser))
					return new HttpContentLengthLeetParser(ParseUtils.parseInt(headersParser.getHeaderValue(Header.CONTENT_LENGTH)));
			}
			else {
				if (hasChunked(headersParser))
					return new HttpChunkedParser(false);
				
				else if (hasContentLength(headersParser))
					return new HttpContentLengthParser(ParseUtils.parseInt(headersParser.getHeaderValue(Header.CONTENT_LENGTH)));
			}
		} catch (NumberFormatException e) {
			throw new IllegalHttpHeadersException("Invalid content-length value: illegal number format");
		}
		
		return null;
	}

	private boolean hasContentLength(HttpHeaderParser headersParser) {
		return headersParser.hasHeaderValue(Header.CONTENT_LENGTH);
	}
	
	private boolean hasChunked(HttpHeaderParser headersParser) {
		return headersParser.hasHeaderValue(Header.TRANSFER_ENCODING) && 
				BytesUtils.equalsBytes(headersParser.getHeaderValue(Header.TRANSFER_ENCODING), HeaderValue.CHUNKED.getValue());
	}
	
	private boolean shouldL33t(HttpHeaderParser headersParser) {
		final byte[] textPlain = HeaderValue.TEXT_PLAIN.getValue();
		
		if (l33tFlag.isSet() && headersParser.hasHeaderValue(Header.CONTENT_TYPE)) {
			final byte[] contentTypeValue = headersParser.getHeaderValue(Header.CONTENT_TYPE);
			
			return BytesUtils.equalsBytes(contentTypeValue, textPlain, textPlain.length) && 
					isAcceptedCharset(charsetParser.extractCharset(contentTypeValue));
		}
		
		return false;
	}

	private boolean isAcceptedCharset(byte[] charset) {
		for (byte[] accepted : acceptedCharsets)
			if (BytesUtils.equalsBytes(charset, accepted, accepted.length))
				return true;
		return false;
	}
	
}
