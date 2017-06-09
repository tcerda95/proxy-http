package tp.pdc.proxy.parser.body;

import org.junit.Before;
import org.junit.Test;

import tp.pdc.proxy.exceptions.ParserFormatException;
import tp.pdc.proxy.properties.ProxyProperties;

import java.nio.ByteBuffer;

import static org.junit.Assert.*;

public class HttpContentLengthParserTest {

	private static final ProxyProperties PROPERTIES = ProxyProperties.getInstance();

	private HttpContentLengthParser parser;
	private ByteBuffer inputBuffer;
	private ByteBuffer outputBuffer;
	private String body;


	@Before
	public void setUp () throws Exception {
		outputBuffer = ByteBuffer.allocate(50);
		body = "hola como te va";
		inputBuffer = ByteBuffer.wrap(body.getBytes(PROPERTIES.getCharset()));
	}

	@Test
	public void testFinished () throws ParserFormatException {
		parser = new HttpContentLengthParser(body.length());

		parser.parse(inputBuffer, outputBuffer);
		assertEquals(body, byteBufferToString(outputBuffer));
		assertTrue(parser.hasFinished());
		assertFalse(inputBuffer.hasRemaining());
	}

	@Test
	public void testNotFinishedLongerContentLength () throws ParserFormatException {
		parser = new HttpContentLengthParser(body.length() + 5);

		parser.parse(inputBuffer, outputBuffer);
		assertEquals(body, byteBufferToString(outputBuffer));
		assertFalse(parser.hasFinished());
		assertFalse(inputBuffer.hasRemaining());

		// Testing the 5 leftover bytes now:

		inputBuffer = ByteBuffer.wrap(body.getBytes(PROPERTIES.getCharset()));
		parser.parse(inputBuffer, outputBuffer);
		assertTrue(parser.hasFinished());
		assertTrue(inputBuffer.hasRemaining());
		assertEquals(body.length() - 5, inputBuffer.remaining());
		assertEquals(body + "hola ", byteBufferToString(outputBuffer));
	}

	@Test
	public void testLeftoverByte() throws ParserFormatException {
		parser = new HttpContentLengthParser(body.length() + 1);

		parser.parse(inputBuffer, outputBuffer);
		assertFalse(parser.hasFinished());
		assertEquals(body, byteBufferToString(outputBuffer));
	}

	@Test
	public void testMinusOneByte() throws ParserFormatException {
		parser = new HttpContentLengthParser(body.length() - 1);

		parser.parse(inputBuffer, outputBuffer);
		assertTrue(parser.hasFinished());
		assertEquals("hola como te v", byteBufferToString(outputBuffer));
		assertEquals(1, inputBuffer.remaining());
	}

	@Test
	public void testFinishedShorterContentLength () throws ParserFormatException {
		parser = new HttpContentLengthParser(body.length() - 2);

		parser.parse(inputBuffer, outputBuffer);
		String parsed = byteBufferToString(outputBuffer);

		assertNotEquals(body, parsed);
		assertEquals("hola como te ", parsed);
		assertTrue(parser.hasFinished());
		assertTrue(inputBuffer.hasRemaining());
		assertEquals(2, inputBuffer.remaining());
	}

	@Test
	public void testShortOutputBuffer () throws ParserFormatException {
		parser = new HttpContentLengthParser(body.length());
		outputBuffer = ByteBuffer.allocate(4);

		assertFalse(parser.parse(inputBuffer, outputBuffer));
		assertFalse(outputBuffer.hasRemaining());
		assertEquals("hola", byteBufferToString(outputBuffer));
		outputBuffer.clear();

		assertFalse(parser.parse(inputBuffer, outputBuffer));
		assertFalse(outputBuffer.hasRemaining());
		assertEquals(" com", byteBufferToString(outputBuffer));
		outputBuffer.clear();

		assertFalse(parser.parse(inputBuffer, outputBuffer));
		assertFalse(outputBuffer.hasRemaining());
		assertEquals("o te", byteBufferToString(outputBuffer));
		outputBuffer.clear();

		assertTrue(parser.parse(inputBuffer, outputBuffer));
		assertTrue(outputBuffer.hasRemaining());
		assertEquals(1, outputBuffer.remaining());
		assertEquals(" va", byteBufferToString(outputBuffer));
	}

	@Test
	public void multipleParseTest() throws ParserFormatException {
		parser = new HttpContentLengthParser(body.length());

		parser.parse(inputBuffer, outputBuffer);
		assertTrue(parser.hasFinished());
		parser.parse(inputBuffer, outputBuffer);
		assertTrue(parser.hasFinished());
	}

	private String byteBufferToString (ByteBuffer buffer) {
		return new String(buffer.array(),  0, buffer.position(),
			PROPERTIES.getCharset());
	}
}
