package tp.pdc.proxy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.channels.UnresolvedAddressException;

public class HttpClientProxyHandler extends HttpHandler {
	private static final int NGINX_PORT = 8081;

	public HttpClientProxyHandler(int bufSize) {
		super(bufSize, ByteBuffer.allocate(bufSize), ByteBuffer.allocate(bufSize));
	}
	
	@Override
	protected void processRead(SelectionKey key) {
		SocketChannel socketChannel = (SocketChannel) key.channel();
		ByteBuffer buffer = this.getReadBuffer();
		
		try {
			socketChannel.read(buffer);
		} catch (IOException e) {
			// TODO No se pudo leer del cliente
			e.printStackTrace();
			try {
				socketChannel.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}

	@Override
	protected void process(ByteBuffer inputBuffer, SelectionKey key) {
		ByteBuffer processedBuffer = this.getProcessedBuffer();
		processedBuffer.put(inputBuffer);
		
		try {
			SocketChannel socket = SocketChannel.open();
			socket.configureBlocking(false);
			HttpHandler serverHandler = new HttpServerProxyHandler(processedBuffer.array().length, processedBuffer, this.getWriteBuffer());
			serverHandler.setConnectedPeerKey(key);
			SelectionKey serverKey;
			
			if (socket.connect(new InetSocketAddress("localhost", NGINX_PORT)))
				serverKey = socket.register(key.selector(), SelectionKey.OP_WRITE, serverHandler);
			else
				serverKey = socket.register(key.selector(), SelectionKey.OP_CONNECT, serverHandler);  // OJO: se crean atributos aunq no se logró la conexión
			
			this.setConnectedPeerKey(serverKey);
		} catch (IOException e) {
			e.printStackTrace();
			// mandar response de error al cliente de no se pudo conectar
		} catch (UnresolvedAddressException e) {
			e.printStackTrace();
			// host falopa	
		}

	}

	@Override
	protected void processWrite(ByteBuffer inputBuffer, SelectionKey key) {
		SocketChannel socketChannel = (SocketChannel) key.channel();
		try {
			socketChannel.write(inputBuffer);
			socketChannel.close();
		} catch (IOException e) {
			e.printStackTrace();
			// No se le pudo responder al cliente
		}
	}

}
