package tp.pdc.proxy.parser;

import static org.junit.Assert.*;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import org.junit.Before;
import org.junit.Test;

import tp.pdc.proxy.exceptions.ParserFormatException;

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

}
