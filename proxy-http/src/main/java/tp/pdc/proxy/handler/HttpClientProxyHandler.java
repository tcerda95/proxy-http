package tp.pdc.proxy.handler;


import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tp.pdc.proxy.HttpErrorCode;
import tp.pdc.proxy.exceptions.IllegalHttpHeadersException;
import tp.pdc.proxy.exceptions.ParserFormatException;
import tp.pdc.proxy.handler.interfaces.HttpClientState;
import tp.pdc.proxy.handler.state.client.ConnectedState;
import tp.pdc.proxy.handler.state.client.ConnectingState;
import tp.pdc.proxy.handler.state.client.LastWriteCloseConnection;
import tp.pdc.proxy.handler.state.client.LastWriteKeepConnection;
import tp.pdc.proxy.handler.state.client.NotConnectedState;
import tp.pdc.proxy.handler.state.client.RequestProcessedState;
import tp.pdc.proxy.handler.state.client.SendingResponseState;
import tp.pdc.proxy.header.BytesUtils;
import tp.pdc.proxy.header.Header;
import tp.pdc.proxy.header.HeaderValue;
import tp.pdc.proxy.header.Method;
import tp.pdc.proxy.metric.ClientMetricImpl;
import tp.pdc.proxy.metric.interfaces.ClientMetric;
import tp.pdc.proxy.parser.factory.HttpBodyParserFactory;
import tp.pdc.proxy.parser.factory.HttpRequestParserFactory;
import tp.pdc.proxy.parser.interfaces.HttpBodyParser;
import tp.pdc.proxy.parser.interfaces.HttpRequestParser;

public class HttpClientProxyHandler extends HttpHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(HttpClientProxyHandler.class);
	private static final HttpBodyParserFactory BODY_PARSER_FACTORY = HttpBodyParserFactory.getInstance();
	private static final HttpRequestParserFactory REQUEST_PARSER_FACTORY = HttpRequestParserFactory.getInstance();
	private static final ClientMetric CLIENT_METRICS = ClientMetricImpl.getInstance();
	
	private HttpRequestParser requestParser;
	private HttpBodyParser bodyParser;
	private Set<Method> acceptedMethods;
	private boolean methodRecorded;
	private boolean errorState;
	
	private HttpClientState state;
	
	public HttpClientProxyHandler(int bufSize, Set<Method> acceptedMethods) {
		super(bufSize, ByteBuffer.allocate(bufSize), ByteBuffer.allocate(bufSize));
		this.acceptedMethods = acceptedMethods;
		this.state = NotConnectedState.getInstance();
		this.requestParser = REQUEST_PARSER_FACTORY.getRequestParser();
	}
	
	public void reset(SelectionKey key) {
		this.bodyParser = null;
		this.methodRecorded = false;
		this.state = NotConnectedState.getInstance();
		this.requestParser.reset();
		
		setConnectedPeerKey(null);
		key.interestOps(SelectionKey.OP_READ);
	}

	public boolean hasFinishedProcessing() {
		return requestParser.hasFinished() && bodyParser.hasFinished();
	}
	
	public HttpRequestParser getRequestParser() {
		return requestParser;
	}
	
	public HttpBodyParser getBodyParser() {
		return bodyParser;
	}
	
	public void signalResponseProcessed(boolean closeConnectionToClient) {
		if (closeConnectionToClient || !shouldKeepConnectionAlive())
			this.state = LastWriteCloseConnection.getInstance();
		else
			this.state = LastWriteKeepConnection.getInstance();
	}
	
	public void signalRequestSent() {
		this.state = SendingResponseState.getInstance();
	}
	
	public void setConnectingState(SelectionKey key) {
		LOGGER.debug("Unregistering client key: connecting");
		key.interestOps(0);
		this.state = ConnectingState.getInstance();
	}
	
	public void handleConnect(SelectionKey key) {
		if (hasFinishedProcessing())
			setRequestProcessedState(key);
		else
			setConnectedState(key);
	}
	
	public void setRequestProcessedState(SelectionKey key) {
		LOGGER.debug("Unregistering client from read: request processed");
		key.interestOps(0);
		
		LOGGER.debug("Registering server for write and signaling end of request");
		this.getConnectedPeerKey().interestOps(SelectionKey.OP_WRITE);
		
		this.state = RequestProcessedState.getInstance();
		getServerHandler().signalRequestProcessed();
	}
	
	public void setConnectedState(SelectionKey key) {
		this.state = ConnectedState.getInstance();
		LOGGER.debug("Registering client for read and server for write: client in connected state");
		key.interestOps(SelectionKey.OP_READ);
		this.getConnectedPeerKey().interestOps(SelectionKey.OP_WRITE);
	}
	
	public void setErrorState(HttpErrorCode errorResponse, SelectionKey key) {
		ByteBuffer writeBuffer = this.getWriteBuffer();
		writeBuffer.clear();
		writeBuffer.put(errorResponse.getBytes());
		
		setErrorState(key);
	}
	
	public void setErrorState(SelectionKey key) {
		errorState = true;
		this.state = LastWriteCloseConnection.getInstance();
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
			
			state.handle(this, key);
			
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
				LOGGER.info("Received EOF from client");
				socketChannel.close();  // TODO: no podría mandar -1 el cliente pero querer seguir leyendo? SI
				closeServerChannel();
			}
			else {
				LOGGER.info("Read {} bytes from client", bytesRead);
				CLIENT_METRICS.addBytesRead(bytesRead);
			}
		} catch (IOException e) {
			LOGGER.warn("Failed to read from client: {}", e.getMessage());
			closeServerChannel();
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
		
		if (!errorState)
			state.handle(this, key);			
	}
	
	private void processBody(ByteBuffer inputBuffer, ByteBuffer outputBuffer, SelectionKey key) {
		try {
			bodyParser.parse(inputBuffer, outputBuffer);
		} catch (ParserFormatException e) {
			LOGGER.warn("Invalid body format: {}", e.getMessage());
			setErrorState(HttpErrorCode.BAD_REQUEST_400, key);
		}
	}
	
	private void processRequest(ByteBuffer inputBuffer, ByteBuffer outputBuffer, SelectionKey key) {
		try {			
			requestParser.parse(inputBuffer, outputBuffer);

			if (requestParser.hasMethod() && !methodRecorded) {
				CLIENT_METRICS.addMethodCount(requestParser.getMethod());
				methodRecorded = true;
				
				if (!acceptedMethods.contains(requestParser.getMethod())) {					
					LOGGER.warn("Client's method not supported: {}", requestParser.getMethod());
					setErrorState(HttpErrorCode.METHOD_NOT_ALLOWED_405, key);
				}
			}
			
			if (requestParser.hasFinished() && !errorState) {
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
		
	public HttpServerProxyHandler getServerHandler() {
		
		if (this.getConnectedPeerKey() == null) {
			LOGGER.error("Asking for server handler when there is none!");
			throw new IllegalStateException("Connection not established yet, no server handler available");
		}
		
		return (HttpServerProxyHandler) this.getConnectedPeerKey().attachment();
	}
}
