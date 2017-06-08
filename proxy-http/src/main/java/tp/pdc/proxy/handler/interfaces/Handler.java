package tp.pdc.proxy.handler.interfaces;

import java.nio.channels.SelectionKey;

public interface Handler {
	public void handleRead (SelectionKey key);

	public void handleWrite (SelectionKey key);
}
