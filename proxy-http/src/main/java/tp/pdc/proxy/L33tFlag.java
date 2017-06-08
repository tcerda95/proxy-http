package tp.pdc.proxy;

/**
 * {@link L33tFlag} is in charge of keeping a track if the l33t translation is activated or not in the proxy,
 * as well as activating or desactivating the conversion.
 */
public class L33tFlag {

	private static final L33tFlag INSTANCE = new L33tFlag();

	private boolean l33tFlag;

	public static L33tFlag getInstance () {
		return INSTANCE;
	}

	public boolean isSet () {
		return l33tFlag;
	}

	public void set () {
		l33tFlag = true;
	}

	public void unset () {
		l33tFlag = false;
	}
}
