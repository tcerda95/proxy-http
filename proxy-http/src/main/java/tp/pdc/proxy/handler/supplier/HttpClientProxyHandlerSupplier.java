package tp.pdc.proxy.handler.supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tp.pdc.proxy.handler.HttpClientProxyHandler;
import tp.pdc.proxy.header.Method;
import tp.pdc.proxy.metric.ClientMetricImpl;
import tp.pdc.proxy.metric.interfaces.ClientMetric;
import tp.pdc.proxy.properties.ProxyProperties;

import java.util.Set;
import java.util.function.Supplier;

/**
 * Encapsulates proxy configuration with clients, such as methods accepted and metrics.
 * Serves as a {@link HttpClientProxyHandler} factory when a connection is accepted on the corresponding proxy port.
 */
public class HttpClientProxyHandlerSupplier implements Supplier<HttpClientProxyHandler> {
	private static final HttpClientProxyHandlerSupplier INSTANCE =
		new HttpClientProxyHandlerSupplier();

	private final Logger logger = LoggerFactory.getLogger(HttpClientProxyHandlerSupplier.class);
	private final ClientMetric metrics = ClientMetricImpl.getInstance();

	/**
	 * Methods the proxy accepts from a client.
	 */
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
