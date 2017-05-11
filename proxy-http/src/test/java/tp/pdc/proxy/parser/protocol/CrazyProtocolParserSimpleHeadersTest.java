package tp.pdc.proxy.parser.protocol;

import static org.junit.Assert.*;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import org.junit.Before;
import org.junit.Test;

import tp.pdc.proxy.ProxyProperties;
import tp.pdc.proxy.exceptions.ParserFormatException;
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
		String protocolInput =  "l33tenable\r\n"
				+ "l33tEnaBle\r\n"
				+ "CLIENT_bytes_transferred\r\n"
				+ "ENd\r\n";
		
		inputBuffer = ByteBuffer.wrap(protocolInput.getBytes("ASCII"));
		
		parser.parse(inputBuffer, outputBuffer);
		assertTrue(parser.hasFinished());
	}
	
	@Test
	public void testNotFinished() throws ParserFormatException, UnsupportedEncodingException {
		String protocolInput =  "l33tenable\r\n"
				+ "l33tEnaBle\r\n"
				+ "CLIENT_bytes_transferred\r\n";
		
		inputBuffer = ByteBuffer.wrap(protocolInput.getBytes("ASCII"));
		
		parser.parse(inputBuffer, outputBuffer);
		assertFalse(parser.hasFinished());
	}
	
	@Test
	public void testNotFinishedBis() throws ParserFormatException, UnsupportedEncodingException {
		String protocolInput =  "l33tenable\r\n"
				+ "l33tEnaBle\r\n"
				+ "CLIENT_bytes_transferred\r\n"
				+ "eNd";
		
		inputBuffer = ByteBuffer.wrap(protocolInput.getBytes("ASCII"));
		
		parser.parse(inputBuffer, outputBuffer);
		assertFalse(parser.hasFinished());
	}

}
