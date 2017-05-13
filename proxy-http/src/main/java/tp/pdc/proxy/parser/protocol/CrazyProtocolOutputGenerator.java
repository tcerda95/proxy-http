package tp.pdc.proxy.parser.protocol;

import java.nio.ByteBuffer;
import tp.pdc.proxy.L33tFlag;
import tp.pdc.proxy.ProxyProperties;
import tp.pdc.proxy.header.Method;
import tp.pdc.proxy.header.protocol.CrazyProtocolHeader;
import tp.pdc.proxy.metric.ClientMetricImpl;
import tp.pdc.proxy.metric.ServerMetricImpl;

public class CrazyProtocolOutputGenerator {
	
	private static ProxyProperties PROPERTIES = ProxyProperties.getInstance();
		
	private ClientMetricImpl clientMetrics;
	private ServerMetricImpl serverMetrics;
	
	//bytes that couldn't be put in the output buffer because it was full
	private ByteBuffer remainingBytes;
	
	public CrazyProtocolOutputGenerator() {
		clientMetrics = ClientMetricImpl.getInstance();
		serverMetrics = ServerMetricImpl.getInstance();
		remainingBytes = ByteBuffer.allocate(4000);
	}
	
	public void generateOutput(CrazyProtocolHeader header, ByteBuffer output) {
		
		putHeader(header.getBytes(), output);
				
		switch (header) {
		
			case L33TENABLE:
				
				L33tFlag.getInstance().set();
				break;
				
			case L33TDISABLE:
				
				L33tFlag.getInstance().unset();
				break;
				
			case ISL33TENABLE:
				
				String toPut = L33tFlag.getInstance().isSet() ? "YES" : "NO";
				putValue(toPut.getBytes(PROPERTIES.getCharset()), output);
				break;
				
			case CLIENT_BYTES_READ:
				
				Long clientBytesRead = clientMetrics.getBytesRead();
				putValue(clientBytesRead.toString().getBytes(PROPERTIES.getCharset()), output);
				break;
				
			case CLIENT_BYTES_WRITTEN:
				
				Long clientBytesWritten = clientMetrics.getBytesWritten();
				putValue(clientBytesWritten.toString().getBytes(PROPERTIES.getCharset()), output);
				break;

			case CLIENT_CONNECTIONS:
				
				Long clientConnections = (clientMetrics.getConnections());
				putValue(clientConnections.toString().getBytes(PROPERTIES.getCharset()), output);
				break;
				
			case SERVER_BYTES_READ:
				
				Long serverBytesRead = serverMetrics.getBytesRead();
				putValue(serverBytesRead.toString().getBytes(PROPERTIES.getCharset()), output);
				break;
				
			case SERVER_BYTES_WRITTEN:
				
				Long serverBytesWritten = serverMetrics.getBytesWritten();
				putValue(serverBytesWritten.toString().getBytes(PROPERTIES.getCharset()), output);
				break;

			case SERVER_CONNECTIONS:
				
				Long serverConnections = serverMetrics.getConnections();
				putValue(serverConnections.toString().getBytes(PROPERTIES.getCharset()), output);
				break;
				
			case METHOD_COUNT:				
				break;
				
			case STATUS_CODE_COUNT:				
				break;
				
			case METRICS:
				addAllMetrics(output);
				break;
				
			case END:				
				break;
				
			default:
				break;
		}
		
		if (header != CrazyProtocolHeader.METRICS)
			putCRLF(output);
	}
	
	public void generateOutput(Method method, ByteBuffer output) {
		
		putHeader(method.getBytes(), output);
		
		Integer methodCount = clientMetrics.getMethodCount(method);
		putValue(methodCount.toString().getBytes(PROPERTIES.getCharset()), output);
		
		putCRLF(output);
	}
	
	public void generateOutput(Integer statusCode, ByteBuffer output) {
		
		putHeader(statusCode.toString().getBytes(PROPERTIES.getCharset()), output);
				
		Integer statusCodeCount = serverMetrics.getResponseCodeCount(statusCode);
		putValue(statusCodeCount.toString().getBytes(PROPERTIES.getCharset()), output);
		
		putCRLF(output);
	}
	
	private void putHeader(byte[] bytes, ByteBuffer output) {
				
		put((byte) '+', output);
		put(bytes, output);
	}
	
	private void putValue(byte[] bytes, ByteBuffer output) {
				
		put((byte) ':', output);
		put((byte) ' ', output);
		put(bytes, output);
	}
	
	private void putCRLF(ByteBuffer output) {
				
		put((byte) '\r', output);
		put((byte) '\n', output);
	}
	
	private void put(byte[] bytes, ByteBuffer output) {
		
		for (byte c : bytes)
			put(c, output);
	}
	
	private void put(byte c, ByteBuffer output) {
		
		putRemainingBytes(output);
		
		if (output.hasRemaining())
			output.put(c);
		else
			remainingBytes.put(c);
	}	

	private void putRemainingBytes(ByteBuffer output) {
		
		remainingBytes.flip();
		
		while(output.hasRemaining() && remainingBytes.hasRemaining())
				output.put(remainingBytes.get());
		
		remainingBytes.compact();
	}	
	
	private void addAllMetrics(ByteBuffer output) {
		
		putCRLF(output);

		for (CrazyProtocolHeader header : CrazyProtocolHeader.values()) {
			
			if (header != CrazyProtocolHeader.END && header != CrazyProtocolHeader.METRICS 
					&& !isFlag(header))
				generateOutput(header, output);
		
			switch (header) {
			
				case METHOD_COUNT:
					
					for (Method method : clientMetrics.getMethods())
						generateOutput(method, output);
					
					break;
					
				case STATUS_CODE_COUNT:
					
					for (Integer statusCode : serverMetrics.getStatusCodes())
						generateOutput(statusCode, output);
					
					break;
					
				default:
					break;
			}
		}
	}
	
	public boolean isFlag(CrazyProtocolHeader header) {
		return (header == CrazyProtocolHeader.L33TENABLE ||
				header == CrazyProtocolHeader.L33TDISABLE);
	}
	
	public void reset() {
		remainingBytes.clear();
	}
	
	public boolean hasFinished() {
		//remainingBytes buffer is always in write mode
		return remainingBytes.position() == 0;
	}
}
