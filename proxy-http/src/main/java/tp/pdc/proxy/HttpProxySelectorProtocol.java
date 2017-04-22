package tp.pdc.proxy;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class HttpProxySelectorProtocol {
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
			if (socketChannel.finishConnect())
				key.interestOps(SelectionKey.OP_WRITE);
		} catch (IOException e) {
			e.printStackTrace();
			// TODO: response de error al cliente de que no se pudo conectar al servidor
		}
	}

	public void handleWrite(SelectionKey key) {
		HttpHandler attributes = (HttpHandler) key.attachment();
		attributes.handleWrite(key);
	}

}
