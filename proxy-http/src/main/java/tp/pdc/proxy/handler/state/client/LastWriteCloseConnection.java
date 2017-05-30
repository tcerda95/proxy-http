package tp.pdc.proxy.handler.state.client;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tp.pdc.proxy.handler.HttpClientProxyHandler;
import tp.pdc.proxy.handler.interfaces.HttpClientState;

public class LastWriteCloseConnection implements HttpClientState {

	private static final Logger LOGGER = LoggerFactory.getLogger(LastWriteCloseConnection.class);
	
	private static final LastWriteCloseConnection INSTANCE = new LastWriteCloseConnection();
	
	private LastWriteCloseConnection() {
	}
	
	public static LastWriteCloseConnection getInstance() {
		return INSTANCE;
	}
	
	@Override
	public void handle(HttpClientProxyHandler httpHandler, SelectionKey key) {
		LOGGER.debug("Last write close connection state handle");

		ByteBuffer writeBuffer = httpHandler.getWriteBuffer();
		SocketChannel socketChannel = (SocketChannel) key.channel();
		
		if (!writeBuffer.hasRemaining()) {
			LOGGER.info("Closing connection to client: response sent");
			
			try {
				socketChannel.close();
			} catch (IOException e) {
				LOGGER.error("Failed to close client's channel on client's write error");
				e.printStackTrace();
			}
			
		}
	}

}
