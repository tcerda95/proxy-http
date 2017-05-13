package tp.pdc.proxy;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
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
import tp.pdc.proxy.structures.ArrayQueue;
import tp.pdc.proxy.structures.FixedLengthQueue;

public class ConnectionManager {
	private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionManager.class);
	private static final ConnectionManager INSTANCE = new ConnectionManager();
	private static final ProxyProperties PROPERTIES = ProxyProperties.getInstance();
	private static final ServerMetric SERVER_METRICS = ServerMetricImpl.getInstance();
	private static final int QUEUE_LENGTH = PROPERTIES.getConnectionQueueLength();
	
	private final Map<SocketAddress, FixedLengthQueue<SelectionKey>> connections;
	
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
		FixedLengthQueue<SelectionKey> connectionQueue = connections.get(address);
		SelectionKey serverKey = retrieveValidKey(connectionQueue);
		
		if (serverKey == null) {
			LOGGER.debug("Cannot reuse: server closed connection");
			connections.remove(address);
			return establishConnection(method, address, clientKey);
		}
		else {
			LOGGER.debug("Reusing connection with server");
			
			HttpClientProxyHandler clientHandler = (HttpClientProxyHandler) clientKey.attachment();
			HttpServerProxyHandler serverHandler = (HttpServerProxyHandler) serverKey.attachment();
			
			serverHandler.setClientMethod(method);
			serverHandler.setConnectedPeerKey(clientKey);
			serverHandler.setWriteBuffer(clientHandler.getProcessedBuffer());
			serverHandler.setProcessedBuffer(clientHandler.getWriteBuffer());
			serverKey.interestOps(SelectionKey.OP_WRITE);
			
			clientHandler.setConnectedPeerKey(serverKey);
			clientHandler.setConnectedState();
			return true;
		}
	}
	
	private SelectionKey retrieveValidKey(FixedLengthQueue<SelectionKey> connectionQueue) throws IOException {		
		while (!connectionQueue.isEmpty()) {
			SelectionKey key = connectionQueue.remove();
			SocketChannel serverSocket = (SocketChannel) key.channel();
			
			if (key.isValid()) {
				ByteBuffer serverReadBuffer = ((HttpServerProxyHandler) key.attachment()).getReadBuffer();
				if (serverSocket.read(serverReadBuffer) != -1)
					return key;
				else
					serverSocket.close();
			}
		}
		
		return null;
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
		
		storeKey(serverKey, serverChannel.getRemoteAddress());
	}

	// TODO: timers
	private void storeKey(SelectionKey serverKey, SocketAddress remoteAddress) {
		FixedLengthQueue<SelectionKey> connectionQueue;
		
		if (connections.containsKey(remoteAddress))
			connectionQueue = connections.get(remoteAddress);
		else {
			connectionQueue = new ArrayQueue<>(SelectionKey.class, QUEUE_LENGTH);
			connections.put(remoteAddress, connectionQueue);
		}
		
		connectionQueue.add(serverKey);
	}
}
