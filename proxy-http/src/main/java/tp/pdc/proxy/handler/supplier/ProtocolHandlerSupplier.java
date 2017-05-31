package tp.pdc.proxy.handler.supplier;

import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tp.pdc.proxy.ProxyProperties;
import tp.pdc.proxy.handler.ProtocolHandler;

public class ProtocolHandlerSupplier implements Supplier<ProtocolHandler> {
	private static final ProtocolHandlerSupplier INSTANCE = new ProtocolHandlerSupplier();
	
	private final Logger logger = LoggerFactory.getLogger(ProtocolHandlerSupplier.class);
	private final int bufferSize;
	
	private ProtocolHandlerSupplier() {
		final ProxyProperties properties = ProxyProperties.getInstance();
		bufferSize = properties.getProtocolBufferSize();
	}
	
	public final static ProtocolHandlerSupplier getInstance() {
		return INSTANCE;
	}
	
	@Override
	public ProtocolHandler get() {
		logger.info("Protocol client connection accepted");
		return new ProtocolHandler(bufferSize);
	}

}