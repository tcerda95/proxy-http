package tp.pdc.proxy.parser.protocol;

import java.nio.ByteBuffer;
import tp.pdc.proxy.L33tFlag;
import tp.pdc.proxy.ProxyProperties;
import tp.pdc.proxy.header.Method;
import tp.pdc.proxy.header.protocol.CrazyProtocolHeader;
import tp.pdc.proxy.metric.ClientMetricImpl;
import tp.pdc.proxy.metric.ServerMetricImpl;
import tp.pdc.proxy.parser.utils.ParseUtils;

public class CrazyProtocolOutputGenerator {
	
	private static ProxyProperties PROPERTIES = ProxyProperties.getInstance();
		
	private ClientMetricImpl clientMetrics;
	private ServerMetricImpl serverMetrics;
	
	//bytes that couldn't be put in the output buffer because it was full
	private ByteBuffer remainingBytes;
	
	public CrazyProtocolOutputGenerator() {
		clientMetrics = ClientMetricImpl.getInstance();
		serverMetrics = ServerMetricImpl.getInstance();
		//TODO: sacarlo de properties
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
				
				long clientBytesRead = clientMetrics.getBytesRead();
				putValue(ParseUtils.parseLong(clientBytesRead), output);
				break;
				
			case CLIENT_BYTES_WRITTEN:
				
				long clientBytesWritten = clientMetrics.getBytesWritten();
				putValue(ParseUtils.parseLong(clientBytesWritten), output);
				break;

			case CLIENT_CONNECTIONS:
				
				long clientConnections = (clientMetrics.getConnections());
				putValue(ParseUtils.parseLong(clientConnections), output);
				break;
				
			case SERVER_BYTES_READ:
				
				long serverBytesRead = serverMetrics.getBytesRead();
				putValue(ParseUtils.parseLong(serverBytesRead), output);
				break;
				
			case SERVER_BYTES_WRITTEN:
				
				long serverBytesWritten = serverMetrics.getBytesWritten();
				putValue(ParseUtils.parseLong(serverBytesWritten), output);
				break;

			case SERVER_CONNECTIONS:
				
				long serverConnections = serverMetrics.getConnections();
				putValue(ParseUtils.parseLong(serverConnections), output);
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
		}
		
		if (header != CrazyProtocolHeader.METRICS)
			putCRLF(output);
	}
	
	public void generateOutput(Method method, ByteBuffer output) {
		
		putHeader(method.getBytes(), output);
		
		int methodCount = clientMetrics.getMethodCount(method);
		putValue(ParseUtils.parseInt(methodCount), output);
		
		putCRLF(output);
	}
	
	public void generateOutput(Integer statusCode, ByteBuffer output) {
		
		putHeader(ParseUtils.parseInt(statusCode), output);
				
		int statusCodeCount = serverMetrics.getResponseCodeCount(statusCode);
		putValue(ParseUtils.parseInt(statusCodeCount), output);
		
		putCRLF(output);
	}
	
	public void generateOutput(byte c, CrazyProtocolInputError errorCode, ByteBuffer output) {
		ByteBuffer characterWrapped = ByteBuffer.allocate(1);
		characterWrapped.put(c);
		characterWrapped.flip();
		generateOutput(characterWrapped, errorCode, output);
	}
	
	public void generateOutput(byte[] input, CrazyProtocolInputError errorCode, ByteBuffer output) {
		generateOutput(ByteBuffer.wrap(input), errorCode, output);
	}
	
	public void generateOutput(ByteBuffer input, CrazyProtocolInputError errorCode, ByteBuffer output) {
		
		put((byte) '-', output);
		
		put(errorCode.getBytes(), output);
		
		put(input, output);
		
		switch (errorCode) {
		
			case NO_MATCH:
				putCRLF(output);
				break;
				
			case NOT_VALID:
				putMightContinue(output);
				putEnd(output);
				break;
				
			case TOO_LONG:
				putMightContinue(output);
				putEnd(output);
				break;
		}
	}
	
	public void generateOutput(CrazyProtocolInputError errorCode, ByteBuffer output) {
		put((byte) '-', output);
		put(errorCode.getBytes(), output);
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
	
	private void putMightContinue(ByteBuffer output) {
		put((byte) '[', output);
		put((byte) '.', output);
		put((byte) '.', output);
		put((byte) '.', output);
		put((byte) ']', output);
		
		putCRLF(output);
	}
	
	private void putEnd(ByteBuffer output) {
		generateOutput(CrazyProtocolHeader.END, output);
	}
	
	private void put(byte[] input, ByteBuffer output) {
		put(ByteBuffer.wrap(input), output);
	}
	
	private void put(ByteBuffer input, ByteBuffer output) {
		while (input.hasRemaining())
			put(input.get(), output);
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
