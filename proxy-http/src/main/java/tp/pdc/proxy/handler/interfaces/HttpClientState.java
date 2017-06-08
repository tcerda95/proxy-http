package tp.pdc.proxy.handler.interfaces;

import tp.pdc.proxy.handler.HttpClientProxyHandler;

import java.nio.channels.SelectionKey;

public interface HttpClientState {
	public void handle (HttpClientProxyHandler httpHandler, SelectionKey key);
}
