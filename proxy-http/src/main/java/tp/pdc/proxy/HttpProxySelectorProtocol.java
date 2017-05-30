package tp.pdc.proxy;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tp.pdc.proxy.handler.HttpClientProxyHandler;
import tp.pdc.proxy.handler.HttpHandler;
import tp.pdc.proxy.handler.interfaces.Handler;
import tp.pdc.proxy.metric.ServerMetricImpl;
import tp.pdc.proxy.metric.interfaces.ServerMetric;

public class HttpProxySelectorProtocol {
	private final static Logger LOGGER = LoggerFactory.getLogger(HttpProxySelectorProtocol.class);
	private static final ServerMetric SERVER_METRICS = ServerMetricImpl.getInstance();

	public void handleAccept(SelectionKey key) {
		Supplier<?> supplier = (Supplier<?>) key.attachment();
		
		try {
			SocketChannel socketChannel = ((ServerSocketChannel) key.channel()).accept();
			socketChannel.configureBlocking(false);
			socketChannel.register(key.selector(), SelectionKey.OP_READ, supplier.get());
		} catch (IOException e) {
			LOGGER.warn("Failed to accept client connection: {}", e.getMessage());
		}
	}

	public void handleRead(SelectionKey key) {
		Handler handler = (Handler) key.attachment();
		handler.handleRead(key);
	}

	public void handleConnect(SelectionKey key) {
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
			
			clientHandler.setErrorState(HttpErrorCode.BAD_GATEWAY_502, clientKey);			
		}
	}

	public void handleWrite(SelectionKey key) {
		Handler handler = (Handler) key.attachment();
		handler.handleWrite(key);
	}

	private SelectionKey clientKeyFromServerKey(SelectionKey key) {
		HttpHandler serverHandler = (HttpHandler) key.attachment();
		return serverHandler.getConnectedPeerKey();
	}
	
	private HttpClientProxyHandler clientHandlerFromServerKey(SelectionKey key) {
		HttpHandler serverHandler = (HttpHandler) key.attachment();
		SelectionKey clientKey = serverHandler.getConnectedPeerKey();
		return (HttpClientProxyHandler) clientKey.attachment();
	}
}
