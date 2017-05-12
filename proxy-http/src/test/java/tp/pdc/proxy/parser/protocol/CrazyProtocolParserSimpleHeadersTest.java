package tp.pdc.proxy.parser.protocol;

import static org.junit.Assert.*;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import org.junit.Before;
import org.junit.Test;

import tp.pdc.proxy.ProxyProperties;
import tp.pdc.proxy.exceptions.ParserFormatException;
import tp.pdc.proxy.metric.ClientMetricImpl;
import tp.pdc.proxy.metric.ServerMetricImpl;
import tp.pdc.proxy.parser.interfaces.CrazyProtocolParser;

public class CrazyProtocolParserSimpleHeadersTest {
	
	private static ProxyProperties PROPERTIES = ProxyProperties.getInstance();
	
	private CrazyProtocolParser parser;
	private ByteBuffer inputBuffer;
	private ByteBuffer outputBuffer;
	
	@Before
	public void setUp() throws Exception {
		parser = new CrazyProtocolParserImpl();
		outputBuffer = ByteBuffer.allocate(4000);
	}

	@Test
	public void testFinished() throws ParserFormatException, UnsupportedEncodingException {
		String protocolInput =  
				"l33tenable\r\n"
				+ "l33tEnaBle\r\n"
				+ "server_bytes_read\r\n"
				+ "server_bytes_read\r\n"
				+ "client_bytes_written\r\n"
				+ "server_bytes_read\r\n"
				+ "client_bytes_written\r\n"
				+ "server_bytes_written\r\n"
				+ "ENd\r\n";
		
		String expectedOutput =
				"+l33tenable\r\n"
				+ "+l33tenable\r\n"
				+ "+server_bytes_read: 1\r\n"
				+ "+repeated\r\n"
				+ "+client_bytes_written: 2\r\n"
				+ "+repeated\r\n"
				+ "+repeated\r\n"
				+ "+server_bytes_written: 0\r\n"
				+ "+end\r\n";
		
		inputBuffer = ByteBuffer.wrap(protocolInput.getBytes("ASCII"));
		
		ServerMetricImpl.getInstance().addBytesRead(1);
		ClientMetricImpl.getInstance().addBytesWritten(2);
		
		parser.parse(inputBuffer, outputBuffer);
		assertEquals(expectedOutput, new String(outputBuffer.array(), 0, outputBuffer.position(), PROPERTIES.getCharset()));
		assertTrue(parser.hasFinished());
	}
	
	@Test
	public void testNotFinished() throws ParserFormatException, UnsupportedEncodingException {
		String protocolInput =  
				"l33tenable\r\n"
				+ "l33tEnaBle\r\n"
				+ "server_bytes_read\r\n";
		
		inputBuffer = ByteBuffer.wrap(protocolInput.getBytes("ASCII"));
		
		parser.parse(inputBuffer, outputBuffer);
		assertFalse(parser.hasFinished());
	}
	
	@Test
	public void testNotFinishedBis() throws ParserFormatException, UnsupportedEncodingException {
		String protocolInput =  
				"l33tenable\r\n"
				+ "l33tEnaBle\r\n"
				+ "server_bytes_read\r\n"
				+ "eNd";
		
		inputBuffer = ByteBuffer.wrap(protocolInput.getBytes("ASCII"));
		
		parser.parse(inputBuffer, outputBuffer);
		assertFalse(parser.hasFinished());
	}

}
