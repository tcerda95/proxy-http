package tp.pdc.proxy.parser.protocol;

import org.junit.Before;
import org.junit.Test;

import tp.pdc.proxy.exceptions.ParserFormatException;
import tp.pdc.proxy.metric.stub.ClientMetricStub;
import tp.pdc.proxy.metric.stub.ServerMetricStub;
import tp.pdc.proxy.parser.interfaces.PopisParser;
import tp.pdc.proxy.properties.ProxyProperties;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import static org.junit.Assert.assertEquals;

public class PopisParserExceptionRequestsTest {

	private static ProxyProperties PROPERTIES = ProxyProperties.getInstance();

	private PopisParser parser;
	private ByteBuffer inputBuffer;
	private ByteBuffer outputBuffer;

	@Before
	public void setUp () throws Exception {
		parser = new PopisParserImpl(new ClientMetricStub(), new ServerMetricStub());
		outputBuffer = ByteBuffer.allocate(4000);
	}

	@Test
	public void testTooLongHeader () throws UnsupportedEncodingException {

		String protocolInput = "client_bytes_writtenn\r\n" + "comova??\r\n" + "end\r\n";


		String expectedOutput = "-[TOO_LONG]client_bytes_writtenn[...]\r\n" + "+end\r\n";


		inputBuffer = ByteBuffer.wrap(protocolInput.getBytes("ASCII"));

		try {
			parser.parse(inputBuffer, outputBuffer);
		} catch (ParserFormatException e) {
		}
		assertEquals(expectedOutput,
			new String(outputBuffer.array(), 0, outputBuffer.position(), PROPERTIES.getCharset()));
	}

	@Test
	public void testNotValidHeader () throws UnsupportedEncodingException {

		String protocolInput = "lala8\r\n" + "isValid!?\r\n" + "end\r\n";


		String expectedOutput =
			"-[NO_MATCH]lala8\r\n" + "-[NOT_VALID]isvalid![...]\r\n" + "+end\r\n";


		inputBuffer = ByteBuffer.wrap(protocolInput.getBytes("ASCII"));

		try {
			parser.parse(inputBuffer, outputBuffer);
		} catch (ParserFormatException e) {
		}
		assertEquals(expectedOutput,
			new String(outputBuffer.array(), 0, outputBuffer.position(), PROPERTIES.getCharset()));

	}

	@Test
	public void testNotValidHeaderBis () throws UnsupportedEncodingException {

		String protocolInput =
			"method_count\r\n" + "*1\r\n" + "GET\r\n" + "lel?\r\n" + "client_bytes_read\r\n";


		String expectedOutput =
			"+method_count\r\n" + "+*1\r\n" + "+GET: 0\r\n" + "-[NOT_VALID]lel?[...]\r\n"
				+ "+end\r\n";


		inputBuffer = ByteBuffer.wrap(protocolInput.getBytes("ASCII"));

		try {
			parser.parse(inputBuffer, outputBuffer);
		} catch (ParserFormatException e) {
		}
		assertEquals(expectedOutput,
			new String(outputBuffer.array(), 0, outputBuffer.position(), PROPERTIES.getCharset()));

	}

	@Test
	public void testTooLongMethod () throws UnsupportedEncodingException {

		String protocolInput =
			"lala8\r\n" + "method_count\r\n" + "*5\r\n" + "GE\r\n" + "GET\r\n" + "oPTIONs\r\n"
				+ "OPCIONES\r\n" + "GET\r\n" + "end\r\n";


		String expectedOutput =
			"-[NO_MATCH]lala8\r\n" + "+method_count\r\n" + "+*5\r\n" + "-[NO_MATCH]GE\r\n"
				+ "+GET: 0\r\n" + "+OPTIONS: 0\r\n" + "-[TOO_LONG]OPCIONES[...]\r\n" + "+end\r\n";


		inputBuffer = ByteBuffer.wrap(protocolInput.getBytes("ASCII"));

		try {
			parser.parse(inputBuffer, outputBuffer);
		} catch (ParserFormatException e) {
		}
		assertEquals(expectedOutput,
			new String(outputBuffer.array(), 0, outputBuffer.position(), PROPERTIES.getCharset()));

	}

