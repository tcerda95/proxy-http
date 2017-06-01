package tp.pdc.proxy.handler.state.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tp.pdc.proxy.ConnectionManager;
import tp.pdc.proxy.HttpErrorCode;
import tp.pdc.proxy.handler.HttpClientProxyHandler;
import tp.pdc.proxy.handler.interfaces.HttpClientState;
import tp.pdc.proxy.header.Method;
import tp.pdc.proxy.parser.HostParser;
import tp.pdc.proxy.parser.interfaces.HttpRequestParser;

public class NotConnectedState implements HttpClientState {
	private static final Logger LOGGER = LoggerFactory.getLogger(NotConnectedState.class);
	private static final HostParser HOST_PARSER = new HostParser();
	private static final ConnectionManager CONNECTION_MANAGER = ConnectionManager.getInstance();

	private static final NotConnectedState INSTANCE = new NotConnectedState();
	
	private NotConnectedState() {
	}
	
	public static NotConnectedState getInstance() {
		return INSTANCE;
	}
	
	@Override
	public void handle(HttpClientProxyHandler httpHandler, SelectionKey key) {
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
				httpHandler.setErrorState(HttpErrorCode.BAD_HOST_FORMAT_400, key);
				return;
			}
			
			try {
				tryConnect(httpHandler, requestParser.getMethod(), address, key);
			} catch (IOException e) {
				LOGGER.warn("Failed to connect to server: {}", e.getMessage());
				httpHandler.setErrorState(HttpErrorCode.BAD_GATEWAY_502, e.getMessage(), key);
			}
		}
		
		else if (!processedBuffer.hasRemaining()) {
			LOGGER.warn("Client's processed buffer full and connection not established with server");
			httpHandler.setErrorState(HttpErrorCode.HEADER_FIELDS_TOO_LARGE_431, key);
		}		
		
		else if (requestParser.hasFinished()) {
			LOGGER.warn("Impossible to connect: host not found in request header nor URL");
			httpHandler.setErrorState(HttpErrorCode.NO_HOST_400, key);
		}
	}
	
	private void tryConnect(HttpClientProxyHandler httpHandler, Method requestMethod, InetSocketAddress address, SelectionKey key) throws IOException {
		
		LOGGER.debug("Server address: {}", address);
		
		if (address.isUnresolved()) {
			LOGGER.warn("Failed to resolve address: {}", address.getHostString());
			httpHandler.setErrorState(HttpErrorCode.UNRESOLVED_ADDRESS_400, key);
		}
		else {
			httpHandler.setConnectingState(key);
			CONNECTION_MANAGER.connect(requestMethod, address, key);		
		}
	}
}
