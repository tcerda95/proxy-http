package tp.pdc.proxy;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import tp.pdc.proxy.header.Method;

public class ProxyLogger {
	
	private static final ProxyProperties PROPERTIES = ProxyProperties.getInstance();
    private static final ExecutorService LOGGER_EXECUTOR  = Executors.newSingleThreadExecutor();

    private static final ProxyLogger INSTANCE = new ProxyLogger();
    
    private ProxyLogger() {
    }
    
    public static ProxyLogger getInstance() {
    	return INSTANCE;
    }
    
    //TODO: client ip o algo. Recurso en el host
    public void LogAccess(Method method, InetSocketAddress source, InetSocketAddress destination, int statusCode, byte[] userAgentBytes, byte[] serverBytes) {
        LOGGER_EXECUTOR.execute(() -> {
        	String userAgent = new String(userAgentBytes, PROPERTIES.getCharset());
        	String server = new String(serverBytes, PROPERTIES.getCharset());

            StringBuilder builder = new StringBuilder();
            builder
            	.append(source.getAddress().getHostAddress())
            	.append(" - ")
                .append(method)
                .append(" [")
                .append(destination.getHostName())
                .append(":")
                .append(destination.getPort())
                .append("]")
                .append(" SC:").append(statusCode)
                .append(" UA:" ).append(userAgent)
                .append(" SV:").append(server)
                .append("\n");

            String log = builder.toString();
            
            // TODO: loggear
            System.out.println(log);
        });
    }

    public void LogError(String errorMessage, Method method, InetSocketAddress destination, int statusCode) {
        LOGGER_EXECUTOR.execute(() -> {
            StringBuilder builder = new StringBuilder();
            builder
                .append(" request: ")
                .append(method)
                .append(" ")
                .append(destination.getHostName())
                .append(":")
                .append(destination.getPort())
                .append(" SC:").append(statusCode)
                .append("\n");

            String log = builder.toString();
            
            // TODO: loggear
            System.out.println(log);
        });
    }
}
