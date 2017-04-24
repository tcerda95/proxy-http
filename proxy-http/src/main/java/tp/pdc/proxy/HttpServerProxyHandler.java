package tp.pdc.proxy;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class HttpServerProxyHandler extends HttpHandler {

	public HttpServerProxyHandler(int readBufferSize, ByteBuffer writeBuffer, ByteBuffer processedBuffer) {
		super(readBufferSize, writeBuffer, processedBuffer);
	}

	// se copia codigo de httpClientProxyHanlder pero las excepciones son distintas
	@Override
	protected void processRead(SelectionKey key) {
		SocketChannel socketChannel = (SocketChannel) key.channel();
		ByteBuffer buffer = this.getReadBuffer();
		
		try {
			socketChannel.read(buffer);
		} catch (IOException e) {
			// Avisar al cliente que no se pudo leer respuesta del server
			e.printStackTrace();
		} finally {
			try {
				socketChannel.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	protected void process(ByteBuffer inputBuffer, SelectionKey key) {
		ByteBuffer processedBuffer = this.getProcessedBuffer();
		processedBuffer.put(inputBuffer);
		this.getConnectedPeerKey().interestOps(SelectionKey.OP_WRITE); // OJO el cliente podría estar anotado para read
	}

	@Override
	protected void processWrite(ByteBuffer inputBuffer, SelectionKey key) {
		SocketChannel socketChannel = (SocketChannel) key.channel();
		try {
			socketChannel.write(inputBuffer);
			key.interestOps(SelectionKey.OP_READ);
		} catch (IOException e) {
			
		}
	}

}
