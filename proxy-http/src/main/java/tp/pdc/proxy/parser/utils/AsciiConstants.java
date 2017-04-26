package tp.pdc.proxy.parser.utils;

public enum AsciiConstants {
	CR(13), 
	LF(10), 
	SP(32), 
	HT(9);
	
	private byte value;
	
	private AsciiConstants(int value) {
		this.value = (byte) value;
	}
	
	public byte getValue() {
		return value;
	}
}
