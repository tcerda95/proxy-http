package tp.pdc.proxy;

import java.nio.ByteBuffer;

public class ByteBufferFactory {

	public static final int MIN_PROXY_SIZE = 512;
	public static final int MAX_PROXY_SIZE = 1024 * 1024; // 1Mb
	private static final ByteBufferFactory INSTANCE = new ByteBufferFactory();
	private int proxyBufferSize;

	private ByteBufferFactory () {
		this.proxyBufferSize = ProxyProperties.getInstance().getProxyBufferSize();
	}

	public static ByteBufferFactory getInstance () {
		return INSTANCE;
	}

	public ByteBuffer getProxyBuffer () {
		return ByteBuffer.allocate(proxyBufferSize);
	}

	public int getProxyBufferSize () {
		return proxyBufferSize;
	}

	public void setProxyBufferSize (int size) {
		this.proxyBufferSize = normalizeSize(size);
	}

	private int normalizeSize (int size) {
		if (size < MIN_PROXY_SIZE)
			return MIN_PROXY_SIZE;

		if (size > MAX_PROXY_SIZE)
			return MAX_PROXY_SIZE;

		return size;
	}
}
