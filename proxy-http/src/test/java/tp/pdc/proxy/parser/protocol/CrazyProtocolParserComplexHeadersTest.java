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
				+ "404\r\n"
				+ "503\r\n"
				+ "server_bytes_read\r\n"
				+ "l33tEnaBle\r\n"
				+ "method_count\r\n"
				+ "*3\r\n"
				+ "GET\r\n"
				+ "POST\r\n"
				+ "GET\r\n"
				+ "method_count\r\n"
				+ "*3\r\n"
				+ "GET\r\n"
				+ "POST\r\n"
				+ "GET\r\n"
				+ "status_CoDe_count\r\n"
				+ "*1\r\n"
				+ "404\r\n"
				+ "status_CoDe_count\r\n"
				+ "*10\r\n"
				+ "404\r\n"
				+ "303\r\n"
				+ "200\r\n"
				+ "599\r\n"
				+ "401\r\n"
				+ "404\r\n"
				+ "303\r\n"
				+ "200\r\n"
				+ "599\r\n"
				+ "401\r\n"
				+ "client_bytes_read\r\n"
				+ "EnD\r\n";
		
		inputBuffer = ByteBuffer.wrap(protocolInput.getBytes("ASCII"));
		
		parser.parse(inputBuffer, outputBuffer);
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
