package tp.pdc.proxy.parser.utils;

public enum DecimalConstants {

	DECIMAL_BASE_VALUE(10),
	HEXA_BASE_VALUE(16), 
	A_DECIMAL_VALUE(10);
	
	private byte value;
	
	private DecimalConstants(int value) {
		this.value = (byte) value;
	}
	
	public byte getValue() {
		return value;
	}
}
