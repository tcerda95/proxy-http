package tp.pdc.proxy.handler.interfaces;

import tp.pdc.proxy.handler.HttpClientProxyHandler;

import java.nio.channels.SelectionKey;

public interface HttpClientState {
	/**
	 * Action to be performed when the proxy is in a certain state
	 * @param httpHandler Client proxy handler
	 * @param key client's key
     */
	public void handle (HttpClientProxyHandler httpHandler, SelectionKey key);
}
