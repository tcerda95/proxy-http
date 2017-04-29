package tp.pdc.proxy.parser.factory;

import tp.pdc.proxy.exceptions.IllegalHttpHeadersException;
import tp.pdc.proxy.header.BytesUtils;
import tp.pdc.proxy.header.Header;
import tp.pdc.proxy.header.HeaderValue;
import tp.pdc.proxy.header.Method;
import tp.pdc.proxy.parser.HttpChunkedParser;
import tp.pdc.proxy.parser.HttpConnectionCloseParser;
import tp.pdc.proxy.parser.HttpContentLengthParser;
import tp.pdc.proxy.parser.HttpNullBodyParser;
import tp.pdc.proxy.parser.interfaces.HttpBodyParser;
import tp.pdc.proxy.parser.interfaces.HttpHeaderParser;
import tp.pdc.proxy.parser.utils.ParseUtils;

public class HttpBodyParserFactory {
	
	private static final HttpNullBodyParser NULL_PARSER = HttpNullBodyParser.getInstance();
	
	private HttpBodyParserFactory() {
	}
	
	public static HttpBodyParser getClientHttpBodyParser(HttpHeaderParser headersParser, Method method) throws IllegalHttpHeadersException {
		if (method == Method.POST)
			return buildClientBodyParser(headersParser);
		return NULL_PARSER;
	}

	private static HttpBodyParser buildClientBodyParser(HttpHeaderParser headersParser) throws IllegalHttpHeadersException {
		HttpBodyParser parser = buildBodyParser(headersParser);
		
		if (parser == null)
			throw new IllegalHttpHeadersException("Missing content-length and tranfser-encoding: chunked headers");
			
		return parser;
	}

	// TODO: recibir también el response code: los 1xx, 204 y 304 NO DEBEN incluir un message-body
	public static HttpBodyParser getServerHttpBodyParser(HttpHeaderParser headersParser, Method method) throws IllegalHttpHeadersException {
		if (method != Method.HEAD)
			return buildServerBodyParser(headersParser);
		return NULL_PARSER;
	}
	
	private static HttpBodyParser buildServerBodyParser(HttpHeaderParser headersParser) throws IllegalHttpHeadersException {
		HttpBodyParser parser = buildBodyParser(headersParser);

		if (parser == null) {
			if (shouldL33t(headersParser))
				// TODO: retornar connection close leet parser
				;
			else
				return HttpConnectionCloseParser.getInstance();
		}
		
		return parser;
	}
	
	// It is imperative to prioritize Chunked over Content-Length parsing: RFC 2616 4.4.3
	private static HttpBodyParser buildBodyParser(HttpHeaderParser headersParser) throws IllegalHttpHeadersException {
		try {
			if (shouldL33t(headersParser)) {
				if (hasChunked(headersParser))
					; // TODO: Retornar ChunkedLeetParser
				else if (hasContentLength(headersParser))
					; // TODO: Retornar ContentLeetParser
			}
			else {
				if (hasChunked(headersParser))
					return new HttpChunkedParser();
				
				else if (hasContentLength(headersParser))
					return new HttpContentLengthParser(ParseUtils.parseInt(headersParser.getHeaderValue(Header.CONTENT_LENGTH)));
			}
		} catch (NumberFormatException e) {
			throw new IllegalHttpHeadersException("Invalid content-length value: illegal number format");
		}
		
		return null;
	}

	private static boolean hasContentLength(HttpHeaderParser headersParser) {
		return headersParser.hasHeaderValue(Header.CONTENT_LENGTH);
	}
	
	private static boolean hasChunked(HttpHeaderParser headersParser) {
		return headersParser.hasHeaderValue(Header.TRANSFER_ENCODING) && 
				BytesUtils.equalsBytes(headersParser.getHeaderValue(Header.TRANSFER_ENCODING), HeaderValue.CHUNKED.getValue());
	}
	
	private static boolean shouldL33t(HttpHeaderParser headersParser) {
		// TODO Auto-generated method stub
		return false;
	}
	
}
