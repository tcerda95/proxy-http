package tp.pdc.proxy.parser.compressors;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.junit.Test;

import tp.pdc.proxy.ProxyProperties;
import tp.pdc.proxy.exceptions.ParserFormatException;

public class GzipTest {
	
	private static ProxyProperties PROPERTIES = ProxyProperties.getInstance();
	
	private ByteBuffer inputBuffer;

	@Test
	public void testCompressandDecompress() throws ParserFormatException, IOException {
		
		String stringtoEncode = "hola como te va?";
		
		inputBuffer = ByteBuffer.wrap(stringtoEncode.getBytes("ASCII"));

        byte[] compressed = Gzip.compress(inputBuffer);

        inputBuffer = ByteBuffer.wrap(compressed);
        
        byte[] decompressed = Gzip.decompress(inputBuffer);
        
		assertEquals(stringtoEncode, new String(decompressed, PROPERTIES.getCharset()));
	}
}