	@Test
	public void testNotValidMethod () throws UnsupportedEncodingException {

		String protocolInput =
			"lala8\r\n" + "method_count\r\n" + "*3\r\n" + "GE\r\n" + "GET\r\n" + "GET\n\r"
				+ "end\r\n";


		String expectedOutput =
			"-[NO_MATCH]lala8\r\n" + "+method_count\r\n" + "+*3\r\n" + "-[NO_MATCH]GE\r\n"
				+ "+GET: 0\r\n" + "-[NOT_VALID]GET\n[...]\r\n" + "+end\r\n";


		inputBuffer = ByteBuffer.wrap(protocolInput.getBytes("ASCII"));

		try {
			parser.parse(inputBuffer, outputBuffer);
		} catch (ParserFormatException e) {
		}
		assertEquals(expectedOutput,
			new String(outputBuffer.array(), 0, outputBuffer.position(), PROPERTIES.getCharset()));

	}

	@Test
	public void testTooLongStatusCount () throws UnsupportedEncodingException {

		String protocolInput =
			"lala8\r\n" + "status_code_count\r\n" + "*4\r\n" + "100\r\n" + "101\r\n" + "404\r\n"
				+ "4040\r\n" + "server_bytes_written\r\n" + "end\r\n";


		String expectedOutput =
			"-[NO_MATCH]lala8\r\n" + "+status_code_count\r\n" + "+*4\r\n" + "+100: 0\r\n"
				+ "+101: 0\r\n" + "+404: 0\r\n" + "-[TOO_LONG]4040[...]\r\n" + "+end\r\n";


		inputBuffer = ByteBuffer.wrap(protocolInput.getBytes("ASCII"));

		try {
			parser.parse(inputBuffer, outputBuffer);
		} catch (ParserFormatException e) {
		}
		assertEquals(expectedOutput,
			new String(outputBuffer.array(), 0, outputBuffer.position(), PROPERTIES.getCharset()));

	}

	@Test
	public void testNotValidStatusCount () throws UnsupportedEncodingException {

		String protocolInput =
			"lala8\r\n" + "status_code_count\r\n" + "*3\r\n" + "100\r\n" + "0000000000000404\r\n"
				+ "606\r\n" + "end\r\n";


		String expectedOutput =
			"-[NO_MATCH]lala8\r\n" + "+status_code_count\r\n" + "+*3\r\n" + "+100: 0\r\n"
				+ "+404: 0\r\n" + "-[NOT_VALID]6[...]\r\n" + "+end\r\n";


		inputBuffer = ByteBuffer.wrap(protocolInput.getBytes("ASCII"));

		try {
			parser.parse(inputBuffer, outputBuffer);
		} catch (ParserFormatException e) {
		}
		assertEquals(expectedOutput,
			new String(outputBuffer.array(), 0, outputBuffer.position(), PROPERTIES.getCharset()));

	}

	@Test
	public void testNotValidStatusCountBis () throws UnsupportedEncodingException {

		String protocolInput =
			"lala8\r\n" + "status_code_count\r\n" + "*3\r\n" + "0000000000000404\r\n"
				+ "00000013\r\n" + "606\r\n" + "end\r\n";


		String expectedOutput =
			"-[NO_MATCH]lala8\r\n" + "+status_code_count\r\n" + "+*3\r\n" + "+404: 0\r\n"
				+ "-[NOT_VALID]13[...]\r\n" + "+end\r\n";


		inputBuffer = ByteBuffer.wrap(protocolInput.getBytes("ASCII"));

		try {
			parser.parse(inputBuffer, outputBuffer);
		} catch (ParserFormatException e) {
		}
		assertEquals(expectedOutput,
			new String(outputBuffer.array(), 0, outputBuffer.position(), PROPERTIES.getCharset()));

	}

