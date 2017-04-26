package tp.pdc.proxy.parser;

import static org.junit.Assert.*;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import org.junit.Before;
import org.junit.Test;

import tp.pdc.proxy.exceptions.ParserFormatException;

public class HttpChunkedParserTest {

	private HttpChunkedParser parser;
	private ByteBuffer inputBuffer;
	private ByteBuffer outputBuffer;
	
	@Before
	public void setUp() throws Exception {
		parser = new HttpChunkedParser();
		outputBuffer = ByteBuffer.allocate(4000);
	}

	@Test
	public void testFinished() throws ParserFormatException, UnsupportedEncodingException {
		String chunked = "7\r\n"
						 + "hola co\r\n"
						 + "8\r\n"
						 + "mo te va\r\n"
						 + "0\r\n"
						 + "\r\n";
		inputBuffer = ByteBuffer.wrap(chunked.getBytes("ASCII"));
		
		parser.parse(inputBuffer, outputBuffer);
		assertTrue(parser.hasFinished());
	}

	@Test
	public void testNotFinished() throws ParserFormatException, UnsupportedEncodingException {
		String chunked = "7\r\n"
						 + "hola co\r\n"
						 + "8\r\n"
						 + "mo te v";
		inputBuffer = ByteBuffer.wrap(chunked.getBytes("ASCII"));
		
		parser.parse(inputBuffer, outputBuffer);
		assertFalse(parser.hasFinished());
	}
}
