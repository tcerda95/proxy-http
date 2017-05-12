package tp.pdc.proxy.parser.protocol;

import static org.junit.Assert.*;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import org.junit.Before;
import org.junit.Test;

import tp.pdc.proxy.ProxyProperties;
import tp.pdc.proxy.exceptions.ParserFormatException;
import tp.pdc.proxy.parser.interfaces.CrazyProtocolParser;

public class CrazyProtocolParserComplexHeadersTest {
	
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
				"server_bytes_read\r\n"
				+ "status_CoDe_count\r\n"
				+ "*3\r\n"
				+ "404\r\n"
				+ "500\r\n"
				+ "404\r\n"
				+ "status_code_count\r\n"
				+ "*3\r\n"
				+ "499\r\n"
				+ "500\r\n"
				+ "300\r\n"
				+ "server_bytes_read\r\n"
				+ "method_count\r\n"
				+ "*3\r\n"
				+ "GET\r\n"
				+ "POST\r\n"
				+ "GET\r\n"
				+ "method_count\r\n"
				+ "*1\r\n"
				+ "PosT\r\n"
				+ "ISL33TenaBle\r\n"
				+ "isl33tenable\r\n"
				+ "enD\r\n";
		
		String expectedOutput = 
				"+server_bytes_read: 0\r\n"
				+ "+status_code_count\r\n"
				+ "+404: 0\r\n"
				+ "+500: 0\r\n"
				+ "+404: 0\r\n"
				+ "+status_code_count\r\n"
				+ "+499: 0\r\n"
				+ "+500: 0\r\n"
				+ "+300: 0\r\n"
				+ "+server_bytes_read: 0\r\n"
				+ "+method_count\r\n"
				+ "+GET: 0\r\n"
				+ "+POST: 0\r\n"
				+ "+GET: 0\r\n"
				+ "+method_count\r\n"
				+ "+POST: 0\r\n"
				+ "+isl33tenable: NO\r\n"
				+ "+isl33tenable: NO\r\n"
				+ "+end\r\n";
		
		inputBuffer = ByteBuffer.wrap(protocolInput.getBytes("ASCII"));
		
		parser.parse(inputBuffer, outputBuffer);
		assertEquals(expectedOutput, new String(outputBuffer.array(), 0, outputBuffer.position(), PROPERTIES.getCharset()));
		assertTrue(parser.hasFinished());
	}
	
	@Test
	public void testNotFinished() throws ParserFormatException, UnsupportedEncodingException {
		String protocolInput =  
				"server_bytes_read\r\n"
				+ "method_count\r\n"
				+ "*3\r\n"
				+ "GET\r\n"
				+ "POST\r\n"
				+ "GET\r\n";
		
		inputBuffer = ByteBuffer.wrap(protocolInput.getBytes("ASCII"));
		
		parser.parse(inputBuffer, outputBuffer);
		assertFalse(parser.hasFinished());
	}
}
