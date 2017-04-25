package tp.pdc.proxy;

import java.nio.ByteBuffer;

//TODO: tests
public class ParseUtils {
    private static final int US_ASCII_LENGTH = 128;

    private static boolean[] isToken, isAlphabetic, isSeparator, isLWS, isDigit;

    private static final byte CR = (byte) 13, LF = (byte) 10, SP = (byte) 32, HT = (byte) 9;

    private static final byte[] separator = {'(', ')', '<', '>', '@', ',', ';', ':', '\\',
        '"', '/', '[', ']', '?', '=', '{', '}', SP, HT};

//    private static final String[] supportedMethods = {"GET", "POST", "HEAD"};

    // Cargo tablas estáticas para hacer los chequeos más rápido.
    static {
        isToken = new boolean[US_ASCII_LENGTH]; isSeparator = new boolean[US_ASCII_LENGTH];
        isLWS = new boolean[US_ASCII_LENGTH]; isDigit = new boolean[US_ASCII_LENGTH];
        isAlphabetic = new boolean[US_ASCII_LENGTH];

        for (int c = 0; c < US_ASCII_LENGTH; c++) {
            isSeparator[c] = contains(separator, (byte) c);
            isToken[c] = (31 < c && c < 127 && !isSeparator[c]);
            isLWS[c] = (c == CR || c == LF || c == SP || c == HT);
            isDigit[c] = ('0' <= c && c <= '9');
            isAlphabetic[c] = ('a' <= c && c <= 'z') || ('A' <= c && c <= 'Z');
        }
    }

    private static boolean contains(byte[] arr, byte toFind) {
        for (byte c: arr) {
            if (c == toFind)
                return true;
        }
        return false;
    }

//    public static boolean isValidMethod(ByteBuffer bytes, int length) {
//        for (byte[] m: methods) {
//            for (int i = 0; i < length; i++) {
//                if (bytes.get() != m[i])
//                    return false;
//            }
//        }
//    }

    public static boolean isHeaderNameChar (byte c) {
        return isToken[c];
    }

    public static boolean isHeaderContentChar (byte c) {
        return isToken[c] || isLWS[c] || isSeparator[c];
    }

    public static boolean isDigit (byte c) {
        return isDigit[c];
    }

    public static boolean isAlphabetic(byte c) {
        return isAlphabetic[c];
    }
}
