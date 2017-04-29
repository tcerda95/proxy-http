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
import tp.pdc.proxy.exceptions.IllegalHttpHeadersException;
import tp.pdc.proxy.exceptions.ParserFormatException;
import tp.pdc.proxy.header.Header;
import tp.pdc.proxy.parser.HttpRequestParserImpl;
import tp.pdc.proxy.parser.factory.HttpBodyParserFactory;
import tp.pdc.proxy.parser.interfaces.HttpBodyParser;
import tp.pdc.proxy.parser.interfaces.HttpRequestParser;
import tp.pdc.proxy.server.HttpServerProxyHandler;

public class HttpClientProxyHandler extends HttpHandler {
	private final static Logger LOGGER = LoggerFactory.getLogger(HttpClientProxyHandler.class);
	private static final ProxyProperties PROPERTIES = ProxyProperties.getInstance();
	private static final HostParser HOST_PARSER = new HostParser();
	
	private ClientHandlerState state;
	private HttpRequestParser headersParser;
	private HttpBodyParser bodyParser;
	
	public HttpClientProxyHandler(int bufSize) {
		super(bufSize, ByteBuffer.allocate(bufSize), ByteBuffer.allocate(bufSize));
		state = ClientHandlerState.NOT_CONNECTED;
		headersParser = new HttpRequestParserImpl();
	}
	
	public void setConnectedState() {
		if (state == ClientHandlerState.CONNECTING)
			state = ClientHandlerState.CONNECTED;
		else if (state == ClientHandlerState.REQUEST_PROCESSED_CONNECTING)
			state = ClientHandlerState.REQUEST_PROCESSED;
		else {
			LOGGER.error("Invalid client state: {}", state);
			throw new IllegalStateException("State must be " + ClientHandlerState.CONNECTING + " or " + ClientHandlerState.REQUEST_PROCESSED_CONNECTING);
		}
	}
	
	public void setRequestSentState() {
		if (state == ClientHandlerState.REQUEST_PROCESSED) {
			LOGGER.debug("Complete request sent");
			state = ClientHandlerState.REQUEST_SENT;
		}
		else {
			LOGGER.error("Invalid client state: {}", state);
			throw new IllegalStateException("State must be " + ClientHandlerState.REQUEST_PROCESSED);
		}
	}
	
	public ClientHandlerState getState() {
		return state;
	}
	
	@Override
	protected void processWrite(ByteBuffer inputBuffer, SelectionKey key) {
		SocketChannel socketChannel = (SocketChannel) key.channel();
		
		try {
			socketChannel.write(inputBuffer);
			
			if (!inputBuffer.hasRemaining()) {
				key.interestOps(0);			
				if (state == ClientHandlerState.ERROR)
					socketChannel.close();
				else if (isServerResponseProcessed()) {
					LOGGER.info("Server response processed and sent: closing connection to client");
					socketChannel.close();
				}
			}
			
			if (state != ClientHandlerState.ERROR && !isServerResponseProcessed()) {
				LOGGER.debug("Registering server for read: server's response not processed yet");
				this.getConnectedPeerKey().interestOps(SelectionKey.OP_READ);
			}
			
		} catch (IOException e) {
			LOGGER.warn("Failed to write to client: {}", e.getMessage());
			e.printStackTrace();
		}
	}
	
	@Override
	protected void processRead(SelectionKey key) {
		SocketChannel socketChannel = (SocketChannel) key.channel();
		ByteBuffer buffer = this.getReadBuffer();
		
		try {
			if (socketChannel.read(buffer) == -1)
				socketChannel.close();  // TODO: no podría mandar -1 el cliente pero querer seguir leyendo?
		} catch (IOException e) {
			LOGGER.warn("Failed to read from client: {}", e.getMessage());
			e.printStackTrace();
		}
	}
	
	@Override
	protected void process(ByteBuffer inputBuffer, SelectionKey key) {
		ByteBuffer processedBuffer = this.getProcessedBuffer();
		
		if (headersParser.hasFinished())
			processBody(inputBuffer, processedBuffer, key);
		else
			processHeaders(inputBuffer, processedBuffer, key);
		
		if (state == ClientHandlerState.ERROR)
			return;
			
		if (!processedBuffer.hasRemaining()) {
			LOGGER.debug("Unregistering client from read: processed buffer full");
			key.interestOps(0);				
		}
		
		if (state.shouldWrite()) {
			LOGGER.debug("Registering server for write. Must send {} bytes", processedBuffer.position());
			this.getConnectedPeerKey().interestOps(SelectionKey.OP_WRITE);
		}
	}
	
	private void processBody(ByteBuffer inputBuffer, ByteBuffer outputBuffer, SelectionKey key) {
		try {
			bodyParser.parse(inputBuffer, outputBuffer);
		} catch (ParserFormatException e) {
			LOGGER.warn("Invalid body format: {}", e.getMessage());
			setErrorState(HttpResponse.BAD_REQUEST_400, key);
			return;
		}
		
		if (bodyParser.hasFinished())
			setRequestProcessedState(key);		
	}
	
	private void processHeaders(ByteBuffer inputBuffer, ByteBuffer outputBuffer, SelectionKey key) {
		try {
			headersParser.parse(inputBuffer, outputBuffer);
			
			if (state == ClientHandlerState.NOT_CONNECTED) {
				manageNotConnected(key);
				if (state == ClientHandlerState.ERROR)
					return;
			}
			
			if (headersParser.hasFinished()) {
				bodyParser = HttpBodyParserFactory.getClientHttpBodyParser(headersParser, headersParser.getMethod());
				processBody(inputBuffer, outputBuffer, key);
			}

		} catch (ParserFormatException e) {
			LOGGER.warn("Invalid header format: {}", e.getMessage());
			setErrorState(HttpResponse.BAD_REQUEST_400, key);

		} catch (IllegalHttpHeadersException e) {
			LOGGER.warn("Illegal request headers: {}", e.getMessage());
			setErrorState(HttpResponse.BAD_REQUEST_400, key);
		}
	}
	
	private void setRequestProcessedState(SelectionKey key) {
		key.interestOps(0);
		
		if (state == ClientHandlerState.CONNECTING)
			state = ClientHandlerState.REQUEST_PROCESSED_CONNECTING;
		else if (state == ClientHandlerState.CONNECTED)
			state = ClientHandlerState.REQUEST_PROCESSED;
		else {
			LOGGER.error("Cannot set request processed state: invalid client state");
			throw new IllegalStateException("State must be " + ClientHandlerState.CONNECTING + " or " + ClientHandlerState.CONNECTING);
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
			try {
				tryConnect(hostBytes, key);
			} catch (NumberFormatException e) {
				LOGGER.warn("Failed to parse port: {}", e.getMessage());
				setErrorState(HttpResponse.BAD_REQUEST_400, key);
			} catch (IllegalArgumentException e) {
				LOGGER.warn("{}", e.getMessage());
				setErrorState(HttpResponse.BAD_REQUEST_400, key);
			} catch (IOException e) {
				LOGGER.warn("Failed to connect to server: {}", e.getMessage());
				setErrorState(HttpResponse.BAD_GATEWAY_502, key);
			}
		}
		
		else if (!processedBuffer.hasRemaining()) {
			LOGGER.warn("Client's processed buffer full and connection not established with server");
			setErrorState(HttpResponse.BAD_REQUEST_400, key);
		}		
		
		else if (headersParser.hasFinished()) {
			LOGGER.warn("Impossible to connect: host not found in request header");
			setErrorState(HttpResponse.BAD_REQUEST_400, key);
		}
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
