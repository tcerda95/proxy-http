package tp.pdc.proxy;

import java.nio.charset.Charset;

public class ProxyProperties {

	private static final ProxyProperties INSTANCE = new ProxyProperties();
	
	private final Charset charset = Charset.forName("ASCII");
	private final int bufferSize = 4096;
	
	private ProxyProperties() {
	}
	
	public static final ProxyProperties getInstance() {
		return INSTANCE;
	}
	
	public final Charset getCharset() {
		return charset;
	}
	
	public final int getBufferSize() {
		return bufferSize;
	}
}
