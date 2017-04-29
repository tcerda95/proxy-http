package tp.pdc.proxy.parser.factory;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import static org.mockito.Mockito.*;

import java.nio.charset.Charset;

import org.mockito.runners.MockitoJUnitRunner;

import tp.pdc.proxy.exceptions.IllegalHttpHeadersException;
import tp.pdc.proxy.header.Header;
import tp.pdc.proxy.header.HeaderValue;
import tp.pdc.proxy.header.Method;
import tp.pdc.proxy.parser.HttpChunkedParser;
import tp.pdc.proxy.parser.HttpContentLengthParser;
import tp.pdc.proxy.parser.HttpNullBodyParser;
import tp.pdc.proxy.parser.interfaces.HttpBodyParser;
import tp.pdc.proxy.parser.interfaces.HttpHeaderParser;

@RunWith(MockitoJUnitRunner.class)
public class HttpBodyParserFactoryTest {

	@Mock
	private HttpHeaderParser headersParserMock;
		
	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testClientContentLength() throws IllegalHttpHeadersException {
		when(headersParserMock.hasHeaderValue(Header.CONTENT_LENGTH)).thenReturn(true);
		when(headersParserMock.getHeaderValue(Header.CONTENT_LENGTH)).thenReturn("5".getBytes(Charset.forName("ASCII")));

		HttpBodyParser parser = HttpBodyParserFactory.getClientHttpBodyParser(headersParserMock, Method.POST);
		
		assertNotNull(parser);
		assertEquals(HttpContentLengthParser.class, parser.getClass());
	}

	@Test
	public void testServerContentLength() throws IllegalHttpHeadersException {
		when(headersParserMock.hasHeaderValue(Header.CONTENT_LENGTH)).thenReturn(true);
		when(headersParserMock.getHeaderValue(Header.CONTENT_LENGTH)).thenReturn("5".getBytes(Charset.forName("ASCII")));

		HttpBodyParser parser = HttpBodyParserFactory.getServerHttpBodyParser(headersParserMock, Method.POST);
		
		assertNotNull(parser);
		assertEquals(HttpContentLengthParser.class, parser.getClass());		
	}
	
	@Test
	public void testClientChunkedParser() throws IllegalHttpHeadersException {
		when(headersParserMock.hasHeaderValue(Header.TRANSFER_ENCODING)).thenReturn(true);
		when(headersParserMock.getHeaderValue(Header.TRANSFER_ENCODING)).thenReturn(HeaderValue.CHUNKED.getValue());

		HttpBodyParser parser = HttpBodyParserFactory.getClientHttpBodyParser(headersParserMock, Method.POST);
		
		assertNotNull(parser);
		assertEquals(HttpChunkedParser.class, parser.getClass());		
	}
	
	@Test
	public void testServerChunkedParser() throws IllegalHttpHeadersException {
		when(headersParserMock.hasHeaderValue(Header.TRANSFER_ENCODING)).thenReturn(true);
		when(headersParserMock.getHeaderValue(Header.TRANSFER_ENCODING)).thenReturn(HeaderValue.CHUNKED.getValue());

		HttpBodyParser parser = HttpBodyParserFactory.getServerHttpBodyParser(headersParserMock, Method.POST);
		
		assertNotNull(parser);
		assertEquals(HttpChunkedParser.class, parser.getClass());		
	}
	
	@Test
	public void testClientNullParser() throws IllegalHttpHeadersException {
		HttpBodyParser parser = HttpBodyParserFactory.getServerHttpBodyParser(headersParserMock, Method.GET);		
		assertNotNull(parser);
		assertEquals(HttpNullBodyParser.getInstance(), parser);
		
		parser = HttpBodyParserFactory.getServerHttpBodyParser(headersParserMock, Method.HEAD);		
		assertNotNull(parser);
		assertEquals(HttpNullBodyParser.getInstance(), parser);
	}
}
