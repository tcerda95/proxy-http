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

/**
 * Manages connections between the proxy and servers to implement persistent connections
 */
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

	private ConnectionManager () {
		connections = new HashMap<>();
		cleanRate = TimeUnit.SECONDS.toMillis(PROPERTIES.getConnectionCleanRate());
		cleanTime = System.currentTimeMillis() + cleanRate;
	}

	public static final ConnectionManager getInstance () {
		return INSTANCE;
	}
	
	public long getCleanRate() {
		return cleanRate;
	}

	/**
	 * Connects to a server, it can reuse an existing connection or establish a new one.
	 * @param method method requested
	 * @param address address to connect
	 * @param clientKey client's key
	 * @return true if the connection was successful, false if not
	 * @throws IOException
     */
	public boolean connect (Method method, SocketAddress address, SelectionKey clientKey)
		throws IOException {
		if (connections.containsKey(address)) {
			LOGGER.debug("Attempting to reuse connection");
			return reuseConnection(method, address, clientKey);
		} else
			return establishConnection(method, address, clientKey);
	}

	/**
	 * In case an existing connection to the specified {@link SocketAddress} is available it tries to connect
	 * to it.
	 * If it cannot reuse the server's connection it establish a new one
	 * @param method method requestes
	 * @param address address to connect
	 * @param clientKey client's key
	 * @return true if connection was successful, false if not
	 * @throws IOException
     */
	private boolean reuseConnection (Method method, SocketAddress address, SelectionKey clientKey)
		throws IOException {
		Queue<ExpirableContainer<SelectionKey>> connectionQueue = connections.get(address);
		SelectionKey serverKey = retrieveValidKey(connectionQueue);

		if (serverKey == null) {
			LOGGER.debug("Cannot reuse: server closed connection");
			return establishConnection(method, address, clientKey);
		} else {
			LOGGER.debug("Reusing connection with server");

			HttpClientProxyHandler clientHandler = (HttpClientProxyHandler) clientKey.attachment();
			HttpServerProxyHandler serverHandler = (HttpServerProxyHandler) serverKey.attachment();

			serverHandler.setClientMethod(method);
			serverHandler.setConnectedPeerKey(clientKey);
			serverHandler.setWriteBuffer(clientHandler.getProcessedBuffer());
			serverHandler.setProcessedBuffer(clientHandler.getWriteBuffer());
			serverKey.interestOps(SelectionKey.OP_WRITE);

			clientHandler.setConnectedPeerKey(serverKey);
			clientHandler.handleConnect(clientKey);
			return true;
		}
	}

	/**
	 * Gets a valid key from the queue
	 * @param connectionQueue queue with expirable keys
	 * @return {@link SelectionKey}
	 * @throws IOException
     */
	private SelectionKey retrieveValidKey (Queue<ExpirableContainer<SelectionKey>> connectionQueue)
		throws IOException {
		while (!connectionQueue.isEmpty()) {
			SelectionKey key = connectionQueue.remove().getElement();

			if (key.isValid()) {
				SocketChannel serverSocket = (SocketChannel) key.channel();
				ByteBuffer serverReadBuffer =
					((HttpServerProxyHandler) key.attachment()).getReadBuffer();
				if (serverSocket.read(serverReadBuffer) != -1)
					return key;
				else
					serverSocket.close();
			}
		}

		return null;
	}

	/**
	 * Establishes a connection to an specified {@link SocketAddress}
	 * @param method request method
	 * @param address address to connect
	 * @param clientKey client's key
	 * @return true if connection was successfull, false if not
	 * @throws IOException
     */
	private boolean establishConnection (Method method, SocketAddress address,
		SelectionKey clientKey) throws IOException {
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
			clientHandler.handleConnect(clientKey);
		} else {
			serverKey = serverSocket.register(selector, SelectionKey.OP_CONNECT);
		}

		serverKey.attach(buildHttpServerProxyHandler(clientHandler, method, clientKey));
		clientHandler.setConnectedPeerKey(serverKey);
		return connected;
	}

	/**
	 * Buils an {@link HttpServerProxyHandler}
	 * @param clientHandler client handler
	 * @param method requested method
	 * @param clientKey client's key
     * @return
     */
	private HttpServerProxyHandler buildHttpServerProxyHandler (
		HttpClientProxyHandler clientHandler, Method method, SelectionKey clientKey) {
		HttpServerProxyHandler handler =
			new HttpServerProxyHandler(clientHandler.getProcessedBuffer(),
				clientHandler.getWriteBuffer(), method);
		handler.setConnectedPeerKey(clientKey);
		return handler;
	}

	/**
	 * Saves the server's {@link SelectionKey} for future reuse.
	 * @param serverKey
	 * @throws IOException
     */
	public void storeConnection (SelectionKey serverKey) throws IOException {
		if (!serverKey.isValid())
			return;

		serverKey.interestOps(0);
		LOGGER.info("Storing connection for future reusing");
		SocketChannel serverChannel = (SocketChannel) serverKey.channel();
		HttpServerProxyHandler serverHandler = (HttpServerProxyHandler) serverKey.attachment();

		serverHandler.reset(serverKey);

		storeKey(serverChannel.getRemoteAddress(), serverKey);
	}

	/**
	 * Stores the server's {@link SelectionKey} for an specified {@link SocketAddress}.
	 * @param remoteAddress address of the connection
	 * @param serverKey server's key
     */
	@SuppressWarnings({"unchecked", "rawtypes"})
	private void storeKey (SocketAddress remoteAddress, SelectionKey serverKey) {
		Queue<ExpirableContainer<SelectionKey>> connectionQueue;

		if (connections.containsKey(remoteAddress))
			connectionQueue = connections.get(remoteAddress);
		else {
			connectionQueue = new ArrayQueue(ExpirableContainer.class, QUEUE_LENGTH);
			connections.put(remoteAddress, connectionQueue);
		}

		connectionQueue
			.add(new ExpirableContainer<SelectionKey>(serverKey, CONNECTION_TTL, TimeUnit.SECONDS));
	}

	/**
	 * It removes expired elements from the map
	 */
	public void clean () {
		long currentTime = System.currentTimeMillis();

		if (currentTime > cleanTime) {
			LOGGER.debug("Clean time!");

			cleanTime = currentTime + cleanRate;

			Iterator<Queue<ExpirableContainer<SelectionKey>>> iter =
				connections.values().iterator();

			while (iter.hasNext()) {
				Queue<ExpirableContainer<SelectionKey>> queue = iter.next();
				removeExpiredKeys(queue);
				if (queue.isEmpty()) {
					LOGGER.debug("Removed empty queue");
					iter.remove();
				}
			}
		}
	}

	/**
	 * Removes expired elements for the queue of an specific address
	 * @param queue
     */
	private void removeExpiredKeys (Queue<ExpirableContainer<SelectionKey>> queue) {
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
