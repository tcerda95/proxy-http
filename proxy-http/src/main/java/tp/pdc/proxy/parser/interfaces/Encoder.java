package tp.pdc.proxy.parser.interfaces;

public interface Encoder {
	static byte encodeByte (byte c) {
		return c;
	}

	static byte decodeByte (byte c) {
		return c;
	}
}
