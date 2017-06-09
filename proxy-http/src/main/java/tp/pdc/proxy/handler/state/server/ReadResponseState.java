package tp.pdc.proxy.handler.state.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tp.pdc.proxy.bytes.BytesUtils;
import tp.pdc.proxy.connection.ConnectionManager;
import tp.pdc.proxy.handler.HttpServerProxyHandler;
import tp.pdc.proxy.handler.interfaces.HttpServerState;
import tp.pdc.proxy.header.Header;
import tp.pdc.proxy.header.HeaderValue;
import tp.pdc.proxy.parser.interfaces.HttpResponseParser;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;

/**
 * Server state in which a response from the server is being read and send to the client.
 * Once server response is fully processed it signals the client to send last write.
 * If the connection to the server should be kept alive it stores the connection in a queue for future uses.
 * If not, it closes the connection to the server.
 *
 * @see {@link tp.pdc.proxy.handler.state.client.LastWriteKeepConnection}
 * @see {@link tp.pdc.proxy.handler.state.client.LastWriteCloseConnection}
 */
public class ReadResponseState implements HttpServerState {

	private static final ConnectionManager CONNECTION_MANAGER = ConnectionManager.getInstance();
	private static final Logger LOGGER = LoggerFactory.getLogger(ReadResponseState.class);

	private static final ReadResponseState INSTANCE = new ReadResponseState();

	private ReadResponseState () {
	}

	public static ReadResponseState getInstance () {
		return INSTANCE;
	}

	@Override
	public void handle (HttpServerProxyHandler handler, SelectionKey key) {
		ByteBuffer processedBuffer = handler.getProcessedBuffer();

		if (processedBuffer.position() != 0) {
			LOGGER.debug("Registering client for write: server has written to processed buffer");
			handler.getConnectedPeerKey().interestOps(SelectionKey.OP_WRITE);
		}

		if (handler.hasFinishedProcessing()) {
			LOGGER.debug("Server response processed");
			LOGGER.debug("Signaling client to send last write and keep connection");
			
			handler.getClientHandler().signalResponseProcessed(false); // Client should attempt to store connection

			try {
				handler.logAccess(key);
				if (shouldKeepAlive(handler))
					CONNECTION_MANAGER.storeConnection(key);
				else
					key.channel().close();
			} catch (IOException e) {
				LOGGER.error("Failed to store server's connection on response processed: {}", e.getMessage());
				e.printStackTrace();
			}
		} else if (!processedBuffer.hasRemaining()) {
			LOGGER.debug("Unregistering server from read: processed buffer full");
			key.interestOps(0);
		}
	}

	private boolean shouldKeepAlive (HttpServerProxyHandler handler) {
		HttpResponseParser responseParser = handler.getResponseParser();
		return responseParser.hasHeaderValue(Header.CONNECTION) && BytesUtils
			.equalsBytes(responseParser.getHeaderValue(Header.CONNECTION),
				HeaderValue.KEEP_ALIVE.getValue(), BytesUtils.TO_LOWERCASE);
	}
}
