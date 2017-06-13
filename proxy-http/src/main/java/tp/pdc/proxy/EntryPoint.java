package tp.pdc.proxy;

import java.io.IOException;

import tp.pdc.proxy.properties.ProxyProperties;

/**
 * Used as an entry point start the proxy.
 */
public final class EntryPoint {

	private static final ProxyProperties PROPERTIES = ProxyProperties.getInstance();
	
	private EntryPoint() {
	}
	
	public static void main(String[] args) throws IOException {
		new PDCServer(PROPERTIES.getProxyPort(), PROPERTIES.getProtocolPort()).run();
	}

}
