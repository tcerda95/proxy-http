package tp.pdc.proxy.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tp.pdc.proxy.HttpHandler;
import tp.pdc.proxy.HttpResponse;
import tp.pdc.proxy.ProxyProperties;
import tp.pdc.proxy.exceptions.ParserFormatException;
import tp.pdc.proxy.header.Header;
import tp.pdc.proxy.header.Method;
import tp.pdc.proxy.parser.HttpRequestParserImpl;
import tp.pdc.proxy.parser.MockParser;
import tp.pdc.proxy.parser.interfaces.HttpRequestParser;
import tp.pdc.proxy.parser.interfaces.Parser;
import tp.pdc.proxy.server.HttpServerProxyHandler;

public class HttpClientProxyHandler extends HttpHandler {
	private final static Logger LOGGER = LoggerFactory.getLogger(HttpClientProxyHandler.class);
	private static final ProxyProperties PROPERTIES = ProxyProperties.getInstance();
	private static final HostParser HOST_PARSER = new HostParser();
	
	private ClientHandlerState state;
	private HttpRequestParser headersParser;
	private Parser bodyParser;
	
	public HttpClientProxyHandler(int bufSize) {
		super(bufSize, ByteBuffer.allocate(bufSize), ByteBuffer.allocate(bufSize));
		state = ClientHandlerState.NOT_CONNECTED;
		headersParser = new HttpRequestParserImpl();
	}
	
	public void setConnectedState() {
		if (this.state == ClientHandlerState.CONNECTING)
			this.state = ClientHandlerState.CONNECTED;
		else if (this.state == ClientHandlerState.REQUEST_PROCESSED_CONNECTING)
			this.state = ClientHandlerState.REQUEST_PROCESSED;
		else {
			LOGGER.error("Invalid client state");
			throw new IllegalStateException("State must be " + ClientHandlerState.CONNECTING + " or " + ClientHandlerState.REQUEST_PROCESSED_CONNECTING);
		}
	}
	
	public ClientHandlerState getState() {
		return state;
	}
	
	public void setRequestSentState() {
		if (state == ClientHandlerState.REQUEST_PROCESSED)
			state = ClientHandlerState.REQUEST_SENT;
		else {
			LOGGER.error("Invalid client state");
			throw new IllegalStateException("State must be " + ClientHandlerState.REQUEST_PROCESSED);
		}
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
			try {
				bodyParser.parse(inputBuffer, processedBuffer);
			} catch (ParserFormatException e) {
				LOGGER.warn("Invalid body format: {}", e.getMessage());
				setErrorState(HttpResponse.BAD_REQUEST_400, key);
				return;
			}
			
			if (bodyParser.hasFinished())
				setRequestProcessedState();
		}
		else {
			try {
				headersParser.parse(inputBuffer, processedBuffer);
			} catch (ParserFormatException e) {
				LOGGER.warn("Invalid header format: {}", e.getMessage());
				setErrorState(HttpResponse.BAD_REQUEST_400, key);
				return;
			}
			
			if (state == ClientHandlerState.NOT_CONNECTED) {
				manageNotConnected(key);
				if (state == ClientHandlerState.ERROR)
					return;
			}
			
			if (headersParser.hasFinished() && headersParser.hasMethod(Method.POST)) {
				// TODO: instanciar body parser de verdad
				bodyParser = new MockParser();
			}
			
			if (headersParser.hasFinished() && !headersParser.hasMethod(Method.POST))
				setRequestProcessedState();
						
			if (state == ClientHandlerState.CONNECTING && headersParser.hasFinished() && !headersParser.hasMethod(Method.POST)) {
				state = ClientHandlerState.REQUEST_PROCESSED_CONNECTING;
				key.interestOps(0);
			}
						
			if (state == ClientHandlerState.CONNECTED && headersParser.hasFinished() && !headersParser.hasMethod(Method.POST)) {
				state = ClientHandlerState.REQUEST_PROCESSED;
				key.interestOps(0);
			}
		}
		
