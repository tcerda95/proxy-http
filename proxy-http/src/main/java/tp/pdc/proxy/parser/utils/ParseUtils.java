package tp.pdc.proxy.parser.utils;

import java.nio.ByteBuffer;

import static tp.pdc.proxy.parser.utils.AsciiConstants.*;

public class ParseUtils {
	private static final int US_ASCII_LENGTH = 128;
	private static final byte[] separator =
		{'(', ')', '<', '>', '@', ',', ';', ':', '\\', '"', '/', '[', ']', '?', '=', '{', '}',
			SP.getValue(), HT.getValue()};
	private static boolean[] isToken, isAlphabetic, isSeparator, isLWS, isDigit, isHexadecimal,
		isAlphaNumerical;

	static {
		isToken = new boolean[US_ASCII_LENGTH];
		isSeparator = new boolean[US_ASCII_LENGTH];
		isLWS = new boolean[US_ASCII_LENGTH];
		isDigit = new boolean[US_ASCII_LENGTH];
		isAlphabetic = new boolean[US_ASCII_LENGTH];
		isHexadecimal = new boolean[US_ASCII_LENGTH];
		isAlphaNumerical = new boolean[US_ASCII_LENGTH];

		for (int c = 0; c < US_ASCII_LENGTH; c++) {
			isSeparator[c] = contains(separator, (byte) c);
			isToken[c] = (31 < c && c < 127 && !isSeparator[c]);
			isLWS[c] = (c == CR.getValue() || c == LF.getValue() || c == SP.getValue() || c == HT.getValue());
			isDigit[c] = ('0' <= c && c <= '9');
			isAlphabetic[c] = ('a' <= c && c <= 'z') || ('A' <= c && c <= 'Z');
			isAlphaNumerical[c] = (isAlphabetic[c] || isDigit[c]);
			isHexadecimal[c] = isDigit[c] || ('A' <= c && c <= 'F') || ('a' <= c && c <= 'f');
		}
	}

	private ParseUtils () {
	}

	private static boolean contains (byte[] arr, byte toFind) {
		for (byte c : arr) {
			if (c == toFind)
				return true;
		}
		return false;
	}

	public static boolean isHeaderNameChar (byte c) {
		return c > 0 && (isToken[c]);
	}

	public static boolean isHeaderContentChar (byte c) {
		return c > 0 && (isToken[c] || isLWS[c] || isSeparator[c]);
	}

	public static boolean isDigit (byte c) {
		return c > 0 && isDigit[c];
	}
	
	public static boolean isWhiteSpace(byte c) {
		return c > 0 && isLWS[c];
	}

	public static boolean isText (byte c) {
		return c > 0 && (isToken[c] || isSeparator[c]);
	}

	public static boolean isUriCharacter (byte c) {
		return c > 0 && (isToken[c] || isDigit[c] || isSeparator[c]);
	}

	public static boolean isAlphabetic (byte c) {
		return c > 0 && isAlphabetic[c];
	}

	public static boolean isAlphaNumerical (byte c) {
		return c > 0 && isAlphaNumerical[c];
	}

	public static boolean isHexadecimal (byte c) {
		return c > 0 && isHexadecimal[c];
	}

	public static int parseInt (byte[] arr) {
		return parseInt(arr, 0, arr.length);
	}

	public static int parseInt (byte[] arr, int length) {
		return parseInt(arr, 0, length);
	}

	public static int parseInt (byte[] arr, int offset, int length) {
		int num = 0;

		for (int i = offset; length > 0; i++, length--) {
			if (ParseUtils.isDigit(arr[i]))
				num = num * 10 + arr[i] - '0';
			else
				throw new NumberFormatException("Invalid number format");
		}

		return num;
	}

	public static byte[] parseInt (int value, ByteBuffer output, ByteBuffer remainingBytes) {

		int len = intLength(value);

		for (int j = len; j > 0; j--) {
			int digit = (int) (value / Math.pow(10, j - 1));

			if (output.hasRemaining())
				output.put((byte) (digit + '0'));
			else
				remainingBytes.put((byte) (digit + '0'));

			value = (int) (value - digit * Math.pow(10, j - 1));
		}

		return output.array();
	}

	public static int intLength (int value) {

		if (value == 0)
			return 1;

		int length = 0;

		while (value != 0) {
			value /= 10;
			length++;
		}

		return length;
	}

	public static byte[] parseLong (long value, ByteBuffer output, ByteBuffer remainingBytes) {

		int len = longLength(value);

		for (int j = len; j > 0; j--) {
			int digit = (int) (value / Math.pow(10, j - 1));

			if (output.hasRemaining())
				output.put((byte) (digit + '0'));
			else
				remainingBytes.put((byte) (digit + '0'));

			value = (long) (value - digit * Math.pow(10, j - 1));
		}

		return output.array();
	}

	public static int longLength (long value) {

		if (value == 0)
			return 1;

		int length = 0;

		while (value != 0) {
			value /= 10;
			length++;
		}

		return length;
	}
}
