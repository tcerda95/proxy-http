package tp.pdc.proxy.handler.supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tp.pdc.proxy.ProxyProperties;
import tp.pdc.proxy.handler.HttpClientProxyHandler;
import tp.pdc.proxy.header.Method;
import tp.pdc.proxy.metric.ClientMetricImpl;
import tp.pdc.proxy.metric.interfaces.ClientMetric;

import java.util.Set;
import java.util.function.Supplier;

public class HttpClientProxyHandlerSupplier implements Supplier<HttpClientProxyHandler> {
	private static final HttpClientProxyHandlerSupplier INSTANCE =
		new HttpClientProxyHandlerSupplier();

	private final Logger logger = LoggerFactory.getLogger(HttpClientProxyHandlerSupplier.class);
	private final ClientMetric metrics = ClientMetricImpl.getInstance();
	private final Set<Method> acceptedMethods;

	private HttpClientProxyHandlerSupplier () {
		final ProxyProperties properties = ProxyProperties.getInstance();

		acceptedMethods = properties.getAcceptedMethods();
	}

	public final static HttpClientProxyHandlerSupplier getInstance () {
		return INSTANCE;
	}

	@Override
	public HttpClientProxyHandler get () {
		logger.info("Client proxy connection accepted");
		metrics.addConnection();
		return new HttpClientProxyHandler(acceptedMethods);
	}

}
