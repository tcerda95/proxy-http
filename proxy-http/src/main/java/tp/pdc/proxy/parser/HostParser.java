package tp.pdc.proxy.parser;

import tp.pdc.proxy.parser.utils.ParseUtils;
import tp.pdc.proxy.properties.ProxyProperties;

import java.net.InetSocketAddress;

/**
 * Given host and port bytes returns the host name and its port as an {@link InetSocketAddress}
 */
public class HostParser {
	public static final int DEFAULT_PORT = 80;
	private static final ProxyProperties PROPERTIES = ProxyProperties.getInstance();

	/**
	 * Given host and port bytes returns the host name and its port as an {@link InetSocketAddress}
	 * @param hostBytes bytes of the host
	 * @return an {@link InetSocketAddress} with the host and its port
     */
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
