package tp.pdc.proxy.parser.factory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import tp.pdc.proxy.L33tFlag;
import tp.pdc.proxy.exceptions.ParserFormatException;
import tp.pdc.proxy.header.Header;
import tp.pdc.proxy.header.HeaderValue;
import tp.pdc.proxy.header.Method;
import tp.pdc.proxy.parser.body.HttpChunkedParser;
import tp.pdc.proxy.parser.body.HttpContentLengthLeetParser;
import tp.pdc.proxy.parser.body.HttpContentLengthParser;
import tp.pdc.proxy.parser.body.HttpNoBodyParser;
import tp.pdc.proxy.parser.interfaces.HttpBodyParser;
import tp.pdc.proxy.parser.interfaces.HttpRequestParser;
import tp.pdc.proxy.parser.interfaces.HttpResponseParser;

import java.nio.charset.Charset;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class HttpBodyParserFactoryTest {

	private final HttpBodyParserFactory bodyParserFactory = HttpBodyParserFactory.getInstance();

	@Mock private HttpRequestParser requestParserMock;

	@Mock private HttpResponseParser responseParserMock;

	private L33tFlag l33tFlag = L33tFlag.getInstance();

	@Before
	public void setUp () throws Exception {
		l33tFlag.unset();
	}

	@Test
	public void testClientContentLength () throws ParserFormatException {
		when(requestParserMock.hasHeaderValue(Header.CONTENT_LENGTH)).thenReturn(true);
		when(requestParserMock.getHeaderValue(Header.CONTENT_LENGTH))
			.thenReturn("5".getBytes(Charset.forName("ASCII")));
		when(requestParserMock.hasMethod()).thenReturn(true);
		when(requestParserMock.getMethod()).thenReturn(Method.POST);

		HttpBodyParser parser = bodyParserFactory.getClientHttpBodyParser(requestParserMock);

		assertNotNull(parser);
		assertEquals(HttpContentLengthParser.class, parser.getClass());
	}

	@Test
	public void testServerContentLength () throws ParserFormatException {
		when(responseParserMock.hasHeaderValue(Header.CONTENT_LENGTH)).thenReturn(true);
		when(responseParserMock.getHeaderValue(Header.CONTENT_LENGTH))
			.thenReturn("5".getBytes(Charset.forName("ASCII")));
		when(responseParserMock.getStatusCode()).thenReturn(200);

		HttpBodyParser parser =
			bodyParserFactory.getServerHttpBodyParser(responseParserMock, Method.POST);

		assertNotNull(parser);
		assertEquals(HttpContentLengthParser.class, parser.getClass());
	}

	@Test
	public void testClientChunkedParser () throws ParserFormatException {
		when(requestParserMock.hasHeaderValue(Header.TRANSFER_ENCODING)).thenReturn(true);
		when(requestParserMock.getHeaderValue(Header.TRANSFER_ENCODING))
			.thenReturn(HeaderValue.CHUNKED.getValue());
		when(requestParserMock.hasMethod()).thenReturn(true);
		when(requestParserMock.getMethod()).thenReturn(Method.POST);

		HttpBodyParser parser = bodyParserFactory.getClientHttpBodyParser(requestParserMock);

		assertNotNull(parser);
		assertEquals(HttpChunkedParser.class, parser.getClass());
	}

	@Test
	public void testServerChunkedParser () throws ParserFormatException {
		when(responseParserMock.hasHeaderValue(Header.TRANSFER_ENCODING)).thenReturn(true);
		when(responseParserMock.getHeaderValue(Header.TRANSFER_ENCODING))
			.thenReturn(HeaderValue.CHUNKED.getValue());
		when(responseParserMock.getStatusCode()).thenReturn(200);

		HttpBodyParser parser =
			bodyParserFactory.getServerHttpBodyParser(responseParserMock, Method.POST);

		assertNotNull(parser);
		assertEquals(HttpChunkedParser.class, parser.getClass());
	}

	@Test
	public void testClientNullParser () throws ParserFormatException {
		when(requestParserMock.hasMethod()).thenReturn(true);
		when(requestParserMock.getMethod()).thenReturn(Method.GET);

		HttpBodyParser parser = bodyParserFactory.getClientHttpBodyParser(requestParserMock);
		assertNotNull(parser);
		assertEquals(HttpNoBodyParser.getInstance(), parser);
	}

	@Test
	public void testServerMethodNullParser () throws ParserFormatException {
		HttpBodyParser parser =
			bodyParserFactory.getServerHttpBodyParser(responseParserMock, Method.HEAD);
		assertNotNull(parser);
		assertEquals(HttpNoBodyParser.getInstance(), parser);
	}

	@Test
	public void testServerStatusCodeNullParser () throws ParserFormatException {
		assertStatusCodeNoBodyParser(100);
		assertStatusCodeNoBodyParser(101);
		assertStatusCodeNoBodyParser(102);
		assertStatusCodeNoBodyParser(103);
		assertStatusCodeNoBodyParser(104);
		assertStatusCodeNoBodyParser(204);
		assertStatusCodeNoBodyParser(304);
	}

	@Test
	public void testClientContentLengthL33tParser () throws ParserFormatException {
		l33tFlag.set();
		when(requestParserMock.hasHeaderValue(Header.CONTENT_LENGTH)).thenReturn(true);
		when(requestParserMock.getHeaderValue(Header.CONTENT_LENGTH))
			.thenReturn("5".getBytes(Charset.forName("ASCII")));
		when(requestParserMock.hasHeaderValue(Header.CONTENT_TYPE)).thenReturn(true);
		when(requestParserMock.getHeaderValue(Header.CONTENT_TYPE))
			.thenReturn("text/plain; charset=utf-8".getBytes(Charset.forName("ASCII")));
		when(requestParserMock.hasMethod()).thenReturn(true);
		when(requestParserMock.getMethod()).thenReturn(Method.POST);

		HttpBodyParser parser = bodyParserFactory.getClientHttpBodyParser(requestParserMock);
		assertNotNull(parser);
		assertEquals(HttpContentLengthLeetParser.class, parser.getClass());
	}

	private void assertStatusCodeNoBodyParser (int statusCode) throws ParserFormatException {
		when(responseParserMock.getStatusCode()).thenReturn(statusCode);
		HttpBodyParser parser =
			bodyParserFactory.getServerHttpBodyParser(responseParserMock, Method.GET);
		assertNotNull(parser);
		assertEquals(HttpNoBodyParser.getInstance(), parser);
	}
}
