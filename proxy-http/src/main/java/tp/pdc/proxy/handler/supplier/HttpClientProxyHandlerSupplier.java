package tp.pdc.proxy.handler.supplier;

import java.util.Set;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tp.pdc.proxy.ProxyProperties;
import tp.pdc.proxy.handler.HttpClientProxyHandler;
import tp.pdc.proxy.header.Method;
import tp.pdc.proxy.metric.ClientMetricImpl;
import tp.pdc.proxy.metric.interfaces.ClientMetric;

public class HttpClientProxyHandlerSupplier implements Supplier<HttpClientProxyHandler> {
	
	private static final HttpClientProxyHandlerSupplier INSTANCE = new HttpClientProxyHandlerSupplier();
	
	private final Logger logger;
	private final ClientMetric metrics;
	private final Set<Method> acceptedMethods;
	private final int bufferSize;

	private HttpClientProxyHandlerSupplier() {
		final ProxyProperties properties = ProxyProperties.getInstance();
		
		metrics = ClientMetricImpl.getInstance();
		logger = LoggerFactory.getLogger(HttpClientProxyHandlerSupplier.class);
		acceptedMethods = properties.getAcceptedMethods();
		bufferSize = properties.getProxyBufferSize();
	}
	
	public final static HttpClientProxyHandlerSupplier getInstance() {
		return INSTANCE;
	}
	
	@Override
	public HttpClientProxyHandler get() {
		logger.info("Client proxy connection accepted");
		metrics.addConnection();
		return new HttpClientProxyHandler(bufferSize, acceptedMethods);
	}

}
