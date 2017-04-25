package tp.pdc.proxy.client;

import java.net.InetSocketAddress;

import tp.pdc.proxy.ProxyProperties;

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
			port = parsePort(hostBytes, colonIndex+1);
		}
					
		return new InetSocketAddress(hostname, port);
	}
	
	private int parsePort(byte[] hostBytes, int i) {
		int port = 0;
		for (; i < hostBytes.length; i++) {
			if (isNum(hostBytes[i]))
				port = port * 10 + hostBytes[i] - '0';
			else
				throw new NumberFormatException("Invalid port format");
		}
		
		return port;
	}

	private boolean isNum(byte b) {
		if (b >= '0' && b <= '9')
			return true;
		return false;
	}

}
