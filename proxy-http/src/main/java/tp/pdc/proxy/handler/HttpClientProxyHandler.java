package tp.pdc.proxy.handler;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tp.pdc.proxy.HttpErrorCode;
import tp.pdc.proxy.ProxyLogger;
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
import tp.pdc.proxy.parser.factory.HttpRequestParserFactory;
import tp.pdc.proxy.parser.interfaces.HttpRequestParser;

public class HttpClientProxyHandler extends HttpHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(HttpClientProxyHandler.class);
	private static final ProxyLogger PROXY_LOGGER = ProxyLogger.getInstance();
	private static final HttpRequestParserFactory REQUEST_PARSER_FACTORY = HttpRequestParserFactory.getInstance();
	private static final ClientMetric CLIENT_METRICS = ClientMetricImpl.getInstance();
	
	private final HttpRequestParser requestParser;
	private final Set<Method> acceptedMethods;
	private boolean methodRecorded;
	private boolean errorState;
	
	private HttpClientState state;
	
	public HttpClientProxyHandler(Set<Method> acceptedMethods) {
		super();
		this.state = NotConnectedState.getInstance();
		this.acceptedMethods = acceptedMethods;
		this.requestParser = REQUEST_PARSER_FACTORY.getRequestParser();
	}
	
	public void reset(SelectionKey key) {
		this.state = NotConnectedState.getInstance();
		this.methodRecorded = false;
		this.errorState = false;
		this.requestParser.reset();
		
		setConnectedPeerKey(null);
		key.interestOps(SelectionKey.OP_READ);
	}

	public boolean hasFinishedProcessing() {
		return requestParser.hasFinished();
	}
	
	public HttpRequestParser getRequestParser() {
		return requestParser;
	}
	
	public void signalResponseProcessed(boolean closeConnectionToClient) {
		if (closeConnectionToClient || !shouldKeepConnectionAlive())
			this.state = LastWriteCloseConnection.getInstance();
		else
			this.state = LastWriteKeepConnection.getInstance();
	}
	
	private boolean shouldKeepConnectionAlive() {
		if (requestParser.hasHeaderValue(Header.CONNECTION))
			return BytesUtils.equalsBytes(requestParser.getHeaderValue(Header.CONNECTION), HeaderValue.KEEP_ALIVE.getValue(), BytesUtils.TO_LOWERCASE);
		else if (requestParser.hasHeaderValue(Header.PROXY_CONNECTION))
			return BytesUtils.equalsBytes(requestParser.getHeaderValue(Header.PROXY_CONNECTION), HeaderValue.KEEP_ALIVE.getValue(), BytesUtils.TO_LOWERCASE);
		return false;
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
			logError(key, "Failed to write to client", e.getMessage());
			closeServerChannel();
			closeChannel(socketChannel);
		}
	}

	@Override
	protected void processRead(SelectionKey key) {
		SocketChannel socketChannel = (SocketChannel) key.channel();
		ByteBuffer buffer = this.getReadBuffer();
		
		try {
			int bytesRead = socketChannel.read(buffer);
			if (bytesRead == -1) {
				LOGGER.info("Received EOF from client");
				closeServerChannel();
				closeChannel(socketChannel);
			}
			else {
				LOGGER.info("Read {} bytes from client", bytesRead);
				CLIENT_METRICS.addBytesRead(bytesRead);
			}
		} catch (IOException e) {
			LOGGER.warn("Failed to read from client: {}", e.getMessage());
			logError(key, "Failed to read from client", e.getMessage());
			closeServerChannel();
			closeChannel(socketChannel);
		}
	}
	
	@Override
	protected void process(ByteBuffer inputBuffer, SelectionKey key) {
		ByteBuffer processedBuffer = this.getProcessedBuffer();	
		
		if (!requestParser.hasFinished())
			processRequest(inputBuffer, processedBuffer, key);
		
		if (!errorState)
			state.handle(this, key);			
	}
		
	private void processRequest(ByteBuffer inputBuffer, ByteBuffer outputBuffer, SelectionKey key) {
		try {			
			requestParser.parse(inputBuffer, outputBuffer);
			recordAndValidateMethod(key);
			
		} catch (ParserFormatException e) {
			recordAndValidateMethod(key);
			
			if (!errorState) {
				LOGGER.warn("Invalid parse format: {}", e.getMessage());
				setErrorState(key, e.getResponseErrorCode());
			}
		}
	}
	
	private void recordAndValidateMethod(SelectionKey key) {
		if (requestParser.hasMethod() && !methodRecorded) {
			Method method = requestParser.getMethod();
			recordMethod(method);
			validateMethod(method, key);
		}
	}

	private void recordMethod(Method method) {
		LOGGER.info("Method recorded: {}", method.toString());
		methodRecorded = true;
		CLIENT_METRICS.addMethodCount(method);
	}
	
	private void validateMethod(Method method, SelectionKey key) {
		if (!acceptedMethods.contains(method)) {
			LOGGER.warn("Client's method not supported: {}", requestParser.getMethod());
			setErrorState(key, HttpErrorCode.NOT_IMPLEMENTED_501, method.toString());
		}
	}
	
	private void closeServerChannel() {
		if (isServerChannelOpen())
			getServerHandler().closeChannel(this.getConnectedPeerKey().channel());
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
	
	public void setErrorState(SelectionKey key, HttpErrorCode errorResponse) {
		setErrorState(key, errorResponse, StringUtils.EMPTY);
	}
		
	public void setErrorState(SelectionKey key, HttpErrorCode errorResponse, String logErrorMessage) {
		ByteBuffer writeBuffer = this.getWriteBuffer();
		writeBuffer.clear();
		writeBuffer.put(errorResponse.getBytes());
		
		setErrorState(key, errorResponse.getErrorMessage(), logErrorMessage);
	}
	
	public void setErrorState(SelectionKey key, String... logErrorMessages) {
		logError(key, logErrorMessages);
		
		this.errorState = true;
		this.state = LastWriteCloseConnection.getInstance();
		key.interestOps(SelectionKey.OP_WRITE);
		closeServerChannel();
	}
	
	private void logError(SelectionKey key, String... logErrorMessages) {
		PROXY_LOGGER.logError(requestParser, addressFromKey(key), logErrorMessages);		
	}
}
