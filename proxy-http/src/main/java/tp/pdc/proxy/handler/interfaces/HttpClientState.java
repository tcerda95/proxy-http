package tp.pdc.proxy.handler.interfaces;

import java.nio.channels.SelectionKey;

import tp.pdc.proxy.handler.HttpClientProxyHandler;

public interface HttpClientState {
	public void handle(HttpClientProxyHandler httpHandler, SelectionKey key);
}
