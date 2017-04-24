package tp.pdc.proxy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.Iterator;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Pruebita {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(Pruebita.class);
	
	public void run(int port) {
		ServerSocketChannel serverChannel;
		Selector selector;
		
		HttpProxySelectorProtocol protocol = new HttpProxySelectorProtocol(4096);
		
		try {
			serverChannel = ServerSocketChannel.open();
			serverChannel.bind(new InetSocketAddress(port));
			serverChannel.configureBlocking(false);
			selector = Selector.open();
			serverChannel.register(selector, SelectionKey.OP_ACCEPT);
		}
		catch (IOException e) {
			e.printStackTrace();
			return;
		}
		
		while (true) {
			try {
				selector.select();
			}
			catch (IOException e) {
				e.printStackTrace();
				break;
			}
			
			Set<SelectionKey> keySet = selector.selectedKeys();			
			Iterator<SelectionKey> keyIter = keySet.iterator();
			while (keyIter.hasNext()) {
				SelectionKey key = keyIter.next();
				keyIter.remove();
				
				if (key.isAcceptable()) {
					LOGGER.debug("Key acceptable");
					protocol.handleAccept(key);
				}
								
				if (key.isConnectable()) {
					LOGGER.debug("Key connectable");
					protocol.handleConnect(key);
				}
				
				if (key.isValid() && key.isReadable()) {
					LOGGER.debug("Key readable");
					protocol.handleRead(key);
				}
				
				if (key.isValid() && key.isWritable()) {  // OJO: tratar casos en los que el servidor pudo haber cerrado la conexión
					LOGGER.debug("Key writable");
					protocol.handleWrite(key);
				}
			}
		}
	}
	
	public static void main(String[] args) {
		new Pruebita().run(9090);
	}
}
