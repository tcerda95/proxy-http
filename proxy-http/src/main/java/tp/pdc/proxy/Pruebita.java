package tp.pdc.proxy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tp.pdc.proxy.handler.interfaces.Handler;
import tp.pdc.proxy.handler.supplier.HttpClientProxyHandlerSupplier;
import tp.pdc.proxy.handler.supplier.ProtocolHandlerSupplier;

public class Pruebita {
	private static final Logger LOGGER = LoggerFactory.getLogger(Pruebita.class);
	private static final ProxyProperties PROPERTIES = ProxyProperties.getInstance();
	private static final ConnectionManager CONNECTION_MANAGER = ConnectionManager.getInstance();
	
	private final Selector selector;
	private final int proxyPort;
	private final int protocolPort;
			
	public Pruebita(int proxyPort, int protocolPort) throws IOException {
		LOGGER.info("Setting up proxy...");

		selector = Selector.open();
		this.proxyPort = proxyPort;
		this.protocolPort = protocolPort;
		
		registerChannel(proxyPort, HttpClientProxyHandlerSupplier.getInstance());
		registerChannel(protocolPort, ProtocolHandlerSupplier.getInstance());
		
		LOGGER.info("Setup done"); 
	}
	
	private void registerChannel(int port, Supplier<? extends Handler> supplier) throws IOException {
		ServerSocketChannel serverChannel = ServerSocketChannel.open();
		serverChannel.bind(new InetSocketAddress(port));
		serverChannel.configureBlocking(false);
		serverChannel.register(selector, SelectionKey.OP_ACCEPT, supplier);
	}

	public void run() {				
		HttpProxySelectorProtocol protocol = new HttpProxySelectorProtocol();
		
		LOGGER.info("Accepting proxy connections from port: {}", proxyPort);
		LOGGER.info("COMING SOON: accepting protocol connections from port: {}", protocolPort);
		
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
			
			CONNECTION_MANAGER.clean();
		}
	}
	
	public static void main(String[] args) throws IOException {
		new Pruebita(PROPERTIES.getProxyPort(), PROPERTIES.getProtocolPort()).run();
	}
}
