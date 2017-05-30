package tp.pdc.proxy.handler.state.client;

import java.nio.channels.SelectionKey;

import tp.pdc.proxy.handler.HttpClientProxyHandler;
import tp.pdc.proxy.handler.interfaces.HttpClientState;

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
