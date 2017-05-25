package tp.pdc.proxy;

import java.nio.ByteBuffer;

import org.apache.commons.lang3.ArrayUtils;

import tp.pdc.proxy.header.BytesUtils;

public class CharsetParser {

	private static final ProxyProperties PROPERTIES = ProxyProperties.getInstance();
	
	private final static int MAX_PARAM_NAME = 50;
	private final static int MAX_PARAM_VALUE = 50;
	
	private final ByteBuffer valueBuffer = ByteBuffer.allocate(MAX_PARAM_VALUE);
	private final ByteBuffer nameBuffer = ByteBuffer.allocate(MAX_PARAM_NAME);
	private final byte[] charsetBytesArray = "charset".getBytes(PROPERTIES.getCharset());
	
	public byte[] extractCharset(final byte[] contentTypeHeader) {
		
		for (int i = 0; i < contentTypeHeader.length;) {
			nameBuffer.clear();
			i = BytesUtils.findValueIndex(contentTypeHeader, (byte) ';', 0);
			
			if (i == -1)
				return ArrayUtils.EMPTY_BYTE_ARRAY;
			
			i = skipWhiteSpaces(contentTypeHeader, i);
			
			i = saveParameterName(contentTypeHeader, i); // equals index
			
			i = skipWhiteSpaces(contentTypeHeader, i);
			
			nameBuffer.flip();
			if (BytesUtils.equalsBytes(charsetBytesArray, nameBuffer, charsetBytesArray.length))
				return extractCharsetValue(contentTypeHeader, i);			
		}
		
		return ArrayUtils.EMPTY_BYTE_ARRAY;
	}

	private int saveParameterName(final byte[] contentTypeHeader, final int semiColonIndex) {
		int i = semiColonIndex + 1;
		
		while (nameBuffer.hasRemaining() && i < contentTypeHeader.length && contentTypeHeader[i] != '=' && contentTypeHeader[i] != ' ')
			nameBuffer.put((byte) Character.toLowerCase(contentTypeHeader[i++]));
		
		while (i < contentTypeHeader.length && contentTypeHeader[i] != '=')
			i++;
		
		return i;
	}
	
	private int skipWhiteSpaces(final byte[] contentTypeHeader, int i) {
		while (i < contentTypeHeader.length && contentTypeHeader[i] == ' ')
			i++;
		return i;
	}
	
	private byte[] extractCharsetValue(final byte[] contentTypeHeader, final int equalsIndex) {
		int i = equalsIndex;
		
		while (valueBuffer.hasRemaining() && i < contentTypeHeader.length && contentTypeHeader[i] != ' ')
			valueBuffer.put((byte) Character.toLowerCase(contentTypeHeader[i++]));
		
		valueBuffer.flip();
		
		byte[] charset = new byte[valueBuffer.limit()];

		for (int j = 0; j < charset.length; j++)
			charset[j] = valueBuffer.get();
		
		valueBuffer.clear();
		
		return charset;
	}
}
