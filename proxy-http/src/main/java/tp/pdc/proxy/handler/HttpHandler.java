package tp.pdc.proxy.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tp.pdc.proxy.bytes.ByteBufferFactory;
import tp.pdc.proxy.handler.interfaces.Handler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

/**
 * Provides the handle methods and is in charge for the flip and compact of the buffers.
 */
public abstract class HttpHandler implements Handler {
	private static final Logger LOGGER = LoggerFactory.getLogger(HttpHandler.class);
	private static final ByteBufferFactory BUFFER_FACTORY = ByteBufferFactory.getInstance();

	private ByteBuffer readBuffer;
	private ByteBuffer writeBuffer;
	private ByteBuffer processedBuffer;
	private SelectionKey connectedPeerKey;

	public HttpHandler (ByteBuffer writeBuffer, ByteBuffer processedBuffer) {
		this.readBuffer = BUFFER_FACTORY.getProxyBuffer();
		this.writeBuffer = writeBuffer;
		this.processedBuffer = processedBuffer;
	}

	public HttpHandler () {
		this.readBuffer = BUFFER_FACTORY.getProxyBuffer();
		this.writeBuffer = BUFFER_FACTORY.getProxyBuffer();
		this.processedBuffer = BUFFER_FACTORY.getProxyBuffer();
	}

	abstract protected void processRead (SelectionKey key);

	abstract protected void process (ByteBuffer inputBuffer, SelectionKey key);

	abstract protected void processWrite (ByteBuffer inputBuffer, SelectionKey key);

	public ByteBuffer getReadBuffer () {
		return readBuffer;
	}

	public ByteBuffer getWriteBuffer () {
		return writeBuffer;
	}

	public void setWriteBuffer (ByteBuffer writeBuffer) {
		this.writeBuffer = writeBuffer;
	}

	public ByteBuffer getProcessedBuffer () {
		return processedBuffer;
	}

	public void setProcessedBuffer (ByteBuffer processedBuffer) {
		this.processedBuffer = processedBuffer;
	}

	public SelectionKey getConnectedPeerKey () {
		return connectedPeerKey;
	}

	public void setConnectedPeerKey (SelectionKey connectedPeerKey) {
		this.connectedPeerKey = connectedPeerKey;
	}

	/**
	 * Handles read for an specific key
	 * @param key
     */
	public void handleRead (SelectionKey key) {
		processRead(key);

		if (key.isValid()) // in case EOF was received
			processReadBuffer(key);
	}

	/**
	 * Handles write for an specific key
	 * @param key
     */
	public void handleWrite (SelectionKey key) {
		writeBuffer.flip();
		processWrite(writeBuffer, key);
		writeBuffer.compact();
	}

	/**
	 * Handles process for an specific key
	 * @param key
	 * @param buffer
     */
	public void handleProcess (SelectionKey key, ByteBuffer buffer) {
		if (key.isValid() && readBuffer.position() != 0) {
			buffer.compact();
			processReadBuffer(key);
			buffer.flip();
		}
	}

	/**
	 * Process the read buffer for a key
	 * @param key
     */
	private void processReadBuffer (SelectionKey key) {
		readBuffer.flip();
		process(readBuffer, key);
		readBuffer.compact();
	}

	/**
	 * Gets an {@link InetSocketAddress} from a {@link SelectionKey}
	 * @param key
	 * @return the address for the key
     */
	protected InetSocketAddress addressFromKey (SelectionKey key) {
		SocketChannel socketChannel = (SocketChannel) key.channel();
		try {
			return (InetSocketAddress) socketChannel.getRemoteAddress();
		} catch (IOException e) {
			LOGGER.error("Failed to retrieve remote address from socket channel");
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Closes the socket channel
	 * @param socketChannel
     */
	protected void closeChannel (SelectableChannel socketChannel) {
		try {
			socketChannel.close();
		} catch (IOException e1) {
			LOGGER.error("Failed to close handler's socket: {}", e1.getMessage());
		}
	}
}
