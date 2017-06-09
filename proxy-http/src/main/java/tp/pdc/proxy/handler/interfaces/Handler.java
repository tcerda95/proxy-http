package tp.pdc.proxy.handler.interfaces;

import java.nio.channels.SelectionKey;

/**
 * Handler used for the proxy protocol
 */
public interface Handler {

	/**
	 * Read request from protocol client
	 * @param key client's key
     */
	public void handleRead (SelectionKey key);

	/**
	 * Sends request to protocol client
	 * @param key client's key
     */
	public void handleWrite (SelectionKey key);
}
