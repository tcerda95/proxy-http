package tp.pdc.proxy.parser.protocol;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import org.junit.Before;
import org.junit.Test;

import tp.pdc.proxy.ProxyProperties;
import tp.pdc.proxy.exceptions.ParserFormatException;
import tp.pdc.proxy.metric.stub.ClientMetricStub;
import tp.pdc.proxy.metric.stub.ServerMetricStub;
import tp.pdc.proxy.parser.interfaces.CrazyProtocolParser;

public class CrazyProtocolParserBufferOverflowTest {
	
	private static ProxyProperties PROPERTIES = ProxyProperties.getInstance();
	
	private CrazyProtocolParser parser;
	private ByteBuffer inputBuffer;
	private ByteBuffer outputBuffer;
	
	@Before
	public void setUp() throws Exception {
		parser = new CrazyProtocolParserImpl(new ClientMetricStub(), new ServerMetricStub());
		outputBuffer = ByteBuffer.allocate(20);
	}

	@Test
	public void testFinished() throws ParserFormatException, UnsupportedEncodingException {
		String protocolInput =
				"server_bytes_read\r\n"
				+ "ISL33TenaBle\r\n"
				+ "isl33tenable\r\n"
				+ "enD\r\n";
		
		String expectedOutput = 
				"+server_bytes_read: 0\r\n"
				+ "+isl33tenable: NO\r\n"
				+ "+isl33tenable: NO\r\n"
				+ "+end\r\n";
		
		inputBuffer = ByteBuffer.wrap(protocolInput.getBytes("ASCII"));
		
		String inputProcessed = "";
		
		while (!parser.parse(inputBuffer, outputBuffer))
		{	
			inputProcessed += new String(outputBuffer.array(), 0, outputBuffer.position(), PROPERTIES.getCharset());
			
			outputBuffer.clear();
		
		}
	
		parser.parse(inputBuffer, outputBuffer);
		assertEquals(expectedOutput, inputProcessed + new String(outputBuffer.array(), 0, outputBuffer.position(), PROPERTIES.getCharset()));
		assertTrue(parser.hasFinished());
	}
}
