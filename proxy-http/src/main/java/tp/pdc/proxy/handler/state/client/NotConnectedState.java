package tp.pdc.proxy.handler.state.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tp.pdc.proxy.ConnectionManager;
import tp.pdc.proxy.HttpErrorCode;
import tp.pdc.proxy.handler.HttpClientProxyHandler;
import tp.pdc.proxy.handler.interfaces.HttpClientState;
import tp.pdc.proxy.header.Method;
import tp.pdc.proxy.parser.HostParser;
import tp.pdc.proxy.parser.interfaces.HttpRequestParser;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;

/**
 * Client state in which a client has not yet established a connection to any server.
 * In case a host is detected in the request it tries to connect to the corresponding address.
 * in case no host is detected in the request or it is invalid the proper error response is sent to the client.
 * Once it tries to connect (address is resolved), it sets client's state to {@link ConnectingState}
 */
public class NotConnectedState implements HttpClientState {
	private static final Logger LOGGER = LoggerFactory.getLogger(NotConnectedState.class);
	private static final HostParser HOST_PARSER = new HostParser();
	private static final ConnectionManager CONNECTION_MANAGER = ConnectionManager.getInstance();

	private static final NotConnectedState INSTANCE = new NotConnectedState();

	private NotConnectedState () {
	}

	public static NotConnectedState getInstance () {
		return INSTANCE;
	}

	@Override
	public void handle (HttpClientProxyHandler httpHandler, SelectionKey key) {
		LOGGER.debug("Not connected state handle");

		final ByteBuffer processedBuffer = httpHandler.getProcessedBuffer();
		final HttpRequestParser requestParser = httpHandler.getRequestParser();

		if (requestParser.hasHost()) {
			byte[] hostBytes = requestParser.getHostValue();
			InetSocketAddress address;

			try {
				address = HOST_PARSER.parseAddress(hostBytes);
			} catch (IllegalArgumentException e) {
				LOGGER.warn("Failed to parse host: {}", e.getMessage());
				httpHandler.setErrorState(key, HttpErrorCode.BAD_HOST_FORMAT_400);
				return;
			}

			try {
				tryConnect(httpHandler, requestParser.getMethod(), address, key);
			} catch (IOException e) {
				LOGGER.warn("Failed to connect to server: {}", e.getMessage());
				httpHandler.setErrorState(key, HttpErrorCode.BAD_GATEWAY_502, e.getMessage());
			}
		} else if (!processedBuffer.hasRemaining()) {
			LOGGER
				.warn("Client's processed buffer full and connection not established with server");
			httpHandler.setErrorState(key, HttpErrorCode.TOO_MANY_HEADERS_NO_HOST_431);
		} else if (requestParser.hasHeadersFinished()) {
			LOGGER.warn("Impossible to connect: host not found in request header nor URL");
			httpHandler.setErrorState(key, HttpErrorCode.NO_HOST_400);
		}
	}

	private void tryConnect (HttpClientProxyHandler httpHandler, Method requestMethod,
		InetSocketAddress address, SelectionKey key) throws IOException {

		LOGGER.debug("Server address: {}", address);

		if (address.isUnresolved()) {
			LOGGER.warn("Failed to resolve address: {}", address.getHostString());
			httpHandler
				.setErrorState(key, HttpErrorCode.UNRESOLVED_ADDRESS_400, address.getHostString());
		} else {
			httpHandler.setConnectingState(key);
			CONNECTION_MANAGER.connect(requestMethod, address, key);
		}
	}
}
