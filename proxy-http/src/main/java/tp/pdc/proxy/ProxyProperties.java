package tp.pdc.proxy;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tp.pdc.proxy.header.Method;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.*;

/**
 * Configuration properties for the HTTP proxy
 */
public class ProxyProperties {

	private static final Logger LOGGER = LoggerFactory.getLogger(ProxyProperties.class);
	private static final String RESOURCE_NAME = "proxy.properties";
	private static final ProxyProperties INSTANCE = new ProxyProperties();
	private static final Set<Method> ACCEPTED_METHODS =
		Collections.unmodifiableSet(EnumSet.of(Method.GET, Method.POST, Method.HEAD));

	private final Charset charset = Charset.forName("ASCII");
	private final List<byte[]> acceptedCharsets;
	private final Properties properties;

	private ProxyProperties () {
		properties = new Properties();
		ClassLoader loader = Thread.currentThread().getContextClassLoader();
		InputStream resourceStream = loader.getResourceAsStream(RESOURCE_NAME);

		try {
			properties.load(resourceStream);
		} catch (IOException e) {
			LOGGER.error("Failed to load proxy.properties: {}", e.getMessage());
			e.printStackTrace();
		}

		acceptedCharsets = new ArrayList<>();
		acceptedCharsets.add(ArrayUtils.EMPTY_BYTE_ARRAY);
		acceptedCharsets.add("utf-8".getBytes(getCharset()));
		acceptedCharsets.add("iso-8859-1".getBytes(getCharset()));
		acceptedCharsets.add("ascii".getBytes(getCharset()));
		acceptedCharsets.add("us-ascii".getBytes(getCharset()));
	}

	public static final ProxyProperties getInstance () {
		return INSTANCE;
	}

	public final Charset getCharset () {
		return charset;
	}

	public Set<Method> getAcceptedMethods () {
		return ACCEPTED_METHODS;
	}

	public List<byte[]> getAcceptedCharsets () {
		return acceptedCharsets;
	}

	public final int getProxyBufferSize () {
		return Integer.parseInt(properties.getProperty("proxy.bufferSize"));
	}

	public final int getProxyPort () {
		return Integer.parseInt(properties.getProperty("proxy.port"));
	}

	public final int getProtocolBufferSize () {
		return Integer.parseInt(properties.getProperty("protocol.bufferSize"));
	}

	public final int getProtocolParserBufferSize () {
		return Integer.parseInt(properties.getProperty("protocol.parser.bufferSize"));
	}

	public final int getProtocolPort () {
		return Integer.parseInt(properties.getProperty("protocol.port"));
	}

	public final int getMethodBufferSize () {
		return Integer.parseInt(properties.getProperty("parser.methodBufferSize"));
	}

	public final int getURIHostBufferSize () {
		return Integer.parseInt(properties.getProperty("parser.URIHostBufferSize"));
	}

	public final int getHeaderNameBufferSize () {
		return Integer.parseInt(properties.getProperty("parser.headerNameBufferSize"));
	}

	public final int getHeaderContentBufferSize () {
		return Integer.parseInt(properties.getProperty("parser.headerContentBufferSize"));
	}

	public final int getConnectionQueueLength () {
		return Integer.parseInt(properties.getProperty("connection.queue.length"));
	}

	public final int getConnectionTimeToLive () {
		return Integer.parseInt(properties.getProperty("connection.ttl"));
	}

	public final int getConnectionCleanRate () {
		return Integer.parseInt(properties.getProperty("connection.clean.rate"));
	}

	public int getProtocolHeaderNameBufferSize () {
		return Integer.parseInt(properties.getProperty("protocol.parser.headerNameBufferSize"));
	}

	public int getProtocolHeaderContentBufferSize () {
		return Integer.parseInt(properties.getProperty("protocol.parser.headerContentBufferSize"));
	}

}
