package tp.pdc.proxy.handler;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tp.pdc.proxy.exceptions.IllegalHttpHeadersException;
import tp.pdc.proxy.exceptions.ParserFormatException;
import tp.pdc.proxy.handler.interfaces.HttpServerState;
import tp.pdc.proxy.handler.state.server.LastWriteState;
import tp.pdc.proxy.handler.state.server.ReadResponseState;
import tp.pdc.proxy.handler.state.server.SendingRequestState;
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
	
	private HttpServerState state;
	private HttpResponseParser responseParser;
	private HttpBodyParser bodyParser;
	private Method clientMethod;
	private boolean responseCodeRecorded;
	
	public HttpServerProxyHandler(int readBufferSize, ByteBuffer writeBuffer, ByteBuffer processedBuffer, Method clientMethod) {
		super(readBufferSize, writeBuffer, processedBuffer);
		this.responseParser = RESPONSE_PARSER_FACTORY.getResponseParser();
		this.clientMethod = clientMethod;
		this.state = SendingRequestState.getInstance();
	}
	
	public void reset(SelectionKey key) {
		key.interestOps(0);
		
		responseParser.reset();
		this.state = SendingRequestState.getInstance();
		
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
						
			state.handle(this, key);
			
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
				LOGGER.info("Closing connection to server: EOF");
				LOGGER.debug("Registering client for write: client must consume EOF");
				this.getConnectedPeerKey().interestOps(SelectionKey.OP_WRITE);
				socketChannel.close();
				
				if (hasFinishedProcessing()) {
					LOGGER.error("Read server EOF and procesing had finished. Shouldn't be registered for reading.");
					LOGGER.debug("Signaling client to send last write and keep connection");
					getClientHandler().signalResponseProcessed(false);
				}
				else {
					LOGGER.debug("Signaling client to send last write and not keep connection");
					getClientHandler().signalResponseProcessed(true);
				}				
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
		
		if (!responseParser.hasFinished())
			processResponse(inputBuffer, processedBuffer, key);
		else if (!bodyParser.hasFinished())
			processBody(inputBuffer, processedBuffer, key);

		state.handle(this, key);		
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
		
		getClientHandler().setErrorState(this.getConnectedPeerKey());
	}
	
	public HttpClientProxyHandler getClientHandler() {
		return (HttpClientProxyHandler) this.getConnectedPeerKey().attachment();
	}
	
	public HttpResponseParser getResponseParser() {
		return responseParser;
	}

	public void signalRequestProcessed() {
		this.state = LastWriteState.getInstance();
	}
	
	public boolean hasFinishedProcessing() {
		return responseParser.hasFinished() && bodyParser.hasFinished();
	}

	public void setReadResponseState(SelectionKey key) {
		LOGGER.debug("Unregistering server from write and registering for read: whole client request sent");
		key.interestOps(SelectionKey.OP_READ);
		this.state = ReadResponseState.getInstance();
	}
}