		if (!processedBuffer.hasRemaining()) {
			LOGGER.debug("Unregistering client from read: processed buffer full");
			key.interestOps(0);				
		}
		
		if (state.shouldWrite())
			this.getConnectedPeerKey().interestOps(SelectionKey.OP_WRITE);
	}
	
	private void setRequestProcessedState() {
		if (state == ClientHandlerState.CONNECTING)
			state = ClientHandlerState.REQUEST_PROCESSED_CONNECTING;
		else if (state == ClientHandlerState.CONNECTED)
			state = ClientHandlerState.REQUEST_PROCESSED;
		else {
			LOGGER.error("Invalid client state");
			throw new IllegalStateException("State must be " + ClientHandlerState.CONNECTING + " or " + ClientHandlerState.CONNECTING);
		}
	}
	
	@Override
	protected void processWrite(ByteBuffer inputBuffer, SelectionKey key) {
		SocketChannel socketChannel = (SocketChannel) key.channel();
		
		try {
			socketChannel.write(inputBuffer);
			
			if (!inputBuffer.hasRemaining() && state == ClientHandlerState.ERROR)
				socketChannel.close();
			else if (!inputBuffer.hasRemaining() && isServerResponseProcessed())
				socketChannel.close();
			
		} catch (IOException e) {
			LOGGER.warn("Failed to write to client");
			e.printStackTrace();
			// No se le pudo responder al cliente
		}
	}
	
	private boolean isServerResponseProcessed() {
		SocketChannel socketChannel = (SocketChannel) this.getConnectedPeerKey().channel();
		return !socketChannel.isOpen(); //TODO: esto es porq no tenemos conexiones persistentes con el servidor
	}

	public void setErrorState(HttpResponse errorResponse, SelectionKey key) {
		state = ClientHandlerState.ERROR;
		this.getWriteBuffer().put(errorResponse.getBytes());
		key.interestOps(SelectionKey.OP_WRITE);
	}
	
	private void manageNotConnected(SelectionKey key) {
		ByteBuffer processedBuffer = this.getProcessedBuffer();
		
		if (headersParser.hasHeaderValue(Header.HOST)) {
			byte[] hostBytes = headersParser.getHeaderValue(Header.HOST);
			
			LOGGER.debug("Host length: {}", hostBytes.length);
			LOGGER.debug("Host: {}", new String(hostBytes, PROPERTIES.getCharset()));
			
			try {
				tryConnect(hostBytes, key);
			} catch (NumberFormatException e) {
				LOGGER.warn("Failed to parse port: {}", e.getMessage());
				setErrorState(HttpResponse.BAD_REQUEST_400, key);
				return;
			} catch (IllegalArgumentException e) {
				LOGGER.warn("{}", e.getMessage());
				setErrorState(HttpResponse.BAD_REQUEST_400, key);
				return;
			} catch (IOException e) {
				LOGGER.warn("Failed to connect to server: {}", e.getMessage());
				setErrorState(HttpResponse.BAD_GATEWAY_502, key);
				return;
			}
		}
					
		if (!processedBuffer.hasRemaining()) {
			LOGGER.warn("Buffer full and connection not established with server");
			setErrorState(HttpResponse.BAD_REQUEST_400, key);
			return;
		}
		
		if (headersParser.hasFinished() && !headersParser.hasHeaderValue(Header.HOST)) {
			LOGGER.warn("Impossible to connect: host not found in request header");
			setErrorState(HttpResponse.BAD_REQUEST_400, key);
			return;
		}
	}

	private void tryConnect(byte[] hostBytes, SelectionKey key) throws IOException {
		InetSocketAddress address = HOST_PARSER.parseAddress(hostBytes);
		Selector selector = key.selector();
		SocketChannel serverSocket = SocketChannel.open();
		SelectionKey serverKey;
		
		LOGGER.debug("InetSocketAddress {}", address);
		
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
