package tp.pdc.proxy.handler.state.client;

import java.nio.channels.SelectionKey;

import tp.pdc.proxy.handler.HttpClientProxyHandler;
import tp.pdc.proxy.handler.interfaces.HttpClientState;

/**
 * Client transitional state representing the request processing has finished.
 * Once the request is processed, the client is ready to receive a response from the server.
 * {@link SendingResponseState}
 */
public class RequestProcessedState implements HttpClientState {

	private static final RequestProcessedState INSTANCE = new RequestProcessedState();
	
	private RequestProcessedState() {
	}
	
	public static RequestProcessedState getInstance() {
		return INSTANCE;
	}
	
	@Override
	public void handle(HttpClientProxyHandler httpHandler, SelectionKey key) {
		throw new UnsupportedOperationException("Request processed state handle should never be called");
	}

}
