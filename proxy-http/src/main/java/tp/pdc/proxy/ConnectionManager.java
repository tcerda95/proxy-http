package tp.pdc.proxy;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tp.pdc.proxy.handler.HttpClientProxyHandler;
import tp.pdc.proxy.handler.HttpServerProxyHandler;
import tp.pdc.proxy.header.Method;
import tp.pdc.proxy.metric.ServerMetricImpl;
import tp.pdc.proxy.metric.interfaces.ServerMetric;
import tp.pdc.proxy.structures.ArrayQueue;
import tp.pdc.proxy.time.ExpirableContainer;

public class ConnectionManager {
	private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionManager.class);
	private static final ProxyProperties PROPERTIES = ProxyProperties.getInstance();
	private static final ConnectionManager INSTANCE = new ConnectionManager();
	private static final ServerMetric SERVER_METRICS = ServerMetricImpl.getInstance();
	private static final int QUEUE_LENGTH = PROPERTIES.getConnectionQueueLength();
	private static final int CONNECTION_TTL = PROPERTIES.getConnectionTimeToLive();
	
	private final Map<SocketAddress, Queue<ExpirableContainer<SelectionKey>>> connections;
	private final long cleanRate;
	private long cleanTime;
	
	private ConnectionManager() {
		connections = new HashMap<>();
		cleanRate = TimeUnit.SECONDS.toMillis(PROPERTIES.getConnectionCleanRate());
		cleanTime = System.currentTimeMillis() + cleanRate;
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
		Queue<ExpirableContainer<SelectionKey>> connectionQueue = connections.get(address);
		SelectionKey serverKey = retrieveValidKey(connectionQueue);
		
		if (serverKey == null) {
			LOGGER.debug("Cannot reuse: server closed connection");
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
	
	private SelectionKey retrieveValidKey(Queue<ExpirableContainer<SelectionKey>> connectionQueue) throws IOException {		
		while (!connectionQueue.isEmpty()) {
			SelectionKey key = connectionQueue.remove().getElement();
			
			if (key.isValid()) {
				SocketChannel serverSocket = (SocketChannel) key.channel();
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
		
		storeKey(serverChannel.getRemoteAddress(), serverKey);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void storeKey(SocketAddress remoteAddress, SelectionKey serverKey) {
		Queue<ExpirableContainer<SelectionKey>> connectionQueue;
		
		if (connections.containsKey(remoteAddress))
			connectionQueue = connections.get(remoteAddress);
		else {
			connectionQueue = new ArrayQueue(ExpirableContainer.class, QUEUE_LENGTH);
			connections.put(remoteAddress, connectionQueue);
		}
		
		connectionQueue.add(new ExpirableContainer<SelectionKey>(serverKey, CONNECTION_TTL, TimeUnit.SECONDS));
	}
	
	public void clean() {
		long currentTime = System.currentTimeMillis();
		
		if (currentTime > cleanTime) {
			LOGGER.debug("Clean time!");

			cleanTime = currentTime + cleanRate;
			
			Iterator<Queue<ExpirableContainer<SelectionKey>>> iter = connections.values().iterator();
			
			while (iter.hasNext()) {
				Queue<ExpirableContainer<SelectionKey>> queue = iter.next();
				removeExpiredKeys(queue);
				if(queue.isEmpty()) {
					LOGGER.debug("Removed empty queue");
					iter.remove();
				}
			}
		}
	}
	
	private void removeExpiredKeys(Queue<ExpirableContainer<SelectionKey>> queue) {
		while (!queue.isEmpty() && queue.peek().hasExpired()) { // oldest keys are first on queue
			LOGGER.debug("Cleaned expired key");
			
			SelectionKey key = queue.remove().getElement();
			
			try {
				key.channel().close();
			} catch (IOException e) {
				LOGGER.error("Failed to close channel during conneciton clean");
				e.printStackTrace();
			}
		}
	}
}
