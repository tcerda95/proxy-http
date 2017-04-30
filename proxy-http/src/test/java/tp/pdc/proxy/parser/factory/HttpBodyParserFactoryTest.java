package tp.pdc.proxy.parser.factory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import java.nio.charset.Charset;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import tp.pdc.proxy.exceptions.IllegalHttpHeadersException;
import tp.pdc.proxy.header.Header;
import tp.pdc.proxy.header.HeaderValue;
import tp.pdc.proxy.header.Method;
import tp.pdc.proxy.parser.HttpChunkedParser;
import tp.pdc.proxy.parser.HttpContentLengthParser;
import tp.pdc.proxy.parser.HttpNullBodyParser;
import tp.pdc.proxy.parser.interfaces.HttpBodyParser;
import tp.pdc.proxy.parser.interfaces.HttpRequestParser;
import tp.pdc.proxy.parser.interfaces.HttpResponseParser;

@RunWith(MockitoJUnitRunner.class)
public class HttpBodyParserFactoryTest {

	@Mock
	private HttpRequestParser requestParserMock;

	@Mock
	private HttpResponseParser responseParserMock;

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testClientContentLength() throws IllegalHttpHeadersException {
		when(requestParserMock.hasHeaderValue(Header.CONTENT_LENGTH)).thenReturn(true);
		when(requestParserMock.getHeaderValue(Header.CONTENT_LENGTH)).thenReturn("5".getBytes(Charset.forName("ASCII")));
		when(requestParserMock.hasMethod(Method.POST)).thenReturn(true);

		HttpBodyParser parser = HttpBodyParserFactory.getClientHttpBodyParser(requestParserMock);
		
		assertNotNull(parser);
		assertEquals(HttpContentLengthParser.class, parser.getClass());
	}

	@Test
	public void testServerContentLength() throws IllegalHttpHeadersException {
		when(responseParserMock.hasHeaderValue(Header.CONTENT_LENGTH)).thenReturn(true);
		when(responseParserMock.getHeaderValue(Header.CONTENT_LENGTH)).thenReturn("5".getBytes(Charset.forName("ASCII")));
		when(responseParserMock.getStatusCode()).thenReturn(200);

		HttpBodyParser parser = HttpBodyParserFactory.getServerHttpBodyParser(responseParserMock, Method.POST);
		
		assertNotNull(parser);
		assertEquals(HttpContentLengthParser.class, parser.getClass());		
	}
	
	@Test
	public void testClientChunkedParser() throws IllegalHttpHeadersException {
		when(requestParserMock.hasHeaderValue(Header.TRANSFER_ENCODING)).thenReturn(true);
		when(requestParserMock.getHeaderValue(Header.TRANSFER_ENCODING)).thenReturn(HeaderValue.CHUNKED.getValue());
		when(requestParserMock.hasMethod(Method.POST)).thenReturn(true);

		HttpBodyParser parser = HttpBodyParserFactory.getClientHttpBodyParser(requestParserMock);
		
		assertNotNull(parser);
		assertEquals(HttpChunkedParser.class, parser.getClass());		
	}
	
	@Test
	public void testServerChunkedParser() throws IllegalHttpHeadersException {
		when(responseParserMock.hasHeaderValue(Header.TRANSFER_ENCODING)).thenReturn(true);
		when(responseParserMock.getHeaderValue(Header.TRANSFER_ENCODING)).thenReturn(HeaderValue.CHUNKED.getValue());
		when(responseParserMock.getStatusCode()).thenReturn(200);

		HttpBodyParser parser = HttpBodyParserFactory.getServerHttpBodyParser(responseParserMock, Method.POST);
		
		assertNotNull(parser);
		assertEquals(HttpChunkedParser.class, parser.getClass());		
	}
	
	@Test
	public void testClientNullParser() throws IllegalHttpHeadersException {
		when(requestParserMock.hasMethod(Method.POST)).thenReturn(false);
		
		HttpBodyParser parser = HttpBodyParserFactory.getClientHttpBodyParser(requestParserMock);
		assertNotNull(parser);
		assertEquals(HttpNullBodyParser.getInstance(), parser);
	}
	
	@Test
	public void testServerMethodNullParser() throws IllegalHttpHeadersException {
		HttpBodyParser parser = HttpBodyParserFactory.getServerHttpBodyParser(responseParserMock, Method.HEAD);		
		assertNotNull(parser);
		assertEquals(HttpNullBodyParser.getInstance(), parser);
	}
	
	@Test
	public void testServerStatusCodeNullParser() throws IllegalHttpHeadersException {
		assertStatusCodeNullParser(100);
		assertStatusCodeNullParser(101);
		assertStatusCodeNullParser(102);
		assertStatusCodeNullParser(103);
		assertStatusCodeNullParser(104);
		assertStatusCodeNullParser(204);
		assertStatusCodeNullParser(304);
	}

	private void assertStatusCodeNullParser(int statusCode) throws IllegalHttpHeadersException {
		when(responseParserMock.getStatusCode()).thenReturn(statusCode);
		HttpBodyParser parser = HttpBodyParserFactory.getServerHttpBodyParser(responseParserMock, Method.GET);
		assertNotNull(parser);
		assertEquals(HttpNullBodyParser.getInstance(), parser);		
	}
}
