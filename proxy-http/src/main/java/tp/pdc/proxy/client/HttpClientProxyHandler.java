package tp.pdc.proxy.client;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tp.pdc.proxy.HeadersParser;
import tp.pdc.proxy.HttpHandler;
import tp.pdc.proxy.HttpResponse;
import tp.pdc.proxy.HttpServerProxyHandler;
import tp.pdc.proxy.Parser;
import tp.pdc.proxy.ProxyProperties;
import tp.pdc.proxy.RequestMethod;
import tp.pdc.proxy.header.Header;

public class HttpClientProxyHandler extends HttpHandler {
	private final static Logger LOGGER = LoggerFactory.getLogger(HttpClientProxyHandler.class);
	private static final ProxyProperties PROPERTIES = ProxyProperties.getInstance();
	private static final HostParser HOST_PARSER = new HostParser();
	
	private ClientHandlerState state;
	private HeadersParser headersParser;
	private Parser contentParser;
	
	public HttpClientProxyHandler(int bufSize) {
		super(bufSize, ByteBuffer.allocate(bufSize), ByteBuffer.allocate(bufSize));
		state = ClientHandlerState.NOT_CONNECTED;
	}
	
	@Override
	protected void processRead(SelectionKey key) {
		SocketChannel socketChannel = (SocketChannel) key.channel();
		ByteBuffer buffer = this.getReadBuffer();
		
		try {
			socketChannel.read(buffer);
		} catch (IOException e) {
			// TODO No se pudo leer del cliente
			LOGGER.warn("Failed to read from client: {}", e.getMessage());
			e.printStackTrace();
		}
	}

	@Override
	protected void process(ByteBuffer inputBuffer, SelectionKey key) {
		ByteBuffer processedBuffer = this.getProcessedBuffer();
		
		if (headersParser.hasFinished()) {
			contentParser.parse(inputBuffer, processedBuffer);
		}
		else {
			headersParser.parse(inputBuffer, processedBuffer);
			
			if (state == ClientHandlerState.NOT_CONNECTED && headersParser.hasHeaderValue(Header.HOST)) {
				byte[] hostBytes = headersParser.getHeaderValue(Header.HOST);
				
				try {
					tryConnect(hostBytes, key);
				} catch (NumberFormatException e) {
					LOGGER.warn("Failed to parse port {}", e.getMessage());
					manageError(HttpResponse.BAD_REQUEST_400, key);
				} catch (IllegalArgumentException e) {
					LOGGER.warn("{}", e.getMessage());
					manageError(HttpResponse.BAD_REQUEST_400, key);
				} catch (IOException e) {
					LOGGER.warn("Failed to connect to server: {}", e.getMessage());
					manageError(HttpResponse.BAD_GATEWAY_502, key);
				}
			}
			
			if (headersParser.hasFinished() && headersParser.getMethod() != RequestMethod.POST && state == ClientHandlerState.CONNECTED || state == ClientHandlerState.CONNECTING)
				state = ClientHandlerState.REQUEST_PROCESSED;
		}		
	}


	@Override
	protected void processWrite(ByteBuffer inputBuffer, SelectionKey key) {
		SocketChannel socketChannel = (SocketChannel) key.channel();
		try {
			socketChannel.write(inputBuffer);
			if (!inputBuffer.hasRemaining() && state == ClientHandlerState.ERROR)
				socketChannel.close();
			if (!inputBuffer.hasRemaining() && state == ClientHandlerState.REQUEST_PROCESSED) {
				state = ClientHandlerState.REQUEST_SENT;
				key.interestOps(SelectionKey.OP_WRITE);
			}
		} catch (IOException e) {
			e.printStackTrace();
			// No se le pudo responder al cliente
		}
	}
	
	private void manageError(HttpResponse errorResponse, SelectionKey key) {
		state = ClientHandlerState.ERROR;
		this.getWriteBuffer().put(errorResponse.getBytes());
		key.interestOps(SelectionKey.OP_WRITE);
	}

	private void tryConnect(byte[] hostBytes, SelectionKey key) throws IOException {
		InetSocketAddress address = HOST_PARSER.parseAddress(hostBytes);
		Selector selector = key.selector();
		SocketChannel serverSocket = SocketChannel.open();
		SelectionKey serverKey;
		
		serverSocket.configureBlocking(false);
		
		if (serverSocket.connect(address)) {
			serverKey = serverSocket.register(selector, SelectionKey.OP_WRITE, buildHttpServerProxyHandler(key));
			state = ClientHandlerState.CONNECTED;
		}
		else {
			serverKey = serverSocket.register(selector, SelectionKey.OP_CONNECT, buildHttpServerProxyHandler(key));
			state = ClientHandlerState.CONNECTING;
		}
		
		this.setConnectedPeerKey(serverKey);
	}
	
	private HttpServerProxyHandler buildHttpServerProxyHandler(SelectionKey key) {
		HttpServerProxyHandler handler = new HttpServerProxyHandler(PROPERTIES.getBufferSize(), this.getProcessedBuffer(), this.getWriteBuffer());
		handler.setConnectedPeerKey(key);
		return handler;
	}	
}
