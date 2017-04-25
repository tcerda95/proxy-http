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

public class HttpServerProxyHandler extends HttpHandler {
	private final static Logger LOGGER = LoggerFactory.getLogger(HttpProxySelectorProtocol.class);
	
	
	public HttpServerProxyHandler(int readBufferSize, ByteBuffer writeBuffer, ByteBuffer processedBuffer) {
		super(readBufferSize, writeBuffer, processedBuffer);
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
			else
				this.getConnectedPeerKey().interestOps(SelectionKey.OP_WRITE);	
			
		} catch (IOException e) {
			LOGGER.warn("Failed to read response from server");
			getClientHandler().setErrorState(HttpResponse.BAD_GATEWAY_502, this.getConnectedPeerKey());
		}
	}

	@Override
	protected void process(ByteBuffer inputBuffer, SelectionKey key) {
		ByteBuffer processedBuffer = this.getProcessedBuffer();
		processedBuffer.put(inputBuffer);
	}
	
	private void processContent() {
		
	}

	@Override
	protected void processWrite(ByteBuffer inputBuffer, SelectionKey key) {
		SocketChannel socketChannel = (SocketChannel) key.channel();
		try {
			socketChannel.write(inputBuffer);
			
			if (!inputBuffer.hasRemaining() && getClientState() != ClientHandlerState.REQUEST_PROCESSED)
				key.interestOps(0);
			
			if (!inputBuffer.hasRemaining() && getClientState() == ClientHandlerState.REQUEST_PROCESSED) {
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
}
