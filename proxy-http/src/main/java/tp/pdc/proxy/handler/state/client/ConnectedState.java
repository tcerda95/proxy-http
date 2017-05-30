package tp.pdc.proxy.handler.state.client;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tp.pdc.proxy.handler.HttpClientProxyHandler;
import tp.pdc.proxy.handler.interfaces.HttpClientState;

public class ConnectedState implements HttpClientState {

	private static final Logger LOGGER = LoggerFactory.getLogger(ConnectedState.class);
	
	private static final ConnectedState INSTANCE = new ConnectedState();
	
	private ConnectedState() {
	}
	
	public static ConnectedState getInstance() {
		return INSTANCE;
	}
	
	@Override
	public void handle(HttpClientProxyHandler httpHandler, SelectionKey key) {
		LOGGER.debug("Connected state handle");

		ByteBuffer processedBuffer = httpHandler.getProcessedBuffer();
		
		if (httpHandler.hasFinishedProcessing())
			httpHandler.setRequestProcessedState(key);
		else {
			if (processedBuffer.position() != 0) {
				LOGGER.debug("Registering server for write. Must send {} bytes", processedBuffer.position());
				httpHandler.getConnectedPeerKey().interestOps(SelectionKey.OP_WRITE);
			}
			
			if (!processedBuffer.hasRemaining()) {
				LOGGER.debug("Unregistering client from read: processed buffer full");
				key.interestOps(0);				
			}
		}
	}

}
