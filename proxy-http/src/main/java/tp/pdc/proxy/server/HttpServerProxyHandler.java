package tp.pdc.proxy.server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tp.pdc.proxy.HttpHandler;
import tp.pdc.proxy.HttpProxySelectorProtocol;
import tp.pdc.proxy.HttpResponse;
import tp.pdc.proxy.client.ClientHandlerState;
import tp.pdc.proxy.client.HttpClientProxyHandler;
import tp.pdc.proxy.exceptions.ParserFormatException;
import tp.pdc.proxy.header.BytesUtils;
import tp.pdc.proxy.header.Header;
import tp.pdc.proxy.header.HeaderValue;
import tp.pdc.proxy.parser.MockHeaderParser;
import tp.pdc.proxy.parser.interfaces.HttpRequestParser;
import tp.pdc.proxy.parser.interfaces.Parser;

public class HttpServerProxyHandler extends HttpHandler {
	private final static Logger LOGGER = LoggerFactory.getLogger(HttpProxySelectorProtocol.class);
	private HttpRequestParser headersParser;
	private Parser bodyParser;
	
	public HttpServerProxyHandler(int readBufferSize, ByteBuffer writeBuffer, ByteBuffer processedBuffer) {
		super(readBufferSize, writeBuffer, processedBuffer);
		// TODO: instanciar parsers de verdad
		headersParser = new MockHeaderParser();
	}

	@Override
	protected void processRead(SelectionKey key) {
		SocketChannel socketChannel = (SocketChannel) key.channel();
		ByteBuffer buffer = this.getReadBuffer();
		int bytesRead;
		
		try {
			bytesRead = socketChannel.read(buffer);
			
			if (bytesRead == -1)
				socketChannel.close();
			else if (bytesRead > 0)
				this.getConnectedPeerKey().interestOps(SelectionKey.OP_WRITE);
			
		} catch (IOException e) {
			LOGGER.warn("Failed to read response from server");
			getClientHandler().setErrorState(HttpResponse.BAD_GATEWAY_502, this.getConnectedPeerKey());
		}
	}
	
	private boolean responseProcessed() {
		if (headersParser.hasFinished() && bodyParser == null)
			return true;
		if (headersParser.hasFinished() && bodyParser.hasFinished())
			return true;
		return false;
	}

	@Override
	protected void process(ByteBuffer inputBuffer, SelectionKey key) {
		ByteBuffer processedBuffer = this.getProcessedBuffer();
		
		if (headersParser.hasFinished())
			try {
				bodyParser.parse(inputBuffer, processedBuffer);
			} catch (ParserFormatException e) {
				// TODO: respuesta de server mal formada. Qué devolver al cliente?
			}
		else {
			try {
				headersParser.parse(inputBuffer, processedBuffer);
				if (headersParser.hasFinished() && isResponseWithBody()) {
					if (isContentLength()) {
						// TODO: instanciar parser de content length
					}
					else if (isChunked()) {
						// TODO: instanciar parser de chunked
					}
				}
			} catch (ParserFormatException e) {
				// TODO: respuesta de server mal formada. Qué devolver al cliente?
			}
		}
		
		if (responseProcessed()) {
			try {
				key.channel().close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	@Override
	protected void processWrite(ByteBuffer inputBuffer, SelectionKey key) {
		SocketChannel socketChannel = (SocketChannel) key.channel();
		
		try {
			LOGGER.info("Sending {} bytes to server", inputBuffer.remaining());
			socketChannel.write(inputBuffer);
			
			if (getClientState() != ClientHandlerState.REQUEST_PROCESSED)
				this.getConnectedPeerKey().interestOps(SelectionKey.OP_READ);
			
			if (!inputBuffer.hasRemaining() && getClientState() != ClientHandlerState.REQUEST_PROCESSED) {
				LOGGER.debug("Unregistering server from write: nothing left in client processed buffer");
				key.interestOps(0);
			}
			
			if (!inputBuffer.hasRemaining() && getClientState() == ClientHandlerState.REQUEST_PROCESSED) {
				LOGGER.debug("Unregistering server from write and registering for read: whole client request sent");
				key.interestOps(SelectionKey.OP_READ);
				getClientHandler().setRequestSentState();
			}
			
		} catch (IOException e) {
			LOGGER.warn("Failed to write request to server");
			getClientHandler().setErrorState(HttpResponse.BAD_GATEWAY_502, this.getConnectedPeerKey());
		}
	}

	private ClientHandlerState getClientState() {
		return getClientHandler().getState();
	}
	
	private HttpClientProxyHandler getClientHandler() {
		return (HttpClientProxyHandler) this.getConnectedPeerKey().attachment();
	}
	
	private boolean isChunked() {
		return headersParser.hasHeaderValue(Header.TRANSFER_ENCODING) 
				&& BytesUtils.equalsBytes(headersParser.getHeaderValue(Header.TRANSFER_ENCODING), HeaderValue.CHUNKED.getValue());
	}

	private boolean isContentLength() {
		return headersParser.hasHeaderValue(Header.CONTENT_LENGTH);
	}

	private boolean isResponseWithBody() {
		// TODO Auto-generated method stub
		return false;
	}
}
