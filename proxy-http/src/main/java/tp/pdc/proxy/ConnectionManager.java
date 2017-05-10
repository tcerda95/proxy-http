package tp.pdc.proxy;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tp.pdc.proxy.handler.HttpClientProxyHandler;
import tp.pdc.proxy.handler.HttpServerProxyHandler;
import tp.pdc.proxy.header.Method;
import tp.pdc.proxy.metric.ServerMetricImpl;
import tp.pdc.proxy.metric.interfaces.ServerMetric;

public class ConnectionManager {
	private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionManager.class);
	private static final ConnectionManager INSTANCE = new ConnectionManager();
	private static final ProxyProperties PROPERTIES = ProxyProperties.getInstance();
	private static final ServerMetric SERVER_METRICS = ServerMetricImpl.getInstance();
	
	private final Map<SocketAddress, SelectionKey> connections;
	
	private ConnectionManager() {
		connections = new HashMap<>();
	}
	
	public static final ConnectionManager getInstance() {
		return INSTANCE;
	}

	public boolean connect(Method method, SocketAddress address, SelectionKey clientKey) throws IOException {
		if (connections.containsKey(address)) {
			LOGGER.debug("Attempting to reuse connection");
			return reuseConnection(method, address, clientKey);
		}
		else
			return establishConnection(method, address, clientKey);
	}
	
	private boolean reuseConnection(Method method, SocketAddress address, SelectionKey clientKey) throws IOException {
		SelectionKey serverKey = connections.get(address);
		HttpClientProxyHandler clientHandler = (HttpClientProxyHandler) clientKey.attachment();
		HttpServerProxyHandler serverHandler = (HttpServerProxyHandler) serverKey.attachment();
		SocketChannel serverSocket = (SocketChannel) serverKey.channel();
		
		connections.remove(address);
		
		// TODO: pensar mejor alternativa a la del -1
		if (serverKey.isValid() && serverSocket.read(serverHandler.getReadBuffer()) != -1) {
			LOGGER.debug("Reusing connection with server");
			
			serverHandler.setClientMethod(method);
			serverHandler.setConnectedPeerKey(clientKey);
			serverHandler.setWriteBuffer(clientHandler.getProcessedBuffer());
			serverHandler.setProcessedBuffer(clientHandler.getWriteBuffer());
			serverKey.interestOps(SelectionKey.OP_WRITE);
			clientHandler.setConnectedPeerKey(serverKey);
			clientHandler.setConnectedState();
			return true;
		}
		
		return establishConnection(method, address, clientKey);
	}

	private boolean establishConnection(Method method, SocketAddress address, SelectionKey clientKey) throws IOException {
		boolean connected;
		HttpClientProxyHandler clientHandler = (HttpClientProxyHandler) clientKey.attachment();
		SocketChannel serverSocket = SocketChannel.open();
		Selector selector = clientKey.selector();
		SelectionKey serverKey;
				
		serverSocket.configureBlocking(false);
		connected = serverSocket.connect(address);
		
		if (connected) {
			serverKey = serverSocket.register(selector, SelectionKey.OP_WRITE);
			SERVER_METRICS.addConnection();
			clientHandler.setConnectedState();
		}
		else {
			serverKey = serverSocket.register(selector, SelectionKey.OP_CONNECT);
		}
		
		serverKey.attach(buildHttpServerProxyHandler(clientHandler, method, clientKey));
		clientHandler.setConnectedPeerKey(serverKey);
		return connected;
	}

	private HttpServerProxyHandler buildHttpServerProxyHandler(HttpClientProxyHandler clientHandler, Method method, SelectionKey clientKey) {
		HttpServerProxyHandler handler = new HttpServerProxyHandler(PROPERTIES.getProxyBufferSize(), clientHandler.getProcessedBuffer(), clientHandler.getWriteBuffer(), method);
		handler.setConnectedPeerKey(clientKey);
		return handler;
	}
	
	public void storeConnection(SelectionKey serverKey) throws IOException {
		if (!serverKey.isValid())
			return;
		
		serverKey.interestOps(0);
		LOGGER.info("Storing connection for future reusing");
		SocketChannel serverChannel = (SocketChannel) serverKey.channel();
		HttpServerProxyHandler serverHandler = (HttpServerProxyHandler) serverKey.attachment();
		
		serverHandler.reset();
		
		SocketAddress address = serverChannel.getRemoteAddress();
		connections.put(address, serverKey);
	}
}
