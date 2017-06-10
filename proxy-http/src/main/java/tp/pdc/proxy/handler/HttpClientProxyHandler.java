package tp.pdc.proxy.handler;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tp.pdc.proxy.bytes.BytesUtils;
import tp.pdc.proxy.exceptions.ParserFormatException;
import tp.pdc.proxy.handler.interfaces.HttpClientState;
import tp.pdc.proxy.handler.state.client.*;
import tp.pdc.proxy.header.Header;
import tp.pdc.proxy.header.HeaderValue;
import tp.pdc.proxy.header.HttpErrorCode;
import tp.pdc.proxy.header.Method;
import tp.pdc.proxy.log.ProxyLogger;
import tp.pdc.proxy.metric.ClientMetricImpl;
import tp.pdc.proxy.metric.interfaces.ClientMetric;
import tp.pdc.proxy.parser.factory.HttpRequestParserFactory;
import tp.pdc.proxy.parser.interfaces.HttpRequestParser;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Set;

/**
 * Handler for a client interacting with the proxy.
 */
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

	public HttpClientProxyHandler (Set<Method> acceptedMethods) {
		super();
		this.state = NotConnectedState.getInstance();
		this.acceptedMethods = acceptedMethods;
		this.requestParser = REQUEST_PARSER_FACTORY.getRequestParser();
	}

	/**
	 * Resets the handlers attributes to it's initial values.
	 * @param key to reset the connected peer and to set an interest to read
     */
	public void reset (SelectionKey key) {
		this.state = NotConnectedState.getInstance();
		this.methodRecorded = false;
		this.errorState = false;
		this.requestParser.reset();

		setConnectedPeerKey(null);
		key.interestOps(SelectionKey.OP_READ);
	}

	/**
	 * Checks if the {@link HttpRequestParser} has finished processing the request
	 * @return true if the parser has finished, false on the contrary
     */
	public boolean hasFinishedProcessing () {
		return requestParser.hasFinished();
	}

	public HttpRequestParser getRequestParser () {
		return requestParser;
	}

	/**
	 * Sends a signal to tell the response is processed.
	 * Sets the {@link HttpClientState} to {@link LastWriteCloseConnection} or {@link LastWriteKeepConnection}
	 * @param closeConnectionToClient
     */
	public void signalResponseProcessed (boolean closeConnectionToClient) {
		if (closeConnectionToClient || !shouldKeepConnectionAlive())
			this.state = LastWriteCloseConnection.getInstance();
		else
			this.state = LastWriteKeepConnection.getInstance();
	}

	/**
	 * Checks if the request header connection or proxy connection is keep alive or not.
	 * @return true if the connection should be kept alive
     */
	private boolean shouldKeepConnectionAlive () {
		if (requestParser.hasHeaderValue(Header.CONNECTION))
			return BytesUtils.equalsBytes(requestParser.getHeaderValue(Header.CONNECTION),
				HeaderValue.KEEP_ALIVE.getValue(), BytesUtils.TO_LOWERCASE);
		else if (requestParser.hasHeaderValue(Header.PROXY_CONNECTION))
			return BytesUtils.equalsBytes(requestParser.getHeaderValue(Header.PROXY_CONNECTION),
				HeaderValue.KEEP_ALIVE.getValue(), BytesUtils.TO_LOWERCASE);
		return false;
	}

	/**
	 * Set the {@link HttpClientState} to {@link SendingResponseState} once the request is sent.
	 */
	public void signalRequestSent () {
		this.state = SendingResponseState.getInstance();
	}

	/**
	 * Sets {@link HttpClientState} to Connecting
	 * @param key client key to unregister
     */
	public void setConnectingState (SelectionKey key) {
		LOGGER.debug("Unregistering client key: connecting");
		key.interestOps(0);
		this.state = ConnectingState.getInstance();
	}

	/**
	 * If the parser has finished processing the request, it sets the {@link HttpClientState} to
	 * {@link RequestProcessedState}. If not, it sets to {@link ConnectedState}.
	 * @param key
     */
	public void handleConnect (SelectionKey key) {
		if (hasFinishedProcessing())
			setRequestProcessedState(key);
		else
			setConnectedState(key);
	}

	/**
	 * Sets to {@link RequestProcessedState}. Unregisters client from read and registers
	 * server for write and signals end of request.
	 * @param key client key
     */
	public void setRequestProcessedState (SelectionKey key) {
		LOGGER.debug("Unregistering client from read: request processed");
		key.interestOps(0);

		LOGGER.debug("Registering server for write and signaling end of request");
		this.getConnectedPeerKey().interestOps(SelectionKey.OP_WRITE);

		this.state = RequestProcessedState.getInstance();
		getServerHandler().signalRequestProcessed();
	}

	/**
	 * Sets {@link HttpClientState} to {@link ConnectedState} and registers client for read
	 * and server for write.
	 * @param key client key
     */
	public void setConnectedState (SelectionKey key) {
		this.state = ConnectedState.getInstance();
		LOGGER.debug("Registering client for read and server for write: client in connected state");
		key.interestOps(SelectionKey.OP_READ);
		this.getConnectedPeerKey().interestOps(SelectionKey.OP_WRITE);
	}

	@Override
	protected void processWrite (ByteBuffer inputBuffer, SelectionKey key) {
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
	protected void processRead (SelectionKey key) {
		SocketChannel socketChannel = (SocketChannel) key.channel();
		ByteBuffer buffer = this.getReadBuffer();

		try {
			int bytesRead = socketChannel.read(buffer);
			if (bytesRead == -1) {
				LOGGER.info("Received EOF from client");
				closeServerChannel();
				closeChannel(socketChannel);
			} else {
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
	protected void process (ByteBuffer inputBuffer, SelectionKey key) {
		ByteBuffer processedBuffer = this.getProcessedBuffer();

		if (!requestParser.hasFinished())
			processRequest(inputBuffer, processedBuffer, key);

		if (!errorState)
			state.handle(this, key);
	}

	/**
	 * Parses a request
	 * @param inputBuffer buffer to read from client
	 * @param outputBuffer buffer to put processed request after parsing
	 * @param key client's key
     */
	private void processRequest (ByteBuffer inputBuffer, ByteBuffer outputBuffer,
		SelectionKey key) {
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

	/**
	 * Gets the method from the request sent by the client and checks if it's accepted by the proxy.
	 * @param key client's key
     */
	private void recordAndValidateMethod (SelectionKey key) {
		if (requestParser.hasMethod() && !methodRecorded) {
			Method method = requestParser.getMethod();
			recordMethod(method);
			validateMethod(method, key);
		}
	}

	/**
	 * Counts the method to fill the metrics
	 * @param method methos to be recorded
     */
	private void recordMethod (Method method) {
		LOGGER.info("Method recorded: {}", method.toString());
		methodRecorded = true;
		CLIENT_METRICS.addMethodCount(method);
	}

	/**
	 * Check if the method in a request is accepted by the proxy
	 * @param method method to be validated
	 * @param key client's key
     */
	private void validateMethod (Method method, SelectionKey key) {
		if (!acceptedMethods.contains(method)) {
			LOGGER.warn("Client's method not supported: {}", requestParser.getMethod());
			setErrorState(key, HttpErrorCode.NOT_IMPLEMENTED_501, method.toString());
		}
	}

	/**
	 * Closes the server channel
	 */
	private void closeServerChannel () {
		if (isServerChannelOpen())
			getServerHandler().closeChannel(this.getConnectedPeerKey().channel());
	}

	/**
	 * Checks if the server channel is open
	 * @return true if the server channel if open, false if not
     */
	private boolean isServerChannelOpen () {
		return this.getConnectedPeerKey() != null && this.getConnectedPeerKey().channel().isOpen();
	}

	/**
	 * Gets the corresponding {@link HttpServerProxyHandler}
	 * @return The server proxy pandler associated with this client.
     */
	public HttpServerProxyHandler getServerHandler () {

		if (this.getConnectedPeerKey() == null) {
			LOGGER.error("Asking for server handler when there is none!");
			throw new IllegalStateException("Connection not established yet, no server handler available");
		}

		return (HttpServerProxyHandler) this.getConnectedPeerKey().attachment();
	}

	/**
	 * Indicates an error has ocurred to the client with the given {@link HttpErrorCode}.
	 * Sets handler to {@link LastWriteCloseConnection} state. <p>
	 * The error is logged to the error logs.
	 * @param key client's key
	 * @param errorResponse error code to notify the client
     */
	public void setErrorState (SelectionKey key, HttpErrorCode errorResponse) {
		setErrorState(key, errorResponse, StringUtils.EMPTY);
	}

	/**
	 * Indicates an error has ocurred to the client with the given {@link HttpErrorCode}.
	 * Sets handler to {@link LastWriteCloseConnection} state. <p>
	 * The error is logged to the error logs with a message which sepcifies more information
	 * about the error.
	 * @param key client's key
	 * @param errorResponse {@link HttpErrorCode}
	 * @param logErrorMessage Extra error message to log
     */
	public void setErrorState (SelectionKey key, HttpErrorCode errorResponse, String logErrorMessage) {
		ByteBuffer writeBuffer = this.getWriteBuffer();
		writeBuffer.clear();
		writeBuffer.put(errorResponse.getBytes());

		setErrorState(key, errorResponse.getErrorMessage(), logErrorMessage);
	}

    /**
	 * Indicates an error has ocurred to the client. Sets handler to {@link LastWriteCloseConnection} state. 
	 * No {@link HttpErrorCode} is sent to the client. This method should be used on server errors once a portion
	 * of the answer had already been sent to the client. <p>
	 * The error is logged to the error logs with a message which sepcifies more information about the error.
	 * @param key client's key
	 * @param logErrorMessages messages to log
     */
	public void setErrorState (SelectionKey key, String... logErrorMessages) {
		logError(key, logErrorMessages);

		this.errorState = true;
		this.state = LastWriteCloseConnection.getInstance();
		key.interestOps(SelectionKey.OP_WRITE);
		closeServerChannel();
	}

	private void logError (SelectionKey key, String... logErrorMessages) {
		PROXY_LOGGER.logError(requestParser, addressFromKey(key), logErrorMessages);
	}
}
