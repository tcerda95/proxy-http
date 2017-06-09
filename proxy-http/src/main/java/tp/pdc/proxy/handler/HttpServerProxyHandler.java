package tp.pdc.proxy.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tp.pdc.proxy.ProxyLogger;
import tp.pdc.proxy.exceptions.ParserFormatException;
import tp.pdc.proxy.handler.interfaces.HttpServerState;
import tp.pdc.proxy.handler.state.server.LastWriteState;
import tp.pdc.proxy.handler.state.server.ReadResponseState;
import tp.pdc.proxy.handler.state.server.SendingRequestState;
import tp.pdc.proxy.header.Method;
import tp.pdc.proxy.metric.ServerMetricImpl;
import tp.pdc.proxy.metric.interfaces.ServerMetric;
import tp.pdc.proxy.parser.factory.HttpResponseParserFactory;
import tp.pdc.proxy.parser.interfaces.HttpResponseParser;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class HttpServerProxyHandler extends HttpHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(HttpServerProxyHandler.class);
	private static final HttpResponseParserFactory RESPONSE_PARSER_FACTORY =
		HttpResponseParserFactory.getInstance();
	private static final ServerMetric SERVER_METRICS = ServerMetricImpl.getInstance();
	private static final ProxyLogger PROXY_LOGGER = ProxyLogger.getInstance();

	/**
	 * Current server state
	 */
	private HttpServerState state;
	/**
	 * Flag indicating if an error has ocurred
	 */
	private boolean errorState;
	private HttpResponseParser responseParser;
	private boolean responseCodeRecorded;

	public HttpServerProxyHandler (ByteBuffer writeBuffer, ByteBuffer processedBuffer,
		Method clientMethod) {
		super(writeBuffer, processedBuffer);
		this.responseParser = RESPONSE_PARSER_FACTORY.getResponseParser(clientMethod);
		this.state = SendingRequestState.getInstance();
	}

	/**
	 * Resets the handlers attributes to it's initial values.
	 * @param key to reset the connected peer and to set an interest to read
	 */
	public void reset (SelectionKey key) {
		key.interestOps(0);

		responseParser.reset();
		this.state = SendingRequestState.getInstance();

		setConnectedPeerKey(null);
		setProcessedBuffer(null);
		setWriteBuffer(null);

		responseCodeRecorded = false;
	}

	/**
	 * Sets client method requested
	 * @param method sent by client
     */
	public void setClientMethod (Method method) {
		responseParser.setClientMethod(method);
	}

	@Override
	protected void processWrite (ByteBuffer inputBuffer, SelectionKey key) {
		SocketChannel socketChannel = (SocketChannel) key.channel();

		try {
			int bytesWritten = socketChannel.write(inputBuffer);

			LOGGER.info("Sent {} bytes to server", bytesWritten);
			SERVER_METRICS.addBytesWritten(bytesWritten);

			state.handle(this, key);

		} catch (IOException e) {
			LOGGER.warn("Failed to write response to server");
			setResponseError(key, e.getMessage());
			closeChannel(socketChannel);
		}
	}

	@Override
	protected void processRead (SelectionKey key) {
		SocketChannel socketChannel = (SocketChannel) key.channel();
		ByteBuffer buffer = this.getReadBuffer();

		try {
			int bytesRead = socketChannel.read(buffer);

			// EOF could signal normal end of response if no content-length or chunked was received
			if (bytesRead == -1) {
				LOGGER.info("Closing connection to server: EOF");
				LOGGER.debug("Registering client for write: client must consume EOF");

				this.getConnectedPeerKey().interestOps(SelectionKey.OP_WRITE);

				if (responseParser.hasHeadersFinished())
					logAccess(key);
				else {
					LOGGER.warn("Sever sent EOF though headers not finished");
					setResponseError(key, "Server EOF but headers not finished");
					return;
				}

				closeChannel(socketChannel);

				if (!hasFinishedProcessing()) {
					LOGGER.debug("Signaling client to send last write and not keep connection");
					getClientHandler().signalResponseProcessed(true);
				} else {
					LOGGER.error(
						"Read server EOF and processing had finished. Shouldn't be registered for reading.");
					LOGGER.debug("Signaling client to send last write and keep connection");
					getClientHandler().signalResponseProcessed(false);
				}
			} else {
				LOGGER.info("Read {} bytes from server", bytesRead);
				SERVER_METRICS.addBytesRead(bytesRead);
			}

		} catch (IOException e) {
			LOGGER.warn("Failed to read response from server: {}", e.getMessage());
			setResponseError(key, e.getMessage());
		}
	}

	@Override
	protected void process (ByteBuffer inputBuffer, SelectionKey key) {
		ByteBuffer processedBuffer = this.getProcessedBuffer();

		if (!responseParser.hasFinished())
			processResponse(inputBuffer, processedBuffer, key);

		if (!errorState)
			state.handle(this, key);
	}

	/**
	 * Parses a response
	 * @param inputBuffer buffer to read from server
	 * @param outputBuffer buffer to put processed response
	 * @param key
     */
	private void processResponse (ByteBuffer inputBuffer, ByteBuffer outputBuffer,
		SelectionKey key) {
		try {
			responseParser.parse(inputBuffer, outputBuffer);

			if (shouldRecordStatusCode())
				recordStatusCode();

		} catch (ParserFormatException e) {
			if (shouldRecordStatusCode())
				recordStatusCode();

			setResponseError(key, e.getMessage());
		}
	}

	/**
	 * Indicates if the status code should be recorded
	 * @return true if the status code should be recorded
     */
	private boolean shouldRecordStatusCode () {
		return responseParser.hasStatusCode() && !responseCodeRecorded;
	}

	/**
	 * Adds to the metric the status code that server sends
	 */
	private void recordStatusCode () {
		SERVER_METRICS.addResponseCodeCount(responseParser.getStatusCode());
		responseCodeRecorded = true;
	}

	/**
	 * In case of error ir closes the connection to the server
	 * @param key server's key
	 * @param message message to log
     */
	private void setResponseError (SelectionKey key, String message) {
		LOGGER.warn("Closing Connection to server: {}", message);
		errorState = true;

		closeChannel(key.channel());

		getClientHandler().setErrorState(this.getConnectedPeerKey(), message);
	}

	/**
	 * Gets the corresponding {@link HttpClientProxyHandler}.
	 * @return client's handler
     */
	public HttpClientProxyHandler getClientHandler () {
		return (HttpClientProxyHandler) this.getConnectedPeerKey().attachment();
	}

	/**
	 * Gets the {@link HttpResponseParser}
	 * @return response parser
     */
	public HttpResponseParser getResponseParser () {
		return responseParser;
	}

	/**
	 * Signals tha the request is processed and changes state to {@link LastWriteState}
	 */
	public void signalRequestProcessed () {
		this.state = LastWriteState.getInstance();
	}

	/**
	 * Checks if the parser has finished processing the response
	 * @return true if the parse has finished, false if not.
     */
	public boolean hasFinishedProcessing () {
		return responseParser.hasFinished();
	}

	/**
	 * Sets the {@link HttpServerState} to {@link ReadResponseState}.
	 * Unregisters server from write and registers for read because the whole client request was sent
	 * @param key server's key
     */
	public void setReadResponseState (SelectionKey key) {
		LOGGER.debug(
			"Unregistering server from write and registering for read: whole client request sent");
		key.interestOps(SelectionKey.OP_READ);
		this.state = ReadResponseState.getInstance();
	}

	public void logAccess (SelectionKey key) {
		PROXY_LOGGER.logAccess(getClientHandler().getRequestParser(), responseParser,
			addressFromKey(getConnectedPeerKey()), addressFromKey(key));
	}
}
