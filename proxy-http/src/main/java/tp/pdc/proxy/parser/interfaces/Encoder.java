package tp.pdc.proxy.parser.interfaces;

/**
 * Encodes or decodes a single byte
 */
public interface Encoder {

	/**
	 * Encodes a single byte
	 * @param c byte to encode
	 * @return encoded byte
     */
	static byte encodeByte (byte c) {
		return c;
	}

	/**
	 * Decodes a single byte
	 * @param c byte to decode
     * @return decoded byte
     */
	static byte decodeByte (byte c) {
		return c;
	}
}
