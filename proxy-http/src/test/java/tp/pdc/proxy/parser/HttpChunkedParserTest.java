package tp.pdc.proxy.parser;

import static org.junit.Assert.*;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import org.junit.Before;
import org.junit.Test;

import tp.pdc.proxy.ProxyProperties;
import tp.pdc.proxy.exceptions.ParserFormatException;

public class HttpChunkedParserTest {

	private static ProxyProperties PROPERTIES = ProxyProperties.getInstance();
	
	private HttpChunkedParser parser;
	private ByteBuffer inputBuffer;
	private ByteBuffer outputBuffer;
	
	@Before
	public void setUp() throws Exception {
		parser = new HttpChunkedParser(false);
		outputBuffer = ByteBuffer.allocate(4000);
	}

	@Test
	public void testFinished() throws ParserFormatException, UnsupportedEncodingException {
		String chunked =   "7\r\n"
						 + "hola co\r\n"
						 + "8\r\n"
						 + "mo te va\r\n"
						 + "0\r\n"
						 + "\r\n";
		
		inputBuffer = ByteBuffer.wrap(chunked.getBytes("ASCII"));
		
		parser.parse(inputBuffer, outputBuffer);
		assertEquals(chunked, new String(outputBuffer.array(), 0, outputBuffer.position(), PROPERTIES.getCharset()));
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
		assertEquals(chunked, new String(outputBuffer.array(), 0, outputBuffer.position(), PROPERTIES.getCharset()));
		assertFalse(parser.hasFinished());
	}
	
	@Test
	public void testIncludeCRLF() throws ParserFormatException, UnsupportedEncodingException {
		String chunked = "7\r\n"
				 + "hola co\r\n"
				 + "A\r\n"
				 + "mo te va\r\n\r\n"
				 + "0\r\n"
				 + "\r\n";
		inputBuffer = ByteBuffer.wrap(chunked.getBytes("ASCII"));

		parser.parse(inputBuffer, outputBuffer);
		assertEquals(chunked, new String(outputBuffer.array(), 0, outputBuffer.position(), PROPERTIES.getCharset()));
		assertTrue(parser.hasFinished());
	}
	
	@Test
	public void testHexaSizeValues() throws ParserFormatException, UnsupportedEncodingException {
		String chunked = "a\r\n"
				 + "hola como \r\n"
				 + "E\r\n"
				 + "andas en el di\r\n"
				 + "11\r\n"
				 + "a de hoy, queria \r\n"
				 + "1A\r\n"
				 + "saber como vas con tu vida\r\n"
				 + "A1\r\n"
				 + "desde que te fuiste..............................."
				 + ".................................................."
				 + ".................................................."
				 + "..........!\r\n"
				 + "0\r\n"
				 + "\r\n";
		inputBuffer = ByteBuffer.wrap(chunked.getBytes("ASCII"));

		parser.parse(inputBuffer, outputBuffer);
		assertEquals(chunked, new String(outputBuffer.array(), 0, outputBuffer.position(), PROPERTIES.getCharset()));
		assertTrue(parser.hasFinished());
	}
}
