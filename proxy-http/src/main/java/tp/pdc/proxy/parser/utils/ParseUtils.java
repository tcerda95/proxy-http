package tp.pdc.proxy.parser.utils;

import static tp.pdc.proxy.parser.utils.AsciiConstants.*;

//TODO: tests
public class ParseUtils {
    private static final int US_ASCII_LENGTH = 128;

    private static boolean[] isToken, isAlphabetic, isSeparator, isLWS, isDigit;

    private static final byte[] separator = {'(', ')', '<', '>', '@', ',', ';', ':', '\\',
        '"', '/', '[', ']', '?', '=', '{', '}', SP.getValue(), HT.getValue()};

    // Cargo tablas estáticas para hacer los chequeos más rápido.
    static {
        isToken = new boolean[US_ASCII_LENGTH]; isSeparator = new boolean[US_ASCII_LENGTH];
        isLWS = new boolean[US_ASCII_LENGTH]; isDigit = new boolean[US_ASCII_LENGTH];
        isAlphabetic = new boolean[US_ASCII_LENGTH];

        for (int c = 0; c < US_ASCII_LENGTH; c++) {
            isSeparator[c] = contains(separator, (byte) c);
            isToken[c] = (31 < c && c < 127 && !isSeparator[c]);
            isLWS[c] = (c == CR.getValue() || c == LF.getValue() || c == SP.getValue() || c == HT.getValue());
            isDigit[c] = ('0' <= c && c <= '9');
            isAlphabetic[c] = ('a' <= c && c <= 'z') || ('A' <= c && c <= 'Z');
        }
    }

    private ParseUtils() {
    }
    
    private static boolean contains(byte[] arr, byte toFind) {
        for (byte c: arr) {
            if (c == toFind)
                return true;
        }
        return false;
    }

    public static boolean isHeaderNameChar (byte c) {
        return isToken[c];
    }

    public static boolean isHeaderContentChar (byte c) {
        return isToken[c] || isLWS[c] || isSeparator[c];
    }

    public static boolean isDigit (byte c) {
        return isDigit[c];
    }

    public static boolean isText (byte c) {
        return isToken[c] || isSeparator[c];
    }

    public static boolean isAlphabetic(byte c) {
        return isAlphabetic[c];
    }
    
	public static int parseInt(byte[] arr) {
		return parseInt(arr, 0, arr.length);
	}
	
	public static int parseInt(byte[] arr, int length) {
		return parseInt(arr, 0, length);
	}
	
	public static int parseInt(byte[] arr, int offset, int length) {
		int num = 0;
		
		for (int i = offset; length > 0; i++, length--) {
			if (ParseUtils.isDigit(arr[i]))
				num = num * 10 + arr[i] - '0';
			else
				throw new NumberFormatException("Invalid number format");
		}
		
		return num;
	}
}
