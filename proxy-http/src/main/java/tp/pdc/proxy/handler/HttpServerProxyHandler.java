package tp.pdc.proxy.handler;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tp.pdc.proxy.ConnectionManager;
import tp.pdc.proxy.HttpErrorCode;
import tp.pdc.proxy.exceptions.IllegalHttpHeadersException;
import tp.pdc.proxy.exceptions.ParserFormatException;
import tp.pdc.proxy.header.BytesUtils;
import tp.pdc.proxy.header.Header;
import tp.pdc.proxy.header.HeaderValue;
import tp.pdc.proxy.header.Method;
import tp.pdc.proxy.metric.ServerMetricImpl;
import tp.pdc.proxy.metric.interfaces.ServerMetric;
import tp.pdc.proxy.parser.factory.HttpBodyParserFactory;
import tp.pdc.proxy.parser.factory.HttpResponseParserFactory;
import tp.pdc.proxy.parser.interfaces.HttpBodyParser;
import tp.pdc.proxy.parser.interfaces.HttpResponseParser;

public class HttpServerProxyHandler extends HttpHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(HttpServerProxyHandler.class);
	private static final HttpBodyParserFactory BODY_PARSER_FACTORY = HttpBodyParserFactory.getInstance();
	private static final HttpResponseParserFactory RESPONSE_PARSER_FACTORY = HttpResponseParserFactory.getInstance();
	private static final ServerMetric SERVER_METRICS = ServerMetricImpl.getInstance();
	private static final ConnectionManager CONNECTION_MANAGER = ConnectionManager.getInstance();
	
	private HttpResponseParser responseParser;
	private HttpBodyParser bodyParser;
	private Method clientMethod;
	private boolean responseCodeRecorded;
	
	public HttpServerProxyHandler(int readBufferSize, ByteBuffer writeBuffer, ByteBuffer processedBuffer, Method clientMethod) {
		super(readBufferSize, writeBuffer, processedBuffer);
		this.responseParser = RESPONSE_PARSER_FACTORY.getResponseParser();
		this.clientMethod = clientMethod;
	}
	
	public void reset() {
		responseParser.reset();
		setConnectedPeerKey(null);
		setProcessedBuffer(null);
		setWriteBuffer(null);
		
		clientMethod = null;
		responseCodeRecorded = false;
		bodyParser = null;
	}
	
	public void setClientMethod(Method method) {
		this.clientMethod = method;
	}
	
	@Override
	protected void processWrite(ByteBuffer inputBuffer, SelectionKey key) {
		SocketChannel socketChannel = (SocketChannel) key.channel();
		
		try {
			int bytesWritten = socketChannel.write(inputBuffer);
			
			LOGGER.info("Sent {} bytes to server", bytesWritten);
			SERVER_METRICS.addBytesWritten(bytesWritten);
			
			if (getClientState() != ClientHandlerState.REQUEST_PROCESSED) {
				LOGGER.debug("Registering client for read: whole request not processed yet");
				this.getConnectedPeerKey().interestOps(SelectionKey.OP_READ);
				getClientHandler().handleProcess(getConnectedPeerKey());  // Espacio libre en processed buffer ---> cliente puede procesar
				
				if (!inputBuffer.hasRemaining()) {
					LOGGER.debug("Unregistering server from write: nothing left in client processed buffer");
					key.interestOps(0);
				}
			}
			
			if (!inputBuffer.hasRemaining() && getClientState() == ClientHandlerState.REQUEST_PROCESSED) {
				LOGGER.debug("Unregistering server from write and registering for read: whole client request sent");
				key.interestOps(SelectionKey.OP_READ);
				getClientHandler().setRequestSentState();
			}
			
		} catch (IOException e) {
			LOGGER.warn("Failed to write response to server");
			setResponseError(key, e.getMessage());
			try {
				socketChannel.close();
			} catch (IOException e1) {
				LOGGER.error("Failed to close server's socket on server's write error: {}", e1.getMessage());
				e1.printStackTrace();
			}
		}
	}
	
	@Override
	protected void processRead(SelectionKey key) {
		SocketChannel socketChannel = (SocketChannel) key.channel();
		ByteBuffer buffer = this.getReadBuffer();
		
		try {
			int bytesRead = socketChannel.read(buffer);
			
			if (bytesRead == -1) {
				LOGGER.warn("Closing connection to server: EOF");
				
				socketChannel.close();
				
				if (bodyParser != null && !shouldKeepAlive()) {
					getClientHandler().setResponseProcessed();
					LOGGER.debug("Registering client for write: client must consume EOF");
					this.getConnectedPeerKey().interestOps(SelectionKey.OP_WRITE);
				}
				else if (bodyParser != null && shouldKeepAlive())
					setResponseError(key, "Server EOF when should keep alive");
				else
					setResponseError(key, "Server EOF when headers not finished");
				
			}
			else {
				LOGGER.info("Read {} bytes from server", bytesRead);
				SERVER_METRICS.addBytesRead(bytesRead);
			}
			
		} catch (IOException e) {
			LOGGER.warn("Failed to read response from server", e.getMessage());
			setResponseError(key, e.getMessage());
		}
	}
	
	@Override
	protected void process(ByteBuffer inputBuffer, SelectionKey key) {
		ByteBuffer processedBuffer = this.getProcessedBuffer();
		
		if (!key.isValid())
			return;
		
		if (!responseParser.hasFinished())
			processResponse(inputBuffer, processedBuffer, key);
		else if (!bodyParser.hasFinished())
			processBody(inputBuffer, processedBuffer, key);
		
		if (!key.channel().isOpen())
			getClientHandler().setResponseProcessed();
		
		if (processedBuffer.position() != 0) {
			LOGGER.debug("Registering client for write: server has written to processed buffer");
			this.getConnectedPeerKey().interestOps(SelectionKey.OP_WRITE);
		}
		
		if (bodyParser != null && bodyParser.hasFinished()) {
			try {
				if (shouldKeepAlive())
					CONNECTION_MANAGER.storeConnection(key);
				else
					key.channel().close();
			} catch (IOException e) {
				LOGGER.error("Failed to store server's connection on response processed: {}", e.getMessage());
				e.printStackTrace();
			}
		}
		else if (!processedBuffer.hasRemaining()) {
			LOGGER.debug("Unregistering server from read: processed buffer full");
			key.interestOps(0);
		}
	}
	
	private boolean shouldKeepAlive() {
		return responseParser.hasHeaderValue(Header.CONNECTION) && 
				BytesUtils.equalsBytes(responseParser.getHeaderValue(Header.CONNECTION), HeaderValue.KEEP_ALIVE.getValue());
	}

	private void processResponse(ByteBuffer inputBuffer, ByteBuffer outputBuffer, SelectionKey key) {
		try {
			responseParser.parse(inputBuffer, outputBuffer);
			
			if (responseParser.hasStatusCode() && !responseCodeRecorded) {
				SERVER_METRICS.addResponseCodeCount(responseParser.getStatusCode());
				responseCodeRecorded = true;
			}
			
			if (responseParser.hasFinished()) {
				bodyParser = BODY_PARSER_FACTORY.getServerHttpBodyParser(responseParser, clientMethod);
				processBody(inputBuffer, outputBuffer, key);
			}
			
		} catch (ParserFormatException e) {
			setResponseError(key, e.getMessage());
		} catch (IllegalHttpHeadersException e) {
			setResponseError(key, e.getMessage());
		}
	}
	
	private void processBody(ByteBuffer inputBuffer, ByteBuffer outputBuffer, SelectionKey key) {
		try {
			bodyParser.parse(inputBuffer, outputBuffer);
			if (bodyParser.hasFinished())
				getClientHandler().setResponseProcessed();
		} catch (ParserFormatException e) {
			setResponseError(key, e.getMessage());
		}
	}

	private void setResponseError(SelectionKey key, String message) {
		LOGGER.warn("Closing Connection to server: {}", message);
		
		try {
			key.channel().close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		// TODO: en realidad solo hay q settear estado de error sin responder ningun codigo de error
		getClientHandler().setErrorState(HttpErrorCode.BAD_GATEWAY_502, this.getConnectedPeerKey());
	}
	
	private ClientHandlerState getClientState() {
		return getClientHandler().getState();
	}
	
	private HttpClientProxyHandler getClientHandler() {
		return (HttpClientProxyHandler) this.getConnectedPeerKey().attachment();
	}	
}
