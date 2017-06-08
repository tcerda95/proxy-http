package tp.pdc.proxy.parser;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.Before;
import org.junit.Test;
import tp.pdc.proxy.ProxyProperties;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;

public class CharsetParserTest {
	private static final ProxyProperties PROPERTIES = ProxyProperties.getInstance();
	private CharsetParser charsetParser = new CharsetParser();

	@Before
	public void setUp () throws Exception {
	}

	@Test
	public void testNoCharset () {
		byte[] expected = ArrayUtils.EMPTY_BYTE_ARRAY;

		List<String> list = Arrays.asList("irrelevant: value ;", "irrelevant: ; ;    ;   ;  ",
			"irrelevant: value    ;    ",
			"irrelevant: value   ; charsett = value   ; c h a r s e t = value",
			"irrelevant:    value    ;   charsetASD = value   ;  charse = value   ;   charse = value  ; t = value");

		for (String str : list)
			assertArrayEquals(expected, charsetParser.extractCharset(strToByteArr(str)));
	}

	@Test
	public void testWithCharset () {
		List<String> charsetList = Arrays.asList("irrelevant: value   ;  charset   =  utf-8  ",
			"irrelevant: value ; Charset=utf-8", "irrelevant: value;   CHARSET=utf-8",
			"irrelevant: value;charset=UTF-8",
			"irrelevant: value ; other = asd ; nvm = me   ;   charset  =   utf-8   ",
			"irrelevant: value;   charset=utF-8",
			"irrelevant: value;    chaRSet  =  utf-8    ;   noMindMe = Sorry");

		byte[] expected = strToByteArr("utf-8"); // lower case expected

		for (String str : charsetList)
			assertArrayEquals(expected, charsetParser.extractCharset(strToByteArr(str)));
	}

	private byte[] strToByteArr (String str) {
		return str.getBytes(PROPERTIES.getCharset());
	}
}
