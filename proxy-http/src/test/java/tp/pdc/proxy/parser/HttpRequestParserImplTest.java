package tp.pdc.proxy.parser;

import static org.junit.Assert.*;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import org.junit.Before;
import org.junit.Test;

import tp.pdc.proxy.ProxyProperties;
import tp.pdc.proxy.exceptions.ParserFormatException;
import tp.pdc.proxy.header.Method;
import tp.pdc.proxy.parser.factory.HttpRequestParserFactory;
import tp.pdc.proxy.parser.interfaces.HttpRequestParser;

public class HttpRequestParserImplTest {

	private HttpRequestParser parser;
	private ByteBuffer inputBuffer;
	private ByteBuffer outputBuffer;

	private static final Charset charset = ProxyProperties.getInstance().getCharset();

	@Before
	public void setUp() throws Exception {
		parser = HttpRequestParserFactory.getInstance().getRequestParser();
		outputBuffer = ByteBuffer.allocate(4000);
	}

	@Test
	public void testFinished() throws UnsupportedEncodingException, ParserFormatException {
		String request =   "GET / HTTP/1.1\r\n"
				 + "Host: localhost:8080\r\n"
				 + "\r\n";
		
		inputBuffer = ByteBuffer.wrap(request.getBytes(charset));
		
 		assertTrue(parser.parse(inputBuffer, outputBuffer));
		assertTrue(parser.hasFinished());
	}

	@Test
	public void testNotFinished() throws UnsupportedEncodingException, ParserFormatException {
		String request = "GET / HTTP/1.1\r\n"
				+ "Host: localhost:8080\r\n"
				+ "X-Header: Custom\r\n";

		inputBuffer = ByteBuffer.wrap(request.getBytes(charset));

		assertFalse(parser.parse(inputBuffer, outputBuffer));
		assertFalse(parser.hasFinished());
	}

	@Test
	public void notHasHostTest() throws UnsupportedEncodingException, ParserFormatException {
		String request = "GET / HTTP/1.1\r\n"
				+ "X-Header: Custom\r\n"
				+ "\r\n";

		inputBuffer = ByteBuffer.wrap(request.getBytes(charset));

		assertTrue(parser.parse(inputBuffer, outputBuffer));
		assertTrue(parser.hasFinished());
		assertFalse(parser.hasHost());
	}

	@Test
	public void hasHostInHeaderTest() throws UnsupportedEncodingException, ParserFormatException {
		String request = "GET / HTTP/1.1\r\n"
				+ "Host: localhost:8080\r\n"
				+ "X-Header: Custom\r\n"
				+ "\r\n";

		inputBuffer = ByteBuffer.wrap(request.getBytes(charset));

		assertTrue(parser.parse(inputBuffer, outputBuffer));
		assertTrue(parser.hasFinished());
		assertTrue(parser.hasHost());
	}

	@Test
	public void getHostInHeaderTest() throws UnsupportedEncodingException, ParserFormatException {
		String host = "localhost:8080";
		String request =  "GET / HTTP/1.1\r\n"
				+ "X-Header: Custom\r\n"
				+ "Host: " + host + "\r\n"
				+ "X-Header-2: Custom\r\n"
				+ "\r\n";

		inputBuffer = ByteBuffer.wrap(request.getBytes(charset));

		parser.parse(inputBuffer, outputBuffer);

		assertArrayEquals(host.getBytes(charset), parser.getHostValue());
	}

	@Test
	public void getHostInURITest() throws UnsupportedEncodingException, ParserFormatException {
		String scheme = "http://";
		String host = "localhost:8080";
		String request =  "GET " + scheme + host + "/ HTTP/1.1\r\n"
				+ "X-Header: Custom\r\n"
				+ "X-Header-2: Custom\r\n"
				+ "\r\n";

		inputBuffer = ByteBuffer.wrap(request.getBytes(charset));

		parser.parse(inputBuffer, outputBuffer);

		assertArrayEquals(host.getBytes(charset), parser.getHostValue());
	}

