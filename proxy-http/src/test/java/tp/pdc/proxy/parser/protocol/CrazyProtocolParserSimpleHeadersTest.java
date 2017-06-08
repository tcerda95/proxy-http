package tp.pdc.proxy.parser.protocol;

import org.junit.Before;
import org.junit.Test;
import tp.pdc.proxy.ProxyProperties;
import tp.pdc.proxy.exceptions.ParserFormatException;
import tp.pdc.proxy.header.Method;
import tp.pdc.proxy.metric.interfaces.ClientMetric;
import tp.pdc.proxy.metric.interfaces.ServerMetric;
import tp.pdc.proxy.metric.stub.ClientMetricStub;
import tp.pdc.proxy.metric.stub.ServerMetricStub;
import tp.pdc.proxy.parser.interfaces.CrazyProtocolParser;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import static org.junit.Assert.*;

public class CrazyProtocolParserSimpleHeadersTest {

	private static ProxyProperties PROPERTIES = ProxyProperties.getInstance();

	private CrazyProtocolParser parser;
	private ByteBuffer inputBuffer;
	private ByteBuffer outputBuffer;

	private ServerMetric serverMetric;
	private ClientMetric clientMetric;

	@Before
	public void setUp () throws Exception {
		clientMetric = new ClientMetricStub();
		serverMetric = new ServerMetricStub();
		outputBuffer = ByteBuffer.allocate(4000);
		parser = new CrazyProtocolParserImpl(clientMetric, serverMetric);
	}

	@Test
	public void testPINGPONG () throws UnsupportedEncodingException, ParserFormatException {



		String protocolInput = "piNG\r\n" + "ENd\r\n";

		String expectedOutput = "+PONG\r\n" + "+end\r\n";

		inputBuffer = ByteBuffer.wrap(protocolInput.getBytes("ASCII"));

		parser.parse(inputBuffer, outputBuffer);
		assertEquals(expectedOutput,
			new String(outputBuffer.array(), 0, outputBuffer.position(), PROPERTIES.getCharset()));
		assertTrue(parser.hasFinished());
	}

	@Test
	public void testFinished () throws UnsupportedEncodingException, ParserFormatException {

		serverMetric.addConnection();
		serverMetric.addBytesRead(2);
		serverMetric.addBytesWritten(3);
		serverMetric.addResponseCodeCount(404);
		serverMetric.addResponseCodeCount(404);
		serverMetric.addResponseCodeCount(404);
		serverMetric.addResponseCodeCount(302);

		clientMetric.addConnection();
		clientMetric.addBytesRead(4);
		clientMetric.addBytesWritten(5);
		clientMetric.addMethodCount(Method.GET);
		clientMetric.addMethodCount(Method.GET);
		clientMetric.addMethodCount(Method.POST);

		String protocolInput =
			"asada\r\n" + "l33t_EnaBle\r\n" + "server_bytes_read\r\n" + "server_bytes_written\r\n"
				+ "server_connections\r\n" + "client_bytes_read\r\n" + "client_bytes_written\r\n"
				+ "client_connections\r\n" + "method_count\r\n" + "*4\r\n" + "GES\r\n" + "GET\r\n"
				+ "POST\r\n" + "PUT\r\n" + "status_code_count\r\n" + "*4\r\n" + "404\r\n"
				+ "410\r\n" + "302\r\n" + "404\r\n" + "set_proxy_BUF_size\r\n" + "*1\r\n"
				+ "1000\r\n" + "ENd\r\n";

		String expectedOutput =
			"-[NO_MATCH]asada\r\n" + "+l33t_enable\r\n" + "+server_bytes_read: 2\r\n"
				+ "+server_bytes_written: 3\r\n" + "+server_connections: 1\r\n"
				+ "+client_bytes_read: 4\r\n" + "+client_bytes_written: 5\r\n"
				+ "+client_connections: 1\r\n" + "+method_count\r\n" + "+*4\r\n"
				+ "-[NO_MATCH]GES\r\n" + "+GET: 2\r\n" + "+POST: 1\r\n" + "+PUT: 0\r\n"
				+ "+status_code_count\r\n" + "+*4\r\n" + "+404: 3\r\n" + "+410: 0\r\n"
				+ "+302: 1\r\n" + "+404: 3\r\n" + "+set_proxy_buf_size\r\n" + "+*1\r\n"
				+ "+1000\r\n" + "+end\r\n";

		inputBuffer = ByteBuffer.wrap(protocolInput.getBytes("ASCII"));

		parser.parse(inputBuffer, outputBuffer);
		assertEquals(expectedOutput,
			new String(outputBuffer.array(), 0, outputBuffer.position(), PROPERTIES.getCharset()));
		assertTrue(parser.hasFinished());
	}

	@Test
	public void testNotFinished () throws ParserFormatException, UnsupportedEncodingException {
		String protocolInput = "l33tenable\r\n" + "l33tEnaBle\r\n" + "server_bytes_read\r\n";

		inputBuffer = ByteBuffer.wrap(protocolInput.getBytes("ASCII"));

		parser.parse(inputBuffer, outputBuffer);
		assertFalse(parser.hasFinished());
	}

	@Test
	public void testNotFinishedBis () throws ParserFormatException, UnsupportedEncodingException {
		String protocolInput =
			"l33tenable\r\n" + "l33tEnaBle\r\n" + "server_bytes_read\r\n" + "eNd";

		inputBuffer = ByteBuffer.wrap(protocolInput.getBytes("ASCII"));

		parser.parse(inputBuffer, outputBuffer);
		assertFalse(parser.hasFinished());
	}

}
