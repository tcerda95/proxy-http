package tp.pdc.proxy.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tp.pdc.proxy.handler.interfaces.Handler;
import tp.pdc.proxy.header.HttpErrorCode;
import tp.pdc.proxy.metric.ServerMetricImpl;
import tp.pdc.proxy.metric.interfaces.ServerMetric;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.function.Supplier;

public class SelectorHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(SelectorHandler.class);
	private static final ServerMetric SERVER_METRICS = ServerMetricImpl.getInstance();

	/**
	 * Handles accepting a client connection. The Supplier attachment get method is invoked
	 * in order to register a proxy or protocol client handler transparently.
	 * @param key client's key
     */
	public void handleAccept (SelectionKey key) {
		Supplier<?> supplier = (Supplier<?>) key.attachment();

		try {
			SocketChannel socketChannel = ((ServerSocketChannel) key.channel()).accept();
			socketChannel.configureBlocking(false);
			socketChannel.register(key.selector(), SelectionKey.OP_READ, supplier.get());
		} catch (IOException e) {
			LOGGER.warn("Failed to accept client connection: {}", e.getMessage());
		}
	}

	/**
	 * Handles reading
	 * @param key
     */
	public void handleRead (SelectionKey key) {
		Handler handler = (Handler) key.attachment();
		handler.handleRead(key);
	}

	/**
	 * Handles connecting to server
	 * @param key server's key
     */
	public void handleConnect (SelectionKey key) {
		SocketChannel socketChannel = (SocketChannel) key.channel();
		HttpClientProxyHandler clientHandler = clientHandlerFromServerKey(key);
		SelectionKey clientKey = clientKeyFromServerKey(key);

		try {
			if (socketChannel.finishConnect()) {
				LOGGER.info("Server connection established");
				SERVER_METRICS.addConnection();

				key.interestOps(SelectionKey.OP_WRITE);
				clientHandler.handleConnect(clientKey);
			}
		} catch (IOException e) {
			LOGGER.warn("Failed to connect to server: {}", e.getMessage());

			clientHandler.setErrorState(clientKey, HttpErrorCode.BAD_GATEWAY_502, e.getMessage());
		}
	}

	/**
	 * Handles write
	 * @param key
     */
	public void handleWrite (SelectionKey key) {
		Handler handler = (Handler) key.attachment();
		handler.handleWrite(key);
	}

	/**
	 * Gets the client's {@link SelectionKey} from the server's selection key
	 * @param key server's selection key
	 * @return client's {@link SelectionKey}
     */
	private SelectionKey clientKeyFromServerKey (SelectionKey key) {
		HttpHandler serverHandler = (HttpHandler) key.attachment();
		return serverHandler.getConnectedPeerKey();
	}

	/**
	 * Gets the server's {@link SelectionKey} from the client's selection key
	 * @param key client's selection key
	 * @return server's {@link SelectionKey}
     */
	private HttpClientProxyHandler clientHandlerFromServerKey (SelectionKey key) {
		HttpHandler serverHandler = (HttpHandler) key.attachment();
		SelectionKey clientKey = serverHandler.getConnectedPeerKey();
		return (HttpClientProxyHandler) clientKey.attachment();
	}
}
