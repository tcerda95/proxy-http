package tp.pdc.proxy;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.EnumSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tp.pdc.proxy.handler.HttpClientProxyHandler;
import tp.pdc.proxy.handler.HttpHandler;
import tp.pdc.proxy.header.Method;
import tp.pdc.proxy.metric.ClientMetricImpl;
import tp.pdc.proxy.metric.ServerMetricImpl;
import tp.pdc.proxy.metric.interfaces.ClientMetric;
import tp.pdc.proxy.metric.interfaces.ServerMetric;

public class HttpProxySelectorProtocol {
	private final static Logger LOGGER = LoggerFactory.getLogger(HttpProxySelectorProtocol.class);
	private static final ServerMetric SERVER_METRICS = ServerMetricImpl.getInstance();
	private static final ClientMetric CLIENT_METRICS = ClientMetricImpl.getInstance();

	private final Set<Method> acceptedMethods;
	private final int bufSize;
	
	public HttpProxySelectorProtocol(int bufSize) {
		this.bufSize = bufSize;
		acceptedMethods = EnumSet.of(Method.GET, Method.POST, Method.HEAD);  // TODO: mover a properties
	}
	
	public void handleAccept(SelectionKey key) { 
		try {
			SocketChannel socketChannel = ((ServerSocketChannel) key.channel()).accept();
			socketChannel.configureBlocking(false);
			socketChannel.register(key.selector(), SelectionKey.OP_READ, new HttpClientProxyHandler(bufSize, acceptedMethods));
			
			LOGGER.info("Client connection accepted");
			CLIENT_METRICS.addConnection();
		} catch (IOException e) {
			LOGGER.warn("Failed to accept client connection: {}", e.getMessage());
		}
	}

	public void handleRead(SelectionKey key) {
		HttpHandler handler = (HttpHandler) key.attachment();
		handler.handleRead(key);
	}

	public void handleConnect(SelectionKey key) {
		SocketChannel socketChannel = (SocketChannel) key.channel();
		HttpClientProxyHandler clientHandler = clientHandlerFromServerKey(key);
		
		try {
			if (socketChannel.finishConnect()) {
				LOGGER.info("Server connection established");
				SERVER_METRICS.addConnection();
				
				key.interestOps(SelectionKey.OP_WRITE);
				clientHandler.setConnectedState();
			}
		} catch (IOException e) {
			LOGGER.warn("Failed to connect to server: {}", e.getMessage());
			
			HttpHandler serverHandler = (HttpHandler) key.attachment();
			SelectionKey clientKey = serverHandler.getConnectedPeerKey();
			
			clientHandler.setErrorState(HttpErrorCode.BAD_GATEWAY_502, clientKey);			
		}
	}

	public void handleWrite(SelectionKey key) {
		HttpHandler handler = (HttpHandler) key.attachment();
		handler.handleWrite(key);
	}

	private HttpClientProxyHandler clientHandlerFromServerKey(SelectionKey key) {
		HttpHandler serverHandler = (HttpHandler) key.attachment();
		SelectionKey clientKey = serverHandler.getConnectedPeerKey();
		return (HttpClientProxyHandler) clientKey.attachment();
	}
}