	@Test
	public void getJUSTHostInURITest() throws UnsupportedEncodingException, ParserFormatException {
		String scheme = "http://";
		String host = "localhost:8080";
		String relative = "/hello/give/me/the/resource/a.html";
		String request =  "GET " + scheme + host + relative + "/ HTTP/1.1\r\n"
				+ "X-Header: Custom\r\n"
				+ "X-Header-2: Custom\r\n"
				+ "\r\n";

		inputBuffer = ByteBuffer.wrap(request.getBytes(charset));

		parser.parse(inputBuffer, outputBuffer);

		assertArrayEquals(host.getBytes(charset), parser.getHostValue());
	}



	@Test
	public void hasHostInURITest() throws UnsupportedEncodingException, ParserFormatException {
		String request = "GET http://google.com HTTP/1.1\r\n"
				+ "X-Header: Custom\r\n"
				+ "\r\n";

		inputBuffer = ByteBuffer.wrap(request.getBytes(charset));

		assertTrue(parser.parse(inputBuffer, outputBuffer));
		assertTrue(parser.hasFinished());
		assertTrue(parser.hasHost());
	}

	@Test
	public void getMethodTest() throws UnsupportedEncodingException, ParserFormatException {
		String get = "GET", head = "HEAD", post = "POST";

		String nomethod = " / HTTP/1.1\r\n"
				+ "Host: localhost:8080\r\n"
				+ "\r\n";

		inputBuffer = ByteBuffer.wrap((get + nomethod).getBytes(charset));
		assertTrue(parser.parse(inputBuffer, outputBuffer));
		assertTrue(parser.hasMethod());
		assertEquals(Method.GET, parser.getMethod());

		outputBuffer.clear();
		inputBuffer = ByteBuffer.wrap((head + nomethod).getBytes(charset));
		parser.reset();
		assertTrue(parser.parse(inputBuffer, outputBuffer));
		assertTrue(parser.hasMethod());
		assertEquals(Method.HEAD, parser.getMethod());

		outputBuffer.clear();
		inputBuffer = ByteBuffer.wrap((post + nomethod).getBytes(charset));
		parser.reset();
		assertTrue(parser.parse(inputBuffer, outputBuffer));
	}

	@Test(expected = ParserFormatException.class)
	public void invalidFormatTest() throws UnsupportedEncodingException, ParserFormatException {
		String request =   "GET / HTTP/1.1\n\n"
				+ "Host: localhost:8080\r\n"
				+ "\r\n";

		inputBuffer = ByteBuffer.wrap(request.getBytes(charset));
		parser.parse(inputBuffer, outputBuffer);
	}

	@Test(expected = ParserFormatException.class)
	public void invalidMethodTest() throws UnsupportedEncodingException, ParserFormatException {
		String request =    "GETT / HTTP/1.1\r\n"
				+ "Host: localhost:8080\r\n"
				+ "\r\n";

		inputBuffer = ByteBuffer.wrap(request.getBytes(charset));
		parser.parse(inputBuffer, outputBuffer);
	}

	@Test(expected = ParserFormatException.class)
	public void methodTooLongTest() throws UnsupportedEncodingException, ParserFormatException {
		String request =   "GETGETGETGETGETGETGETGETGET / HTTP/1.1\r\n"
				+ "Host: localhost:8080\r\n"
				+ "\r\n";

		inputBuffer = ByteBuffer.wrap(request.getBytes(charset));
		parser.parse(inputBuffer, outputBuffer);
	}

	@Test(expected = ParserFormatException.class)
	public void URIHostTooLongTest() throws UnsupportedEncodingException, ParserFormatException {
		String longHost = "http://www.google.com.www.google.com.www.google.com.www.google.com.www.google.com"
				+ "www.google.com.www.google.com.www.google.com.www.google.com.www.google.com.www.google.com"
				+ "www.google.com.www.google.com.www.google.com.www.google.com.www.google.com.www.google.com";


		String request = "GET " + longHost + " HTTP/1.1\r\n"
				+ "Host: localhost:8080\r\n"
				+ "\r\n";

		inputBuffer = ByteBuffer.wrap(request.getBytes(charset));
		parser.parse(inputBuffer, outputBuffer);
	}

}
