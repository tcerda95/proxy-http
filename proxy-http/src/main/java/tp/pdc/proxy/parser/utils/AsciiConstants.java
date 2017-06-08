package tp.pdc.proxy.parser.utils;

public enum AsciiConstants {
	HT(9),
	CR(13),
	LF(10),
	SP(32),
	AS(42),
	PS(43),
	LS(45),
	PT(46),
	DP(58),
	OB(91),
	CB(93),
	US(95);

	private byte value;

	private AsciiConstants (int value) {
		this.value = (byte) value;
	}

	public byte getValue () {
		return value;
	}
}
