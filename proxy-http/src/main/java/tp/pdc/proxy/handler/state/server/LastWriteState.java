package tp.pdc.proxy.handler.state.server;

import tp.pdc.proxy.handler.HttpServerProxyHandler;
import tp.pdc.proxy.handler.interfaces.HttpServerState;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;

/**
 * Server state that respresents the last time the proxy sends data to the server.
 * Once it is sent, it sets the server state to {@link ReadResponseState}
 */
public class LastWriteState implements HttpServerState {

	private static final LastWriteState INSTANCE = new LastWriteState();

	private LastWriteState () {
	}

	public static LastWriteState getInstance () {
		return INSTANCE;
	}

	@Override
	public void handle (HttpServerProxyHandler handler, SelectionKey key) {
		ByteBuffer writeBuffer = handler.getWriteBuffer();

		if (!writeBuffer.hasRemaining()) {
			handler.getClientHandler().signalRequestSent();
			handler.setReadResponseState(key);
		}
	}
}
