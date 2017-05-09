package tp.pdc.proxy.handler;

import static tp.pdc.proxy.handler.ClientHandlerState.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tp.pdc.proxy.HttpErrorCode;
import tp.pdc.proxy.exceptions.IllegalHttpHeadersException;
import tp.pdc.proxy.exceptions.ParserFormatException;
import tp.pdc.proxy.header.BytesUtils;
import tp.pdc.proxy.header.Header;
import tp.pdc.proxy.header.HeaderValue;
import tp.pdc.proxy.header.Method;
import tp.pdc.proxy.metric.ClientMetricImpl;
import tp.pdc.proxy.metric.ServerMetricImpl;
import tp.pdc.proxy.metric.interfaces.ClientMetric;
import tp.pdc.proxy.parser.HostParser;
import tp.pdc.proxy.parser.factory.HttpBodyParserFactory;
import tp.pdc.proxy.parser.factory.HttpRequestParserFactory;
import tp.pdc.proxy.parser.interfaces.HttpBodyParser;
import tp.pdc.proxy.parser.interfaces.HttpRequestParser;

public class HttpClientProxyHandler extends HttpHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(HttpClientProxyHandler.class);
	private static final HostParser HOST_PARSER = new HostParser();
	private static final HttpBodyParserFactory BODY_PARSER_FACTORY = HttpBodyParserFactory.getInstance();
	private static final HttpRequestParserFactory REQUEST_PARSER_FACTORY = HttpRequestParserFactory.getInstance();
	private static final ClientMetric CLIENT_METRICS = ClientMetricImpl.getInstance();
	
	private ClientHandlerState state;
	private HttpRequestParser requestParser;
	private HttpBodyParser bodyParser;
	private Set<Method> acceptedMethods;
	private boolean methodRecorded;
	
	public HttpClientProxyHandler(int bufSize, Set<Method> acceptedMethods) {
		super(bufSize, ByteBuffer.allocate(bufSize), ByteBuffer.allocate(bufSize));
		this.acceptedMethods = acceptedMethods;
		this.state = NOT_CONNECTED;
		this.requestParser = REQUEST_PARSER_FACTORY.getRequestParser();
	}
	
	public void reset(SelectionKey key) {
		this.bodyParser = null;
		this.methodRecorded = false;
		this.state = NOT_CONNECTED;
		this.requestParser.reset();
		key.interestOps(SelectionKey.OP_READ);
	}
		
	public ClientHandlerState getState() {
		return state;
	}
	
	public void setConnectedState() {
		if (state == CONNECTING)
			state = CONNECTED;
		else if (state == REQUEST_PROCESSED_CONNECTING)
			state = REQUEST_PROCESSED;
		else {
			LOGGER.error("Invalid client state: {}", state);
			throw new IllegalStateException("State must be " + CONNECTING + " or " + REQUEST_PROCESSED_CONNECTING);
		}
	}
	
	public void setRequestSentState() {
		if (state == REQUEST_PROCESSED) {
			LOGGER.debug("Complete request sent");
			state = REQUEST_SENT;
		}
		else {
			LOGGER.error("Invalid client state: {}", state);
			throw new IllegalStateException("State must be " + REQUEST_PROCESSED);
		}
	}
	
	public void setErrorState(HttpErrorCode errorResponse, SelectionKey key) {		
		state = ERROR;
		
		ByteBuffer writeBuffer = this.getWriteBuffer();
		writeBuffer.clear();
		writeBuffer.put(errorResponse.getBytes());
		
		key.interestOps(SelectionKey.OP_WRITE);
		
		closeServerChannel();
	}
	
	@Override
	protected void processWrite(ByteBuffer inputBuffer, SelectionKey key) {
		SocketChannel socketChannel = (SocketChannel) key.channel();
		
		try {
			int bytesSent = socketChannel.write(inputBuffer);
			
			LOGGER.info("Sent {} bytes to client", bytesSent);
			CLIENT_METRICS.addBytesWritten(bytesSent);
			
			if (!inputBuffer.hasRemaining()) {
				LOGGER.debug("Unregistering client: write buffer empty");
				key.interestOps(0);			
				if (state == ERROR) {
					socketChannel.close();
					LOGGER.info("Closing connection to client: error message sent");
				}
				else if (isServerResponseProcessed()) {
					if (shouldKeepConnectionAlive()) {
						LOGGER.info("Keeping alive connection: server response processed and sent");
						reset(key);
					}
					else {
						LOGGER.info("Closing connection to client: server response processed and sent");
						socketChannel.close();
					}
				}
			}
			
			if (state != ERROR && !isServerResponseProcessed()) {
				LOGGER.debug("Registering server for read: server's response not processed yet");
				this.getConnectedPeerKey().interestOps(SelectionKey.OP_READ);
				getServerHandler().handleProcess(getConnectedPeerKey());  // Espacio libre en processedBuffer ---> servidor puede procesar
			}
			
		} catch (IOException e) {
			LOGGER.warn("Failed to write to client: {}", e.getMessage());
			e.printStackTrace();
			closeServerChannel();
			try {
				socketChannel.close();
			} catch (IOException e1) {
				LOGGER.error("Failed to close client's channel on client's write error");
				e1.printStackTrace();
			}
		}
	}
	
	private boolean shouldKeepConnectionAlive() {
		if (requestParser.hasHeaderValue(Header.CONNECTION))
			return BytesUtils.equalsBytes(requestParser.getHeaderValue(Header.CONNECTION), HeaderValue.KEEP_ALIVE.getValue());
		else if (requestParser.hasHeaderValue(Header.PROXY_CONNECTION))
			return BytesUtils.equalsBytes(requestParser.getHeaderValue(Header.PROXY_CONNECTION), HeaderValue.KEEP_ALIVE.getValue());
		return false;
	}

	@Override
	protected void processRead(SelectionKey key) {
		SocketChannel socketChannel = (SocketChannel) key.channel();
		ByteBuffer buffer = this.getReadBuffer();
		
		try {
			int bytesRead = socketChannel.read(buffer);
			if (bytesRead == -1) {
				LOGGER.warn("Received EOF from client");
				socketChannel.close();  // TODO: no podría mandar -1 el cliente pero querer seguir leyendo? SI
				closeServerChannel();
			}
			else {
				LOGGER.info("Read {} bytes from client", bytesRead);
				CLIENT_METRICS.addBytesRead(bytesRead);
			}
		} catch (IOException e) {
			LOGGER.warn("Failed to read from client: {}", e.getMessage());
			e.printStackTrace();
		}
	}
	
	@Override
	protected void process(ByteBuffer inputBuffer, SelectionKey key) {
		ByteBuffer processedBuffer = this.getProcessedBuffer();
		
		if (!requestParser.hasFinished())
			processRequest(inputBuffer, processedBuffer, key);
		else if (!bodyParser.hasFinished())
			processBody(inputBuffer, processedBuffer, key);
		
		if (state == ERROR)
			return;
			
		if (!processedBuffer.hasRemaining()) {
			LOGGER.debug("Unregistering client from read: processed buffer full");
			key.interestOps(0);				
		}
		
		if (state.shouldWrite() && processedBuffer.position() != 0) {
			LOGGER.debug("Registering server for write. Must send {} bytes", processedBuffer.position());
			this.getConnectedPeerKey().interestOps(SelectionKey.OP_WRITE);
		}
	}
	
	private void processBody(ByteBuffer inputBuffer, ByteBuffer outputBuffer, SelectionKey key) {
		try {
			bodyParser.parse(inputBuffer, outputBuffer);
			if (bodyParser.hasFinished())
				setRequestProcessedState(key);
		} catch (ParserFormatException e) {
			LOGGER.warn("Invalid body format: {}", e.getMessage());
			setErrorState(HttpErrorCode.BAD_REQUEST_400, key);
			return;
		}
	}
	
	private void processRequest(ByteBuffer inputBuffer, ByteBuffer outputBuffer, SelectionKey key) {
		try {
			requestParser.parse(inputBuffer, outputBuffer);
			
			if (state == NOT_CONNECTED) {
				manageNotConnected(key);
				if (state == ERROR)
					return;
			}
			
			if (requestParser.hasFinished()) {
				bodyParser = BODY_PARSER_FACTORY.getClientHttpBodyParser(requestParser);
				processBody(inputBuffer, outputBuffer, key);
			}

		} catch (ParserFormatException e) {
			if (requestParser.hasMethod() && !acceptedMethods.contains(requestParser.getMethod())) {
				LOGGER.warn("Client's method not supported: {}", requestParser.getMethod());
				setErrorState(HttpErrorCode.METHOD_NOT_ALLOWED_405, key);
			}
			else {
				LOGGER.warn("Invalid header format: {}", e.getMessage());
				setErrorState(HttpErrorCode.BAD_REQUEST_400, key);
			}

		} catch (IllegalHttpHeadersException e) {
			LOGGER.warn("Illegal request headers: {}", e.getMessage());
			setErrorState(HttpErrorCode.LENGTH_REQUIRED_411, key);
		}
	}
	
	private void setRequestProcessedState(SelectionKey key) {
		LOGGER.debug("Unregistering client from read: request processed");
		key.interestOps(0);
		
		if (state == CONNECTING)
			state = REQUEST_PROCESSED_CONNECTING;
		else if (state == CONNECTED)
			state = REQUEST_PROCESSED;
		else {
			LOGGER.error("Cannot set request processed state: invalid client state");
			throw new IllegalStateException("State must be " + CONNECTING + " or " + CONNECTING);
		}
	}
	
	private boolean isServerResponseProcessed() {
		return getServerHandler().isResponseProcessed();
	}
	
	private void closeServerChannel() {
		if (isServerChannelOpen()) {
			try {
				this.getConnectedPeerKey().channel().close();
			} catch (IOException e) {
				LOGGER.error("Failed to close server's connection on client's error");
				e.printStackTrace();
			}
		}
	}
	
	private boolean isServerChannelOpen() {
		return this.getConnectedPeerKey() != null && this.getConnectedPeerKey().channel().isOpen();
	}
	
	private void manageNotConnected(SelectionKey key) {
		ByteBuffer processedBuffer = this.getProcessedBuffer();
		
		if (requestParser.hasMethod() && !methodRecorded) {
			CLIENT_METRICS.addMethodCount(requestParser.getMethod());
			methodRecorded = true;
		}
		
		if (requestParser.hasMethod() && !acceptedMethods.contains(requestParser.getMethod())) {
			LOGGER.warn("Client's method not supported: {}", requestParser.getMethod());
			setErrorState(HttpErrorCode.METHOD_NOT_ALLOWED_405, key);
		}
		
		else if (requestParser.hasHost()) {
			byte[] hostValue = requestParser.getHostValue();
			try {
				tryConnect(hostValue, key);
			} catch (NumberFormatException e) {
				LOGGER.warn("Failed to parse port: {}", e.getMessage());
				setErrorState(HttpErrorCode.BAD_REQUEST_400, key);
			} catch (IllegalArgumentException e) {
				LOGGER.warn("{}", e.getMessage());
				setErrorState(HttpErrorCode.BAD_REQUEST_400, key);
			} catch (IOException e) {
				LOGGER.warn("Failed to connect to server: {}", e.getMessage());
				setErrorState(HttpErrorCode.BAD_GATEWAY_502, key);
			}
		}
		
		else if (!processedBuffer.hasRemaining()) {
			LOGGER.warn("Client's processed buffer full and connection not established with server");
			setErrorState(HttpErrorCode.HEADER_FIELDS_TOO_LARGE_431, key);
		}		
		
		else if (requestParser.hasFinished()) {
			LOGGER.warn("Impossible to connect: host not found in request header nor URL");
			setErrorState(HttpErrorCode.NO_HOST_400, key);
		}
	}

	private void tryConnect(byte[] hostBytes, SelectionKey key) throws IOException {
		InetSocketAddress address = HOST_PARSER.parseAddress(hostBytes);
		Selector selector = key.selector();
		SocketChannel serverSocket = SocketChannel.open();
		SelectionKey serverKey;
				
		serverSocket.configureBlocking(false);
		LOGGER.debug("Server address: {}", address);
		
		if (serverSocket.connect(address)) {
			serverKey = serverSocket.register(selector, SelectionKey.OP_WRITE, buildHttpServerProxyHandler(key));
			ServerMetricImpl.getInstance().addConnection();
			state = CONNECTED;
		}
		else {
			serverKey = serverSocket.register(selector, SelectionKey.OP_CONNECT, buildHttpServerProxyHandler(key));
			state = CONNECTING;
		}
		
		this.setConnectedPeerKey(serverKey);
	}
	
	private HttpServerProxyHandler buildHttpServerProxyHandler(SelectionKey key) {
		HttpServerProxyHandler handler = new HttpServerProxyHandler(this.getProcessedBuffer().capacity(), this.getProcessedBuffer(), this.getWriteBuffer(), requestParser.getMethod());
		handler.setConnectedPeerKey(key);
		return handler;
	}
	
	private HttpServerProxyHandler getServerHandler() {
		return (HttpServerProxyHandler) this.getConnectedPeerKey().attachment();
	}
}
