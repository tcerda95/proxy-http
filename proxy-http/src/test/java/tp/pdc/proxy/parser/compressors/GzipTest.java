package tp.pdc.proxy.parser.compressors;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.junit.Before;
import org.junit.Test;

import tp.pdc.proxy.ProxyProperties;
import tp.pdc.proxy.exceptions.ParserFormatException;

public class GzipTest {
	
	private static ProxyProperties PROPERTIES = ProxyProperties.getInstance();
	
	private Gzip gzip;
	private ByteBuffer inputBuffer;
	
	@Before
	public void setUp() throws Exception {
		gzip = new Gzip();
	}

	@Test
	public void testCompressandDecompress() throws ParserFormatException, IOException {
		
		String stringtoEncode = "hola como te va?";
		
		inputBuffer = ByteBuffer.wrap(stringtoEncode.getBytes("ASCII"));

        byte[] compressed = gzip.compress(inputBuffer);

        inputBuffer = ByteBuffer.wrap(compressed);
        
        byte[] decompressed = gzip.decompress(inputBuffer);
        
		assertEquals(stringtoEncode, new String(decompressed, PROPERTIES.getCharset()));
	}
}
