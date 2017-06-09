package tp.pdc.proxy.parser.protocol;

import org.junit.Before;
import org.junit.Test;

import tp.pdc.proxy.exceptions.ParserFormatException;
import tp.pdc.proxy.flag.L33tFlag;
import tp.pdc.proxy.metric.stub.ClientMetricStub;
import tp.pdc.proxy.metric.stub.ServerMetricStub;
import tp.pdc.proxy.parser.interfaces.CrazyProtocolParser;
import tp.pdc.proxy.properties.ProxyProperties;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CrazyProtocolParserBufferOverflowTest {

	private static ProxyProperties PROPERTIES = ProxyProperties.getInstance();

	private CrazyProtocolParser parser;
	private ByteBuffer inputBuffer;
	private ByteBuffer outputBuffer;
	private L33tFlag flag = L33tFlag.getInstance();

	@Before
	public void setUp () throws Exception {
		flag.unset();
		parser = new CrazyProtocolParserImpl(new ClientMetricStub(), new ServerMetricStub());
		outputBuffer = ByteBuffer.allocate(30);
	}

	@Test
	public void testFinished () throws ParserFormatException, UnsupportedEncodingException {
		String protocolInput =
			"server_bytes_read\r\n" + "IS_L33T_enaBled\r\n" + "is_l33t_enabled\r\n"
				+ "server_bytes_read\r\n" + "server_bytes_read\r\n" + "server_bytes_read\r\n"
				+ "method_count\r\n" + "*4\r\n" + "GES\r\n" + "GET\r\n" + "POST\r\n" + "PUT\r\n"
				+ "status_code_count\r\n" + "*4\r\n" + "404\r\n" + "410\r\n" + "302\r\n" + "404\r\n"
				+ "enD\r\n";

		String expectedOutput =
			"+server_bytes_read: 0\r\n" + "+is_l33t_enabled: NO\r\n" + "+is_l33t_enabled: NO\r\n"
				+ "+server_bytes_read: 0\r\n" + "+server_bytes_read: 0\r\n"
				+ "+server_bytes_read: 0\r\n" + "+method_count\r\n" + "+*4\r\n"
				+ "-[NO_MATCH]GES\r\n" + "+GET: 0\r\n" + "+POST: 0\r\n" + "+PUT: 0\r\n"
				+ "+status_code_count\r\n" + "+*4\r\n" + "+404: 0\r\n" + "+410: 0\r\n"
				+ "+302: 0\r\n" + "+404: 0\r\n" + "+end\r\n";

		inputBuffer = ByteBuffer.wrap(protocolInput.getBytes("ASCII"));

		String inputProcessed = "";

		while (!parser.parse(inputBuffer, outputBuffer)) {
			inputProcessed += new String(outputBuffer.array(), 0, outputBuffer.position(),
				PROPERTIES.getCharset());

			outputBuffer.clear();
		}

		parser.parse(inputBuffer, outputBuffer);
		assertEquals(expectedOutput,
			inputProcessed + new String(outputBuffer.array(), 0, outputBuffer.position(),
				PROPERTIES.getCharset()));
		assertTrue(parser.hasFinished());
	}
}
