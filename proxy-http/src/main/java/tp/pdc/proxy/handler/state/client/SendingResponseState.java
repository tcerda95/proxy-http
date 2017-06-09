package tp.pdc.proxy.handler.state.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tp.pdc.proxy.handler.HttpClientProxyHandler;
import tp.pdc.proxy.handler.interfaces.HttpClientState;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;

/**
 * Client state in which the response from the server is being sent to the client.
 * This state does not cover the last time the proxy writes to the client({@link LastWriteCloseConnection} and
 * {@link LastWriteKeepConnection})
 */
public class SendingResponseState implements HttpClientState {

	private static final Logger LOGGER = LoggerFactory.getLogger(SendingResponseState.class);

	private static final SendingResponseState INSTANCE = new SendingResponseState();

	private SendingResponseState () {
	}

	public static SendingResponseState getInstance () {
		return INSTANCE;
	}

	@Override
	public void handle (HttpClientProxyHandler httpHandler, SelectionKey key) {
		LOGGER.debug("Server response state handle");
		ByteBuffer writeBuffer = httpHandler.getWriteBuffer();

		LOGGER.debug("Registering server for read: server's response not processed yet");
		httpHandler.getConnectedPeerKey().interestOps(SelectionKey.OP_READ);
		httpHandler.getServerHandler().handleProcess(httpHandler.getConnectedPeerKey(), writeBuffer);  // Espacio libre en processedBuffer ---> servidor puede procesar

		if (!writeBuffer.hasRemaining()) {
			LOGGER.debug("Unregistering client from write: write buffer empty");
			key.interestOps(0);
		}
	}
}
