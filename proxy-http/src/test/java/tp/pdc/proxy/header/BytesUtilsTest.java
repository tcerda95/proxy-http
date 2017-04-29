package tp.pdc.proxy.header;

import static org.junit.Assert.*;

import java.nio.ByteBuffer;

import org.junit.Before;
import org.junit.Test;

import tp.pdc.proxy.ProxyProperties;

public class BytesUtilsTest {

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void lengthPut() {
		ByteBuffer outputBuffer = ByteBuffer.allocate(50);
		ByteBuffer inputBuffer = ByteBuffer.wrap("hola como estas hoy?".getBytes(ProxyProperties.getInstance().getCharset()));
		
		BytesUtils.lengthPut(inputBuffer, outputBuffer, 4);
		outputBuffer.flip();
		assertEquals("hola", new String(outputBuffer.array(), outputBuffer.position(), outputBuffer.remaining(), ProxyProperties.getInstance().getCharset()));
	}
	
	@Test
	public void testEqualsBytesArraysNoLength() {
		byte arr1[] = new byte[]{'h', 'o', 'l', 'a'};
		byte arr2[] = new byte[]{'h', 'o', 'l', 'a'};
		
		assertTrue(BytesUtils.equalsBytes(arr1, arr2));
	}

	@Test
	public void testNotEqualsBytesArraysNoLength() {
		byte arr1[] = new byte[]{'h', 'o', 'l', 'a'};
		byte arr2[] = new byte[]{'h', 'o', 'l'};
		byte arr3[] = new byte[]{'c', 'h', 'a', 'u'};
		
		assertFalse(BytesUtils.equalsBytes(arr1, arr2));
		assertFalse(BytesUtils.equalsBytes(arr1, arr3));
		assertFalse(BytesUtils.equalsBytes(arr2, arr3));
	}
}
