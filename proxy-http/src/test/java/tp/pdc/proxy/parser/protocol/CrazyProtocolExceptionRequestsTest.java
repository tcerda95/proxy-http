package tp.pdc.proxy.parser.protocol;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import org.junit.Before;
import org.junit.Test;

import tp.pdc.proxy.ProxyProperties;
import tp.pdc.proxy.exceptions.ParserFormatException;
import tp.pdc.proxy.header.Method;
import tp.pdc.proxy.parser.interfaces.CrazyProtocolParser;

public class CrazyProtocolExceptionRequestsTest {
	
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
	public void testTooLongHeader() throws UnsupportedEncodingException {
				
		String protocolInput =  
				"lalalalalalalalalalallalalalalalalalalalalalallalalalalalalalallalalalallalalalalalalal\r\n"
				+ "comova??\r\n"
				+ "end\r\n";
				
		
		String expectedOutput =
				"-[TOO_LONG]lalalalalalalalalalal[...]\r\n"
		+		"+end\r\n";
				
		
		inputBuffer = ByteBuffer.wrap(protocolInput.getBytes("ASCII"));
		
		try {
			parser.parse(inputBuffer, outputBuffer);
		} catch (ParserFormatException e) {
		}
		assertEquals(expectedOutput, new String(outputBuffer.array(), 0, outputBuffer.position(), PROPERTIES.getCharset()));
	}
	
	@Test
	public void testNotValidHeader() throws UnsupportedEncodingException {
		
		String protocolInput =  
				"lala8\r\n"
				+ "isValid¿?\r\n"
				+ "end\r\n";
				
		
		String expectedOutput =
				"-[NO_MATCH]lala8\r\n"
				+ "-[NOT_VALID]isvalid[...]\r\n"
				+ "+end\r\n";
				
		
		inputBuffer = ByteBuffer.wrap(protocolInput.getBytes("ASCII"));
		
		try {
			parser.parse(inputBuffer, outputBuffer);
		} catch (ParserFormatException e) {
		}
		assertEquals(expectedOutput, new String(outputBuffer.array(), 0, outputBuffer.position(), PROPERTIES.getCharset()));

	}
	
	@Test
	public void testNotValidBisHeader() throws UnsupportedEncodingException {
		
		String protocolInput =  
				"method_count\r\n"
				+ "*1\r\n"
				+ "GET\r\n"
				+ "lel?\r\n"
				+ "client_bytes_read\r\n";
				
		
		String expectedOutput =
				"+method_count\r\n"
				+ "+GET: 0\r\n"
				+ "-[NOT_VALID]lel[...]\r\n"
				+ "+end\r\n";
				
		
		inputBuffer = ByteBuffer.wrap(protocolInput.getBytes("ASCII"));
		
		try {
			parser.parse(inputBuffer, outputBuffer);
		} catch (ParserFormatException e) {
		}
		assertEquals(expectedOutput, new String(outputBuffer.array(), 0, outputBuffer.position(), PROPERTIES.getCharset()));

	}
	
	@Test
	public void testTooLongMethod() throws UnsupportedEncodingException {
				
		String protocolInput =  
				"lalala\r\n"
				+ "method_count\r\n"
				+ "*2\r\n"
				+ "LOLOLOLOLOLOL\r\n"
				+ "GEt\r\n"
				+ "end\r\n";
				
		
		String expectedOutput =
				"-[NO_MATCH]lalala\r\n"
				+ "+method_count\r\n"
				+ "-[TOO_LONG]LOLOLOL[...]\r\n"
		+		"+end\r\n";
				
		
		inputBuffer = ByteBuffer.wrap(protocolInput.getBytes("ASCII"));
		
		try {
			parser.parse(inputBuffer, outputBuffer);
		} catch (ParserFormatException e) {
		}
		assertEquals(expectedOutput, new String(outputBuffer.array(), 0, outputBuffer.position(), PROPERTIES.getCharset()));
	}
}