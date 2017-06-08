package tp.pdc.proxy.client;

import org.junit.Before;
import org.junit.Test;
import tp.pdc.proxy.parser.HostParser;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;

import static org.junit.Assert.assertEquals;

public class HostParserTest {

	private final static Charset CHARSET = Charset.forName("ASCII");
	private HostParser parser;

	@Before
	public void setUp () throws Exception {
		parser = new HostParser();
	}

	@Test
	public void testOnlyHostname () {
		byte[] hostname = stringToAscii("www.example.com");

		InetSocketAddress address = parser.parseAddress(hostname);

		assertEquals("www.example.com", address.getHostName());
		assertEquals(80, address.getPort());
	}

	@Test
	public void testHostAndPort () {
		byte[] host = stringToAscii("www.example.com:8080");

		InetSocketAddress address = parser.parseAddress(host);

		assertEquals("www.example.com", address.getHostName());
		assertEquals(8080, address.getPort());
	}

	private byte[] stringToAscii (String str) {
		return str.getBytes(CHARSET);
	}
}
