package tp.pdc.proxy.parser;

import org.apache.commons.lang3.ArrayUtils;

import tp.pdc.proxy.bytes.BytesUtils;
import tp.pdc.proxy.properties.ProxyProperties;

import java.nio.ByteBuffer;

public class CharsetParser {

	private static final ProxyProperties PROPERTIES = ProxyProperties.getInstance();

	private final static int MAX_PARAM_NAME = 50;
	private final static int MAX_PARAM_VALUE = 50;
	private static final byte[] CHARSET_BYTES = "charset".getBytes(PROPERTIES.getCharset());

	private final ByteBuffer valueBuffer = ByteBuffer.allocate(MAX_PARAM_VALUE);
	private final ByteBuffer nameBuffer = ByteBuffer.allocate(MAX_PARAM_NAME);

	public byte[] extractCharset (final byte[] contentTypeHeader) {

		for (int i = 0; i < contentTypeHeader.length; ) {
			nameBuffer.clear();
			i = BytesUtils.findValueIndex(contentTypeHeader, (byte) ';', i);

			if (i == -1)
				return ArrayUtils.EMPTY_BYTE_ARRAY;

			i = skipWhiteSpaces(contentTypeHeader, i + 1);

			i = saveParameterName(contentTypeHeader, i); // returns equals index

			i = skipWhiteSpaces(contentTypeHeader, i + 1);

			nameBuffer.flip();
			if (BytesUtils.equalsBytes(CHARSET_BYTES, nameBuffer, nameBuffer.remaining()))
				return extractCharsetValue(contentTypeHeader, i);
		}

		return ArrayUtils.EMPTY_BYTE_ARRAY;
	}

	private int saveParameterName (final byte[] contentTypeHeader, final int paramNameIndex) {
		int i = paramNameIndex;

		while (nameBuffer.hasRemaining() && i < contentTypeHeader.length
			&& contentTypeHeader[i] != '=' && contentTypeHeader[i] != ' ')
			nameBuffer.put((byte) Character.toLowerCase(contentTypeHeader[i++]));

		while (i < contentTypeHeader.length && contentTypeHeader[i] != '=')
			i++;

		return i;
	}

	private int skipWhiteSpaces (final byte[] contentTypeHeader, int i) {
		while (i < contentTypeHeader.length && contentTypeHeader[i] == ' ')
			i++;
		return i;
	}

	private byte[] extractCharsetValue (final byte[] contentTypeHeader, final int equalsIndex) {
		int i = equalsIndex;

		while (valueBuffer.hasRemaining() && i < contentTypeHeader.length
			&& contentTypeHeader[i] != ' ')
			valueBuffer.put((byte) Character.toLowerCase(contentTypeHeader[i++]));

		valueBuffer.flip();

		byte[] charset = new byte[valueBuffer.limit()];

		for (int j = 0; j < charset.length; j++)
			charset[j] = valueBuffer.get();

		valueBuffer.clear();

		return charset;
	}
}
