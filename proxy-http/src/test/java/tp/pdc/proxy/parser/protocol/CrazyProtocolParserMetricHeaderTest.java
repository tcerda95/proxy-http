package tp.pdc.proxy.parser.protocol;

import static org.junit.Assert.*;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

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

public class CrazyProtocolParserMetricHeaderTest {
	
	private static ProxyProperties PROPERTIES = ProxyProperties.getInstance();
	
	private CrazyProtocolParser parser;
	private ByteBuffer inputBuffer;
	private ByteBuffer outputBuffer;
	
	private ServerMetric serverMetric;
	private ClientMetric clientMetric;
	
	@Before
	public void setUp() throws Exception {
		serverMetric = new ServerMetricStub();
		clientMetric = new ClientMetricStub();
		parser = new CrazyProtocolParserImpl(clientMetric, serverMetric);
		outputBuffer = ByteBuffer.allocate(4000);
	}
	
	@Test
	public void testMetricsHeader() throws ParserFormatException, UnsupportedEncodingException {
		
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
				"server_bytes_read\r\n"
				+ "METricS\r\n"
				+ "EnD\r\n";
		
		String expectedOutput =
				"+server_bytes_read: 2\r\n"
				+ "+metrics\r\n"
				+ "+isl33tenable: NO\r\n"
				+ "+client_bytes_read: 4\r\n"
				+ "+client_bytes_written: 5\r\n"
				+ "+client_connections: 1\r\n"
				+ "+server_bytes_read: 2\r\n"
				+ "+server_bytes_written: 3\r\n"
				+ "+server_connections: 1\r\n"
				+ "+method_count\r\n"
				+ "+GET: 2\r\n"
				+ "+POST: 1\r\n"
				+ "+HEAD: 0\r\n"
				+ "+OPTIONS: 0\r\n"
				+ "+PUT: 0\r\n"
				+ "+DELETE: 0\r\n"
				+ "+TRACE: 0\r\n"
				+ "+CONNECT: 0\r\n"
				+ "+status_code_count\r\n"
				+ "+404: 3\r\n"
				+ "+302: 1\r\n"
				+ "+end\r\n";
		
		inputBuffer = ByteBuffer.wrap(protocolInput.getBytes("ASCII"));
		
		parser.parse(inputBuffer, outputBuffer);
		assertEquals(expectedOutput, new String(outputBuffer.array(), 0, outputBuffer.position(), PROPERTIES.getCharset()));
		assertTrue(parser.hasFinished());
	}
}
