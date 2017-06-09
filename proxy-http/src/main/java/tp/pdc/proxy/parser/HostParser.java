package tp.pdc.proxy.parser;

import tp.pdc.proxy.parser.utils.ParseUtils;
import tp.pdc.proxy.properties.ProxyProperties;

import java.net.InetSocketAddress;

public class HostParser {
	public static final int DEFAULT_PORT = 80;
	private static final ProxyProperties PROPERTIES = ProxyProperties.getInstance();

	public InetSocketAddress parseAddress (byte[] hostBytes) {
		int colonIndex;
		int port;
		String hostname;

		for (colonIndex = 0; colonIndex < hostBytes.length && hostBytes[colonIndex] != ':'; colonIndex++)
			;

		hostname = new String(hostBytes, 0, colonIndex, PROPERTIES.getCharset());

		if (colonIndex == hostBytes.length)
			port = DEFAULT_PORT;
		else
			port = ParseUtils.parseInt(hostBytes, colonIndex + 1, hostBytes.length - colonIndex - 1);

		return new InetSocketAddress(hostname, port);
	}
}
