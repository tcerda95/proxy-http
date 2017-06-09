package tp.pdc.proxy.handler.interfaces;

import tp.pdc.proxy.handler.HttpServerProxyHandler;

import java.nio.channels.SelectionKey;

public interface HttpServerState {

	/**
	 * Action to be performed when the proxy is in a certain state
	 * @param handler Server proxy handler
	 * @param key server's key
     */
	public void handle (HttpServerProxyHandler handler, SelectionKey key);
}
