package tp.pdc.proxy.handler;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tp.pdc.proxy.HttpResponse;
import tp.pdc.proxy.exceptions.IllegalHttpHeadersException;
import tp.pdc.proxy.exceptions.ParserFormatException;
import tp.pdc.proxy.header.Method;
import tp.pdc.proxy.parser.factory.HttpBodyParserFactory;
import tp.pdc.proxy.parser.interfaces.HttpBodyParser;
import tp.pdc.proxy.parser.interfaces.HttpResponseParser;
import tp.pdc.proxy.parser.mainParsers.HttpResponseParserImpl;

public class HttpServerProxyHandler extends HttpHandler {
	private final static Logger LOGGER = LoggerFactory.getLogger(HttpServerProxyHandler.class);
	
	private HttpResponseParser responseParser;
	private HttpBodyParser bodyParser;
	private Method clientMethod;
	private boolean responseProcessed;
	
	public HttpServerProxyHandler(int readBufferSize, ByteBuffer writeBuffer, ByteBuffer processedBuffer, Method clientMethod) {
		super(readBufferSize, writeBuffer, processedBuffer);
		responseParser = new HttpResponseParserImpl();
		this.clientMethod = clientMethod;
	}

	@Override
	protected void processRead(SelectionKey key) {
		SocketChannel socketChannel = (SocketChannel) key.channel();
		ByteBuffer buffer = this.getReadBuffer();
		int bytesRead;
		
		try {
			bytesRead = socketChannel.read(buffer);
			
			if (bytesRead == -1) {
				LOGGER.warn("Closing connection to server: EOF");
				socketChannel.close();
				responseProcessed = true;
			}
			else if (bytesRead > 0) {
				LOGGER.info("Read {} bytes from server", bytesRead);
				this.getConnectedPeerKey().interestOps(SelectionKey.OP_WRITE);
			}
			
		} catch (IOException e) {
			LOGGER.warn("Failed to read response from server");
			getClientHandler().setErrorState(HttpResponse.BAD_GATEWAY_502, this.getConnectedPeerKey());
		}
	}
	
	@Override
	protected void process(ByteBuffer inputBuffer, SelectionKey key) {
		ByteBuffer processedBuffer = this.getProcessedBuffer();
		
		if (!responseParser.hasFinished())
			processResponse(inputBuffer, processedBuffer, key);
		else if (!bodyParser.hasFinished())
			processBody(inputBuffer, processedBuffer, key);
		
		if (!key.channel().isOpen())
			responseProcessed = true;
		
		if (processedBuffer.position() != 0) {
			LOGGER.debug("Registering client for write: server has written to processed buffer");
			this.getConnectedPeerKey().interestOps(SelectionKey.OP_WRITE);
		}
		
		// Conexión no persistente
		if (isResponseProcessed()) {
			try {
				LOGGER.info("Closing connection to server: whole response processed");
				key.channel().close();
			} catch (IOException e) {
				LOGGER.error("Failed to close server's socket on response processed: {}", e.getMessage());
				e.printStackTrace();
			}
		}
		else if (!processedBuffer.hasRemaining()) {
			LOGGER.debug("Unregistering server from read: processed buffer full");
			key.interestOps(0);
		}
	}
	
	@Override
	protected void processWrite(ByteBuffer inputBuffer, SelectionKey key) {
		SocketChannel socketChannel = (SocketChannel) key.channel();
		
		try {
			LOGGER.info("Sending {} bytes to server", inputBuffer.remaining());
			socketChannel.write(inputBuffer);
			
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
			LOGGER.warn("Failed to write request to server");
			getClientHandler().setErrorState(HttpResponse.BAD_GATEWAY_502, this.getConnectedPeerKey());
			try {
				socketChannel.close();
			} catch (IOException e1) {
				LOGGER.error("Failed to close server's socket on server's write error: {}", e1.getMessage());
				e1.printStackTrace();
			}
		}
	}
	
	private void processResponse(ByteBuffer inputBuffer, ByteBuffer outputBuffer, SelectionKey key) {
		try {
			responseParser.parse(inputBuffer, outputBuffer);
			
			if (responseParser.hasFinished()) {
				bodyParser = HttpBodyParserFactory.getServerHttpBodyParser(responseParser, clientMethod);
				processBody(inputBuffer, outputBuffer, key);
			}
			
		} catch (ParserFormatException e) {
			setResponseError(key, e);
		} catch (IllegalHttpHeadersException e) {
			setResponseError(key, e);
		}
	}
	
	private void processBody(ByteBuffer inputBuffer, ByteBuffer outputBuffer, SelectionKey key) {
		try {
			bodyParser.parse(inputBuffer, outputBuffer);
			responseProcessed = bodyParser.hasFinished();
		} catch (ParserFormatException e) {
			setResponseError(key, e);
		}
	}

	private void setResponseError(SelectionKey key, Exception e) {
		LOGGER.warn("Closing Connection to server: {}", e.getMessage());
		
		try {
			key.channel().close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		responseProcessed = true;
		this.getConnectedPeerKey().interestOps(SelectionKey.OP_WRITE);
	}

	public boolean isResponseProcessed() {
		return responseProcessed;
	}
	
	private ClientHandlerState getClientState() {
		return getClientHandler().getState();
	}
	
	private HttpClientProxyHandler getClientHandler() {
		return (HttpClientProxyHandler) this.getConnectedPeerKey().attachment();
	}	
}
