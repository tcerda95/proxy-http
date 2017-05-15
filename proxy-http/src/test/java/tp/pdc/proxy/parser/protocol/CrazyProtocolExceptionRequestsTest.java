package tp.pdc.proxy.parser.protocol;

import static org.junit.Assert.assertEquals;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import org.junit.Before;
import org.junit.Test;

import tp.pdc.proxy.ProxyProperties;
import tp.pdc.proxy.exceptions.ParserFormatException;
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
				"client_bytes_writtenn\r\n"
				+ "comova??\r\n"
				+ "end\r\n";
				
		
		String expectedOutput =
				"-[TOO_LONG]client_bytes_written[...]\r\n"
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
				"lala8\r\n"
				+ "method_count\r\n"
				+ "*6\r\n"
				+ "GE\r\n"
				+ "GET\r\n"
				+ "\r\n"
				+ "oPTIONs\r\n"
				+ "OPCIONES\r\n"
				+ "GET\r\n"
				+ "end\r\n";
				
		
		String expectedOutput =
				"-[NO_MATCH]lala8\r\n"
				+ "+method_count\r\n"
				+ "-[NO_MATCH]GE\r\n"
				+ "+GET: 0\r\n"
				+ "-[NO_MATCH]\r\n"
				+ "+OPTIONS: 0\r\n"
				+ "-[TOO_LONG]OPCIONE[...]\r\n"
				+ "+end\r\n";
				
		
		inputBuffer = ByteBuffer.wrap(protocolInput.getBytes("ASCII"));
		
		try {
			parser.parse(inputBuffer, outputBuffer);
		} catch (ParserFormatException e) {
		}
		assertEquals(expectedOutput, new String(outputBuffer.array(), 0, outputBuffer.position(), PROPERTIES.getCharset()));

	}
	
	@Test
	public void testNotValidMethod() throws UnsupportedEncodingException {
		
		String protocolInput =  
				"lala8\r\n"
				+ "method_count\r\n"
				+ "*4\r\n"
				+ "GE\r\n"
				+ "GET\r\n"
				+ "\r\n"
				+ "GET\n\r"
				+ "end\r\n";
				
		
		String expectedOutput =
				"-[NO_MATCH]lala8\r\n"
				+ "+method_count\r\n"
				+ "-[NO_MATCH]GE\r\n"
				+ "+GET: 0\r\n"
				+ "-[NO_MATCH]\r\n"
				+ "-[NOT_VALID]GET[...]\r\n"
				+ "+end\r\n";
				
		
		inputBuffer = ByteBuffer.wrap(protocolInput.getBytes("ASCII"));
		
		try {
			parser.parse(inputBuffer, outputBuffer);
		} catch (ParserFormatException e) {
		}
		assertEquals(expectedOutput, new String(outputBuffer.array(), 0, outputBuffer.position(), PROPERTIES.getCharset()));

	}
	
	@Test
	public void testNotValidMethodBis() throws UnsupportedEncodingException {
		
		String protocolInput =  
				"lala8\r\n"
				+ "method_count\r\n"
				+ "*4\r\n"
				+ "GE\r\n"
				+ "GET\r\n"
				+ "\r\n"
				+ "POS\rT\n"
				+ "end\r\n";
				
		
		String expectedOutput =
				"-[NO_MATCH]lala8\r\n"
				+ "+method_count\r\n"
				+ "-[NO_MATCH]GE\r\n"
				+ "+GET: 0\r\n"
				+ "-[NO_MATCH]\r\n"
				+ "-[NOT_VALID]POS[...]\r\n"
				+ "+end\r\n";
				
		
		inputBuffer = ByteBuffer.wrap(protocolInput.getBytes("ASCII"));
		
		try {
			parser.parse(inputBuffer, outputBuffer);
		} catch (ParserFormatException e) {
		}
		assertEquals(expectedOutput, new String(outputBuffer.array(), 0, outputBuffer.position(), PROPERTIES.getCharset()));

	}
	
	@Test
	public void testTooLongStatusCount() throws UnsupportedEncodingException {
		
		String protocolInput =  
				"lala8\r\n"
				+ "status_code_count\r\n"
				+ "*6\r\n"
				+ "100\r\n"
				+ "101\r\n"
				+ "\r\n"
				+ "0\r\n"
				+ "404\r\n"
				+ "4040\r\n"
				+ "server_bytes_written\r\n"
				+ "end\r\n";
				
		
		String expectedOutput =
				"-[NO_MATCH]lala8\r\n"
				+ "+status_code_count\r\n"
				+ "+100: 0\r\n"
				+ "+101: 0\r\n"
				+ "-[NO_MATCH]\r\n"
				+ "-[NO_MATCH]\r\n"
				+ "+404: 0\r\n"
				+ "-[TOO_LONG]404[...]\r\n"
				+ "+end\r\n";
				
		
		inputBuffer = ByteBuffer.wrap(protocolInput.getBytes("ASCII"));
		
		try {
			parser.parse(inputBuffer, outputBuffer);
		} catch (ParserFormatException e) {
		}
		assertEquals(expectedOutput, new String(outputBuffer.array(), 0, outputBuffer.position(), PROPERTIES.getCharset()));

	}
	
	@Test
	public void testNotValidStatusCount() throws UnsupportedEncodingException {
		
		String protocolInput =  
				"lala8\r\n"
				+ "status_code_count\r\n"
				+ "*3\r\n"
				+ "100\r\n"
				+ "0000000000000404\r\n"
				+ "606\r\n"
				+ "end\r\n";
				
		
		String expectedOutput =
				"-[NO_MATCH]lala8\r\n"
				+ "+status_code_count\r\n"
				+ "+100: 0\r\n"
				+ "+404: 0\r\n"
				+ "-[NOT_VALID]6[...]\r\n"
				+ "+end\r\n";
				
		
		inputBuffer = ByteBuffer.wrap(protocolInput.getBytes("ASCII"));
		
		try {
			parser.parse(inputBuffer, outputBuffer);
		} catch (ParserFormatException e) {
		}
		assertEquals(expectedOutput, new String(outputBuffer.array(), 0, outputBuffer.position(), PROPERTIES.getCharset()));

	}
}