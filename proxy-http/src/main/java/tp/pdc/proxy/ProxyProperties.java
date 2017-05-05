package tp.pdc.proxy;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProxyProperties {

	private static final Logger LOGGER = LoggerFactory.getLogger(ProxyProperties.class);
	private static final String RESOURCE_NAME = "proxy.properties";
	private static final ProxyProperties INSTANCE = new ProxyProperties();
	
	private final Charset charset = Charset.forName("ASCII");
	private final Properties properties;
	
	private ProxyProperties() {
		properties = new Properties();
		ClassLoader loader = Thread.currentThread().getContextClassLoader();
		InputStream resourceStream = loader.getResourceAsStream(RESOURCE_NAME);
		try {
			properties.load(resourceStream);
		} catch (IOException e) {
			LOGGER.error("Failed to load proxy.properties: {}", e.getMessage());
			e.printStackTrace();
		}
	}
	
	public static final ProxyProperties getInstance() {
		return INSTANCE;
	}
	
	public final Charset getCharset() {
		return charset;
	}
	
	public final int getProxyBufferSize() {
		return Integer.parseInt(properties.getProperty("proxy.bufferSize"));
	}
	
	public final int getProxyPort() {
		return Integer.parseInt(properties.getProperty("proxy.port"));
	}
	
	public final int getProtocolBufferSize() {
		return Integer.parseInt(properties.getProperty("protocol.bufferSize"));
	}
	
	public final int getProtocolPort() {
		return Integer.parseInt(properties.getProperty("protocol.port"));
	}
	
}
