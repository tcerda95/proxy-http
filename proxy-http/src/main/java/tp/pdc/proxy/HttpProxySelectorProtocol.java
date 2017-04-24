package tp.pdc.proxy;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpProxySelectorProtocol {
	private final static Logger LOGGER = LoggerFactory.getLogger(HttpProxySelectorProtocol.class);
	private int bufSize;
	
	public HttpProxySelectorProtocol(int bufSize) {
		this.bufSize = bufSize;
	}
	
	public void handleAccept(SelectionKey key) { 
		try {
			SocketChannel socketChannel = ((ServerSocketChannel) key.channel()).accept();
			socketChannel.configureBlocking(false);
			socketChannel.register(key.selector(), SelectionKey.OP_READ, new HttpClientProxyHandler(bufSize)); // Leer del cliente
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void handleRead(SelectionKey key) {
		HttpHandler attributes = (HttpHandler) key.attachment();
		attributes.handleRead(key);
	}

	public void handleConnect(SelectionKey key) {
		SocketChannel socketChannel = (SocketChannel) key.channel();
		try {
			if (socketChannel.finishConnect()) {
				key.interestOps(SelectionKey.OP_WRITE);
			}
		} catch (IOException e) {
			LOGGER.warn("Failed to connect to server {}", e.getMessage());
			HttpHandler serverHandler = (HttpHandler) key.attachment();
			SelectionKey clientKey = serverHandler.getConnectedPeerKey();
			serverHandler.getProcessedBuffer().put(HttpResponse.BAD_GATEWAY_502.getBytes());
			clientKey.interestOps(SelectionKey.OP_WRITE);
		}
	}

	public void handleWrite(SelectionKey key) {
		HttpHandler attributes = (HttpHandler) key.attachment();
		attributes.handleWrite(key);
	}

}
