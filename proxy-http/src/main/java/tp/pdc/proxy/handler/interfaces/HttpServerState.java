package tp.pdc.proxy.handler.interfaces;

import tp.pdc.proxy.handler.HttpServerProxyHandler;

import java.nio.channels.SelectionKey;

public interface HttpServerState {
	public void handle (HttpServerProxyHandler handler, SelectionKey key);
}
