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
	private static final ProxyProperties PROPERTIES = ProxyProperties.getInstance();
	
	public void run(int port) {
		ServerSocketChannel serverChannel;
		Selector selector;
		
		LOGGER.info("Setting up proxy...");
		
		HttpProxySelectorProtocol protocol = new HttpProxySelectorProtocol(PROPERTIES.getProxyBufferSize());
		
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
		
		LOGGER.info("Setup done. Accepting connections from port: {}", port);
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
								
				if (key.isValid() && key.isAcceptable()) {
					protocol.handleAccept(key);
				}
								
				if (key.isValid() && key.isConnectable()) {
					protocol.handleConnect(key);
				}
				
				if (key.isValid() && key.isReadable()) {
					protocol.handleRead(key);
				}
				
				if (key.isValid() && key.isWritable()) {  // OJO: tratar casos en los que el servidor pudo haber cerrado la conexión
					protocol.handleWrite(key);
				}
			}
		}
	}
	
	public static void main(String[] args) {
		new Pruebita().run(PROPERTIES.getProxyPort());
	}
}