	@Test
	public void testNotValidArgSizeOutOfBounds () throws UnsupportedEncodingException {

		String protocolInput = "lala8\r\n" + "status_code_count\r\n" + "*19\r\n" + "end\r\n";


		String expectedOutput =
			"-[NO_MATCH]lala8\r\n" + "+status_code_count\r\n" + "-[NOT_VALID]*19[...]\r\n"
				+ "+end\r\n";


		inputBuffer = ByteBuffer.wrap(protocolInput.getBytes("ASCII"));

		try {
			parser.parse(inputBuffer, outputBuffer);
		} catch (ParserFormatException e) {
		}
		assertEquals(expectedOutput,
			new String(outputBuffer.array(), 0, outputBuffer.position(), PROPERTIES.getCharset()));

	}

	@Test
	public void testNotValidAsteriskNotPresent () throws UnsupportedEncodingException {

		String protocolInput = "lala8\r\n" + "status_code_count\r\n" + "\r\n" + "end\r\n";


		String expectedOutput =
			"-[NO_MATCH]lala8\r\n" + "+status_code_count\r\n" + "-[NOT_VALID]\r[...]\r\n"
				+ "+end\r\n";


		inputBuffer = ByteBuffer.wrap(protocolInput.getBytes("ASCII"));

		try {
			parser.parse(inputBuffer, outputBuffer);
		} catch (ParserFormatException e) {
		}
		assertEquals(expectedOutput,
			new String(outputBuffer.array(), 0, outputBuffer.position(), PROPERTIES.getCharset()));

	}

	@Test
	public void testNotValidArgSizeNotPresent () throws UnsupportedEncodingException {

		String protocolInput = "lala8\r\n" + "status_code_count\r\n" + "*\r\n" + "end\r\n";


		String expectedOutput =
			"-[NO_MATCH]lala8\r\n" + "+status_code_count\r\n" + "-[NOT_VALID]*[...]\r\n"
				+ "+end\r\n";


		inputBuffer = ByteBuffer.wrap(protocolInput.getBytes("ASCII"));

		try {
			parser.parse(inputBuffer, outputBuffer);
		} catch (ParserFormatException e) {
		}
		assertEquals(expectedOutput,
			new String(outputBuffer.array(), 0, outputBuffer.position(), PROPERTIES.getCharset()));

	}

	@Test
	public void testNotValidBufSizeArgSizeOutOfBounds () throws UnsupportedEncodingException {

		String protocolInput = "set_proxy_buf_size\r\n" + "*2\r\n";


		String expectedOutput = "+set_proxy_buf_size\r\n" + "-[NOT_VALID]*2[...]\r\n" + "+end\r\n";


		inputBuffer = ByteBuffer.wrap(protocolInput.getBytes("ASCII"));

		try {
			parser.parse(inputBuffer, outputBuffer);
		} catch (ParserFormatException e) {
		}
		assertEquals(expectedOutput,
			new String(outputBuffer.array(), 0, outputBuffer.position(), PROPERTIES.getCharset()));

	}

	@Test
	public void testNotValidBufSizeOutOfBounds () throws UnsupportedEncodingException {

		String protocolInput = "set_proxy_buf_size\r\n" + "*1\r\n" + "200\r\n";


		String expectedOutput =
			"+set_proxy_buf_size\r\n" + "+*1\r\n" + "-[NOT_VALID]200[...]\r\n" + "+end\r\n";


		inputBuffer = ByteBuffer.wrap(protocolInput.getBytes("ASCII"));

		try {
			parser.parse(inputBuffer, outputBuffer);
		} catch (ParserFormatException e) {
		}
		assertEquals(expectedOutput,
			new String(outputBuffer.array(), 0, outputBuffer.position(), PROPERTIES.getCharset()));

	}

