package tp.pdc.proxy.parser;

import static org.junit.Assert.*;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import org.junit.Before;
import org.junit.Test;

import tp.pdc.proxy.exceptions.ParserFormatException;
import tp.pdc.proxy.header.Method;
import tp.pdc.proxy.parser.mainParsers.HttpRequestParserImpl;

public class HttpRequestParserImplTest {

	private HttpRequestParserImpl parser;
	private ByteBuffer inputBuffer;
	private ByteBuffer outputBuffer;
	
	@Before
	public void setUp() throws Exception {
		parser = new HttpRequestParserImpl();
		outputBuffer = ByteBuffer.allocate(4000);
	}

	@Test
	public void testFinished() throws UnsupportedEncodingException, ParserFormatException {
		String request =   "GET / HTTP/1.1\r\n"
				 + "Host: localhost:8080\r\n"
				 + "\r\n";
		
		inputBuffer = ByteBuffer.wrap(request.getBytes());
		
 		assertTrue(parser.parse(inputBuffer, outputBuffer));
		assertTrue(parser.hasFinished());
	}

	@Test
	public void testNotFinished() throws UnsupportedEncodingException, ParserFormatException {
		String request = "GET / HTTP/1.1\r\n"
				+ "Host: localhost:8080\r\n"
				+ "X-Header: Custom\r\n";

		inputBuffer = ByteBuffer.wrap(request.getBytes());

		assertFalse(parser.parse(inputBuffer, outputBuffer));
		assertFalse(parser.hasFinished());
	}

	@Test
	public void notHasHostTest() throws UnsupportedEncodingException, ParserFormatException {
		String request = "GET / HTTP/1.1\r\n"
				+ "X-Header: Custom\r\n"
				+ "\r\n";

		inputBuffer = ByteBuffer.wrap(request.getBytes());

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

		inputBuffer = ByteBuffer.wrap(request.getBytes());

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

		inputBuffer = ByteBuffer.wrap(request.getBytes());

		parser.parse(inputBuffer, outputBuffer);

		assertArrayEquals(host.getBytes(), parser.getHostValue());
	}

	@Test
	public void getHostInURITest() throws UnsupportedEncodingException, ParserFormatException {
		String host = "http://localhost:8080";
		String request =  "GET " + host + "/ HTTP/1.1\r\n"
				+ "X-Header: Custom\r\n"
				+ "X-Header-2: Custom\r\n"
				+ "\r\n";

		inputBuffer = ByteBuffer.wrap(request.getBytes());

		parser.parse(inputBuffer, outputBuffer);

		assertArrayEquals(host.getBytes(), parser.getHostValue());
	}

	@Test
	public void getJUSTHostInURITest() throws UnsupportedEncodingException, ParserFormatException {
		String host = "http://localhost:8080";
		String relative = "/hello/give/me/the/resource/a.html";
		String request =  "GET " + host + relative + "/ HTTP/1.1\r\n"
				+ "X-Header: Custom\r\n"
				+ "X-Header-2: Custom\r\n"
				+ "\r\n";

		inputBuffer = ByteBuffer.wrap(request.getBytes());

		parser.parse(inputBuffer, outputBuffer);

		assertArrayEquals(host.getBytes(), parser.getHostValue());
	}



	@Test
	public void hasHostInURITest() throws UnsupportedEncodingException, ParserFormatException {
		String request = "GET http://google.com HTTP/1.1\r\n"
				+ "X-Header: Custom\r\n"
				+ "\r\n";

		inputBuffer = ByteBuffer.wrap(request.getBytes());

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

		inputBuffer = ByteBuffer.wrap((get + nomethod).getBytes());
		assertTrue(parser.parse(inputBuffer, outputBuffer));
		assertTrue(parser.hasMethod(Method.GET));

		outputBuffer.clear();
		inputBuffer = ByteBuffer.wrap((head + nomethod).getBytes());
		parser = new HttpRequestParserImpl();
		assertTrue(parser.parse(inputBuffer, outputBuffer));
		assertTrue(parser.hasMethod(Method.HEAD));

		outputBuffer.clear();
		inputBuffer = ByteBuffer.wrap((post + nomethod).getBytes());
		parser = new HttpRequestParserImpl();
		assertTrue(parser.parse(inputBuffer, outputBuffer));
	}

	@Test(expected = ParserFormatException.class)
	public void invalidFormatTest() throws UnsupportedEncodingException, ParserFormatException {
		String request =   "GET / HTTP/1.1\n\n"
				+ "Host: localhost:8080\r\n"
				+ "\r\n";

		inputBuffer = ByteBuffer.wrap(request.getBytes());

		parser.parse(inputBuffer, outputBuffer);
	}

	@Test(expected = ParserFormatException.class)
	public void invalidMethodTest() throws UnsupportedEncodingException, ParserFormatException {
		String request =   "GETT / HTTP/1.1\n\n"
				+ "Host: localhost:8080\r\n"
				+ "\r\n";

		inputBuffer = ByteBuffer.wrap(request.getBytes());

		parser.parse(inputBuffer, outputBuffer);
	}

}
