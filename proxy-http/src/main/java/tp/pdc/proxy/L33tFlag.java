package tp.pdc.proxy;

public class L33tFlag {

	private static final L33tFlag INSTANCE = new L33tFlag();
	
	private boolean l33tFlag;
	
	public static L33tFlag getInstance() {
		return INSTANCE;
	}
	
	public boolean isSet() {
		return l33tFlag;
	}
	
	public void set() {
		l33tFlag = true;
	}
	
	public void unset() {
		l33tFlag = false;
	}
}
