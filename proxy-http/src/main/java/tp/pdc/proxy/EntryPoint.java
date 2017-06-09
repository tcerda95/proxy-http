package tp.pdc.proxy;

import java.io.IOException;

import tp.pdc.proxy.properties.ProxyProperties;

public class EntryPoint {

	private static final ProxyProperties PROPERTIES = ProxyProperties.getInstance();
	
	public static void main(String[] args) throws IOException {
		new PDCServer(PROPERTIES.getProxyPort(), PROPERTIES.getProtocolPort()).run();
	}

}
