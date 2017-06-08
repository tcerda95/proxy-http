package tp.pdc.proxy.handler.state.client;

import java.nio.channels.SelectionKey;

import tp.pdc.proxy.handler.HttpClientProxyHandler;
import tp.pdc.proxy.handler.interfaces.HttpClientState;

/**
 * Transitional client state which represent the connection process of a client and a server.
 * Client's request is not being read in this state.
 * After this state, it sets client's state to {@link ConnectedState}.
 */
public class ConnectingState implements HttpClientState {
	
	private static final ConnectingState INSTANCE = new ConnectingState();
	
	private ConnectingState() {
	}
	
	public static ConnectingState getInstance() {
		return INSTANCE;
	}
	
	@Override
	public void handle(HttpClientProxyHandler httpHandler, SelectionKey key) {
		throw new UnsupportedOperationException("Connecting state handler should never be called");
	}

}
