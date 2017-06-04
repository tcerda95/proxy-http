package tp.pdc.proxy;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tp.pdc.proxy.header.Header;
import tp.pdc.proxy.header.Method;
import tp.pdc.proxy.parser.interfaces.HttpRequestParser;
import tp.pdc.proxy.parser.interfaces.HttpResponseParser;

public class ProxyLogger {
	private static final String SEPARATOR = ", ";
	private static final ProxyProperties PROPERTIES = ProxyProperties.getInstance();
	private static final Logger LOGGER = LoggerFactory.getLogger(ProxyLogger.class);
	
    private final ExecutorService loggerExecutor  = Executors.newSingleThreadExecutor();

    private static final ProxyLogger INSTANCE = new ProxyLogger();
    
    private ProxyLogger() {
    }
    
    public static ProxyLogger getInstance() {
    	return INSTANCE;
    }
    
    public void logAccess(HttpRequestParser requestParser, HttpResponseParser responseParser, InetSocketAddress source, InetSocketAddress destination) {
    	byte[] userAgentBytes = requestParser.hasHeaderValue(Header.USER_AGENT) ? requestParser.getHeaderValue(Header.USER_AGENT) : ArrayUtils.EMPTY_BYTE_ARRAY;
    	byte[] serverBytes = responseParser.hasHeaderValue(Header.SERVER) ? responseParser.getHeaderValue(Header.SERVER) : ArrayUtils.EMPTY_BYTE_ARRAY;
    	Method method = requestParser.getMethod();
    	int statusCode = responseParser.getStatusCode();
    	
    	loggerExecutor.execute(() -> {
        	String userAgent = new String(userAgentBytes, PROPERTIES.getCharset());
        	String server = new String(serverBytes, PROPERTIES.getCharset());

            StringBuilder builder = new StringBuilder();
            builder
            	.append(source.getHostString())
            	.append(" - ")
                .append(method)
                .append(" [")
                .append(destination.getHostString())
                .append(":")
                .append(destination.getPort())
                .append("]")
                .append(" SC:").append(statusCode)
                .append(" UA:").append(userAgent)
                .append(" SV:").append(server);

            String log = builder.toString();
            
            LOGGER.info("{}", log);
        });
    }

    public void logError(HttpRequestParser requestParser, InetSocketAddress source, String... messages) {
    	byte[] requestBytes = requestParser.hasRequestLineFinished() ? requestParser.getWholeRequestLine() : ArrayUtils.EMPTY_BYTE_ARRAY;
    	byte[] destinationHostBytes = requestParser.hasHost() ? requestParser.getHostValue() : ArrayUtils.EMPTY_BYTE_ARRAY;

    	loggerExecutor.execute(() -> {
    		String request = new String(requestBytes, PROPERTIES.getCharset());
    		String destinationHost = new String(destinationHostBytes, PROPERTIES.getCharset());
            StringBuilder builder = new StringBuilder();
            
            builder = appendMessages(builder, messages);
            
            if (source != null)
            	builder.append(", client: ").append(source.getHostString());     
            
            if (request.length() > 0)
            	builder.append(", request: ").append(request);
            
            if (destinationHost.length() > 0)
                builder.append(", host: ").append(destinationHost);

            String log = builder.toString();
            
            LOGGER.error("{}", log);
        });
    }

	private StringBuilder appendMessages(StringBuilder builder, String[] messages) {
		builder.append(messages[0]);
		
		for (int i = 1; i < messages.length; i++)
			if (messages[i].length() > 0)
				builder.append(SEPARATOR).append(messages[i]);
		
		return builder;
	}
}
