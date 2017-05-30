package tp.pdc.proxy.handler.interfaces;

import java.nio.channels.SelectionKey;

import tp.pdc.proxy.handler.HttpServerProxyHandler;

public interface HttpServerState {
	public void handle(HttpServerProxyHandler handler, SelectionKey key);
}
