package tp.pdc.proxy.handler.state.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tp.pdc.proxy.handler.HttpClientProxyHandler;
import tp.pdc.proxy.handler.interfaces.HttpClientState;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;

/**
 * Client state representing the last time the proxy is writing to the client the response from the server.
 * Once the response is sent it keeps alive the connection between the proxy and the client(Does not close the connection).
 */
public class LastWriteKeepConnection implements HttpClientState {

	private static final Logger LOGGER = LoggerFactory.getLogger(LastWriteKeepConnection.class);

	private static final LastWriteKeepConnection INSTANCE = new LastWriteKeepConnection();

	private LastWriteKeepConnection () {
	}

	public static LastWriteKeepConnection getInstance () {
		return INSTANCE;
	}

	@Override
	public void handle (HttpClientProxyHandler httpHandler, SelectionKey key) {
		LOGGER.debug("Last write keep connection state handle");

		ByteBuffer writeBuffer = httpHandler.getWriteBuffer();

		if (!writeBuffer.hasRemaining()) {
			LOGGER.info("Keeping alive connection: server response processed and sent");
			httpHandler.reset(key);
		}
	}

}
