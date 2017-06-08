package tp.pdc.proxy.parser;

import org.junit.Before;
import org.junit.Test;
import tp.pdc.proxy.ProxyProperties;
import tp.pdc.proxy.exceptions.ParserFormatException;
import tp.pdc.proxy.parser.body.HttpContentLengthLeetParser;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class HttpContentLengthLeetParserTest {

	private static ProxyProperties PROPERTIES = ProxyProperties.getInstance();

	private HttpContentLengthLeetParser parser;
	private ByteBuffer inputBuffer;
	private ByteBuffer outputBuffer;

	@Before
	public void setUp () throws Exception {
		outputBuffer = ByteBuffer.allocate(50);
	}

	@Test
	public void testLeet () throws ParserFormatException, UnsupportedEncodingException {
		String body = "hola como te vistes";

		String expectedOutput = "h0l4 <0m0 t3 v1st3s";

		parser = new HttpContentLengthLeetParser(body.length());

		inputBuffer = ByteBuffer.wrap(body.getBytes("ASCII"));

		parser.parse(inputBuffer, outputBuffer);
		assertEquals(expectedOutput,
			new String(outputBuffer.array(), 0, outputBuffer.position(), PROPERTIES.getCharset()));
		assertTrue(parser.hasFinished());
	}
}
