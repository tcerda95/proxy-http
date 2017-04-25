package tp.pdc.proxy;

//TODO: tests
public class ParseUtils {
    private static final int US_ASCII_LENGTH = 128;

    private static boolean[] isToken, isAlphabetic, isSeparator, isLWS, isDigit;

    private static final char CR = (char) 13, LF = (char) 10, SP = (char) 32, HT = (char) 9;

    private static final char[] separator = {'(', ')', '<', '>', '@', ',', ';', ':', '\\',
        '"', '/', '[', ']', '?', '=', '{', '}', SP, HT};
    
    // Cargo tablas estáticas para hacer los chequeos más rápido.
    static {
        isToken = new boolean[US_ASCII_LENGTH]; isSeparator = new boolean[US_ASCII_LENGTH];
        isLWS = new boolean[US_ASCII_LENGTH]; isDigit = new boolean[US_ASCII_LENGTH];
        isAlphabetic = new boolean[US_ASCII_LENGTH];

        for (char c = 0; c < US_ASCII_LENGTH; c++) {
            isSeparator[c] = contains(separator, c);
            isToken[c] = (31 < c && c < 127 && !isSeparator[c]);
            isLWS[c] = (c == CR || c == LF || c == SP || c == HT);
            isDigit[c] = ('0' <= c && c <= '9');
            isAlphabetic[c] = ('a' <= c && c <= 'z') || ('A' <= c && c <= 'Z');
        }
    }

    private static boolean contains(char[] arr, char toFind) {
        for (char c: arr) {
            if (c == toFind)
                return true;
        }
        return false;
    }

    public static boolean isHeaderNameChar (char c) {
        return isToken[c];
    }

    public static boolean isHeaderContentChar (char c) {
        return isToken[c] || isLWS[c] || isSeparator[c];
    }

    public static boolean isDigit (char c) {
        return isDigit[c];
    }

    public static boolean isAlphabetic(char c) {
        return isAlphabetic[c];
    }
}
