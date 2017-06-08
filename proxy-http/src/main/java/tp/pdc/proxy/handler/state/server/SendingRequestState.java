package tp.pdc.proxy.handler.state.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tp.pdc.proxy.handler.HttpServerProxyHandler;
import tp.pdc.proxy.handler.interfaces.HttpServerState;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;

/**
 * Server state representing a request being sent to the server.
 * It registers client for reading the server's response.
 *
 * @see LastWriteState
 */
public class SendingRequestState implements HttpServerState {

	private static final Logger LOGGER = LoggerFactory.getLogger(SendingRequestState.class);

	private static final SendingRequestState INSTANCE = new SendingRequestState();

	private SendingRequestState () {
	}

	public static SendingRequestState getInstance () {
		return INSTANCE;
	}

	@Override
	public void handle (HttpServerProxyHandler handler, SelectionKey key) {
		ByteBuffer writeBuffer = handler.getWriteBuffer();

		LOGGER.debug("Registering client for read: whole request not processed yet");
		handler.getConnectedPeerKey().interestOps(SelectionKey.OP_READ);
		handler.getClientHandler().handleProcess(handler
			.getConnectedPeerKey());  // Free space in client processedBuffer --> client can process

		if (!writeBuffer.hasRemaining()) {
			LOGGER.debug("Unregistering server from write: nothing left in server write buffer");
			key.interestOps(0);
		}
	}
}
