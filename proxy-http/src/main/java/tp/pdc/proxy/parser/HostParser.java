package tp.pdc.proxy.parser;

import java.net.InetSocketAddress;

import tp.pdc.proxy.ProxyProperties;
import tp.pdc.proxy.parser.utils.ParseUtils;

public class HostParser {
	public static final int DEFAULT_PORT = 80;
	private static final ProxyProperties PROPERTIES = ProxyProperties.getInstance();
	
	public InetSocketAddress parseAddress(byte[] hostBytes) {
		int colonIndex;
		int port;
		String hostname;
	
		for (colonIndex = 0; colonIndex < hostBytes.length && hostBytes[colonIndex] != ':'; colonIndex++)
			;
		
		hostname = new String(hostBytes, 0, colonIndex, PROPERTIES.getCharset());
		
		if (colonIndex == hostBytes.length)
			port = DEFAULT_PORT;
		else {
			port = ParseUtils.parseInt(hostBytes, colonIndex+1, hostBytes.length - colonIndex - 1);
		}
		
		return new InetSocketAddress(hostname, port);
	}
}