	@Test
	public void testNotValidBufSizeOutOfBoundsBis () throws UnsupportedEncodingException {

		String protocolInput = "set_proxy_buf_size\r\n" + "*1\r\n" + "99999997\r\n";


		String expectedOutput =
			"+set_proxy_buf_size\r\n" + "+*1\r\n" + "-[NOT_VALID]9999999[...]\r\n" + "+end\r\n";


		inputBuffer = ByteBuffer.wrap(protocolInput.getBytes("ASCII"));

		try {
			parser.parse(inputBuffer, outputBuffer);
		} catch (ParserFormatException e) {
		}
		assertEquals(expectedOutput,
			new String(outputBuffer.array(), 0, outputBuffer.position(), PROPERTIES.getCharset()));

	}

	@Test
	public void testNotValidMethodLFNotPresent () throws UnsupportedEncodingException {

		String protocolInput =
			"lala8\r\n" + "method_count\r\n" + "*3\r\n" + "GE\r\n" + "GET\r\n" + "POS\rT\n"
				+ "end\r\n";


		String expectedOutput =
			"-[NO_MATCH]lala8\r\n" + "+method_count\r\n" + "+*3\r\n" + "-[NO_MATCH]GE\r\n"
				+ "+GET: 0\r\n" + "-[NOT_VALID]POS\rT[...]\r\n" + "+end\r\n";


		inputBuffer = ByteBuffer.wrap(protocolInput.getBytes("ASCII"));

		try {
			parser.parse(inputBuffer, outputBuffer);
		} catch (ParserFormatException e) {
		}
		assertEquals(expectedOutput,
			new String(outputBuffer.array(), 0, outputBuffer.position(), PROPERTIES.getCharset()));

	}

	@Test
	public void testNotValidHeaderLFNotPresent () throws UnsupportedEncodingException {

		String protocolInput = "lala8\r\n" + "server_bytes_written\rLEL\n";


		String expectedOutput =
			"-[NO_MATCH]lala8\r\n" + "-[NOT_VALID]server_bytes_written\rL[...]\r\n" + "+end\r\n";


		inputBuffer = ByteBuffer.wrap(protocolInput.getBytes("ASCII"));

		try {
			parser.parse(inputBuffer, outputBuffer);
		} catch (ParserFormatException e) {
		}
		assertEquals(expectedOutput,
			new String(outputBuffer.array(), 0, outputBuffer.position(), PROPERTIES.getCharset()));

	}

	@Test
	public void testNotValidStatusCodeLFNotPresent () throws UnsupportedEncodingException {

		String protocolInput =
			"lala8\r\n" + "status_code_count\r\n" + "*3\r\n" + "404\r\n" + "200\rUFF\n" + "302\rT\n"
				+ "end\r\n";


		String expectedOutput =
			"-[NO_MATCH]lala8\r\n" + "+status_code_count\r\n" + "+*3\r\n" + "+404: 0\r\n"
				+ "-[NOT_VALID]200\rU[...]\r\n" + "+end\r\n";


		inputBuffer = ByteBuffer.wrap(protocolInput.getBytes("ASCII"));

		try {
			parser.parse(inputBuffer, outputBuffer);
		} catch (ParserFormatException e) {
		}
		assertEquals(expectedOutput,
			new String(outputBuffer.array(), 0, outputBuffer.position(), PROPERTIES.getCharset()));

	}

	@Test
	public void testNotValidArgumentNumberLFNotPresent () throws UnsupportedEncodingException {

		String protocolInput =
			"lala8\r\n" + "method_count\r\n" + "*3\rSC\n" + "GE\r\n" + "GET\r\n" + "POST\n"
				+ "end\r\n";


		String expectedOutput =
			"-[NO_MATCH]lala8\r\n" + "+method_count\r\n" + "-[NOT_VALID]*3\rS[...]\r\n"
				+ "+end\r\n";


		inputBuffer = ByteBuffer.wrap(protocolInput.getBytes("ASCII"));

		try {
			parser.parse(inputBuffer, outputBuffer);
		} catch (ParserFormatException e) {
		}
		assertEquals(expectedOutput,
			new String(outputBuffer.array(), 0, outputBuffer.position(), PROPERTIES.getCharset()));

	}

	@Test
	public void testNotValidHeaderisEmpty () throws UnsupportedEncodingException {

		String protocolInput = "lala8\r\n" + "\r\n" + "client_bytes_written\r\n";


		String expectedOutput = "-[NO_MATCH]lala8\r\n" + "-[NOT_VALID][...]\r\n" + "+end\r\n";


		inputBuffer = ByteBuffer.wrap(protocolInput.getBytes("ASCII"));

		try {
			parser.parse(inputBuffer, outputBuffer);
		} catch (ParserFormatException e) {
		}
		assertEquals(expectedOutput,
			new String(outputBuffer.array(), 0, outputBuffer.position(), PROPERTIES.getCharset()));

	}

	@Test
	public void testNotValidMethodisEmpty () throws UnsupportedEncodingException {

		String protocolInput =
			"lala8\r\n" + "method_count\r\n" + "*3\r\n" + "\r\n" + "GET\rUFF\n" + "POST\rT\n"
				+ "end\r\n";


		String expectedOutput =
			"-[NO_MATCH]lala8\r\n" + "+method_count\r\n" + "+*3\r\n" + "-[NOT_VALID][...]\r\n"
				+ "+end\r\n";


		inputBuffer = ByteBuffer.wrap(protocolInput.getBytes("ASCII"));

		try {
			parser.parse(inputBuffer, outputBuffer);
		} catch (ParserFormatException e) {
		}
		assertEquals(expectedOutput,
			new String(outputBuffer.array(), 0, outputBuffer.position(), PROPERTIES.getCharset()));

	}

	@Test
	public void testNotValidStatusCodeisEmpty () throws UnsupportedEncodingException {

		String protocolInput =
			"lala8\r\n" + "status_code_count\r\n" + "*3\r\n" + "\r\n" + "200\r\n" + "302\rT\n"
				+ "end\r\n";


		String expectedOutput =
			"-[NO_MATCH]lala8\r\n" + "+status_code_count\r\n" + "+*3\r\n" + "-[NOT_VALID][...]\r\n"
				+ "+end\r\n";


		inputBuffer = ByteBuffer.wrap(protocolInput.getBytes("ASCII"));

		try {
			parser.parse(inputBuffer, outputBuffer);
		} catch (ParserFormatException e) {
		}
		assertEquals(expectedOutput,
			new String(outputBuffer.array(), 0, outputBuffer.position(), PROPERTIES.getCharset()));

	}

	@Test
	public void testNotValidStatusCodeisEmptyBis () throws UnsupportedEncodingException {

		String protocolInput = "lala8\r\n" + "status_code_count\r\n" + "*3\r\n"
			+ "00000000000000000000000000000000000000000000000000000000000000000000000000000000\r\n"
			+ "200\r\n" + "302\rT\n" + "end\r\n";


		String expectedOutput =
			"-[NO_MATCH]lala8\r\n" + "+status_code_count\r\n" + "+*3\r\n" + "-[NOT_VALID][...]\r\n"
				+ "+end\r\n";


		inputBuffer = ByteBuffer.wrap(protocolInput.getBytes("ASCII"));

		try {
			parser.parse(inputBuffer, outputBuffer);
		} catch (ParserFormatException e) {
		}
		assertEquals(expectedOutput,
			new String(outputBuffer.array(), 0, outputBuffer.position(), PROPERTIES.getCharset()));

	}

	@Test
	public void testNotValidArgSizeisEmpty () throws UnsupportedEncodingException {

		String protocolInput =
			"lala8\r\n" + "status_code_count\r\n" + "*\r\n" + "200\r\n" + "302\rT\n" + "end\r\n";


		String expectedOutput =
			"-[NO_MATCH]lala8\r\n" + "+status_code_count\r\n" + "-[NOT_VALID]*[...]\r\n"
				+ "+end\r\n";


		inputBuffer = ByteBuffer.wrap(protocolInput.getBytes("ASCII"));

		try {
			parser.parse(inputBuffer, outputBuffer);
		} catch (ParserFormatException e) {
		}
		assertEquals(expectedOutput,
			new String(outputBuffer.array(), 0, outputBuffer.position(), PROPERTIES.getCharset()));

	}
}
