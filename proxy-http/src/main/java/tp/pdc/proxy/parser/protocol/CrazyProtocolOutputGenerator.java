package tp.pdc.proxy.parser.protocol;

import static tp.pdc.proxy.parser.utils.AsciiConstants.*;
import java.nio.ByteBuffer;
import tp.pdc.proxy.L33tFlag;
import tp.pdc.proxy.ProxyProperties;
import tp.pdc.proxy.header.BytesUtils;
import tp.pdc.proxy.header.Method;
import tp.pdc.proxy.header.protocol.CrazyProtocolHeader;
import tp.pdc.proxy.metric.ClientMetricImpl;
import tp.pdc.proxy.metric.ServerMetricImpl;
import tp.pdc.proxy.parser.utils.ParseUtils;


public class CrazyProtocolOutputGenerator {
	
	private static ProxyProperties PROPERTIES = ProxyProperties.getInstance();
		
	private ClientMetricImpl clientMetrics;
	private ServerMetricImpl serverMetrics;
	private L33tFlag l33tFlag;
	
	//bytes that couldn't be put in the output buffer because it was full
	private ByteBuffer remainingBytes;
	
	public CrazyProtocolOutputGenerator() {
		clientMetrics = ClientMetricImpl.getInstance();
		serverMetrics = ServerMetricImpl.getInstance();
		l33tFlag = L33tFlag.getInstance();
		//TODO: sacarlo de properties
		remainingBytes = ByteBuffer.allocate(4000);
	}
	
	public void generateOutput(CrazyProtocolHeader header, ByteBuffer output) {
		
		putField(header.getBytes(), output);
				
		switch (header) {
		
			case L33TENABLE:
				
				l33tFlag.set();
				break;
				
			case L33TDISABLE:
				
				l33tFlag.unset();
				break;
				
			case ISL33TENABLE:
				
				String toPut = L33tFlag.getInstance().isSet() ? "YES" : "NO";
				putValue(toPut.getBytes(PROPERTIES.getCharset()), output);
				break;
				
			case CLIENT_BYTES_READ:
				
				long clientBytesRead = clientMetrics.getBytesRead();
				putValue(clientBytesRead, output);
				break;
				
			case CLIENT_BYTES_WRITTEN:
				
				long clientBytesWritten = clientMetrics.getBytesWritten();
				putValue(clientBytesWritten, output);
				break;

			case CLIENT_CONNECTIONS:
				
				long clientConnections = (clientMetrics.getConnections());
				putValue(clientConnections, output);
				break;
				
			case SERVER_BYTES_READ:
				
				long serverBytesRead = serverMetrics.getBytesRead();
				putValue(serverBytesRead, output);
				break;
				
			case SERVER_BYTES_WRITTEN:
				
				long serverBytesWritten = serverMetrics.getBytesWritten();
				putValue(serverBytesWritten, output);
				break;

			case SERVER_CONNECTIONS:
				
				long serverConnections = serverMetrics.getConnections();
				putValue(serverConnections, output);
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
		
		putField(method.getBytes(), output);
		
		int methodCount = clientMetrics.getMethodCount(method);
		putValue(methodCount, output);
		
		putCRLF(output);
	}
	
	public void generateOutput(int statusCode, ByteBuffer output) {
		
		putField(statusCode, output);
				
		int statusCodeCount = serverMetrics.getResponseCodeCount(statusCode);
		putValue(statusCodeCount, output);
		
		putCRLF(output);
	}

	public void generateOutput(ByteBuffer input, ByteBuffer output) {
	
		put(PS.getValue(), output);
		put(input, output);
	
		putCRLF(output);
	}
	
	public void generateErrorOutput(ByteBuffer input, CrazyProtocolError errorCode, ByteBuffer output) {
		
		put(LS.getValue(), output);
		
		put(errorCode.getBytes(), output);
		
		put(input, output);
		
		switch (errorCode) {
		
			case NO_MATCH:
				putCRLF(output);
				break;
				
			default:
				putMightContinue(output);
				putEnd(output);
				break;
		}
	}
	
	private void putField(byte[] bytes, ByteBuffer output) {
				
		put(PS.getValue(), output);
		put(bytes, output);
	}
	
	private void putField(int number, ByteBuffer output) {
		
		put(PS.getValue(), output);
		
		ParseUtils.parseInt(number, output, remainingBytes);
	}
	
	private void putValue(byte[] bytes, ByteBuffer output) {
				
		put(DP.getValue(), output);
		put(SP.getValue(), output);
		put(bytes, output);
	}
	
	private void putValue(int number, ByteBuffer output) {
		
		put(DP.getValue(), output);
		put(SP.getValue(), output);

		ParseUtils.parseInt(number, output, remainingBytes);
	}
	
	private void putValue(long number, ByteBuffer output) {
		
		put(DP.getValue(), output);
		put(SP.getValue(), output);

		ParseUtils.parseLong(number, output, remainingBytes);
	}
	
	private void putCRLF(ByteBuffer output) {
				
		put(CR.getValue(), output);
		put(LF.getValue(), output);
	}
	
	private void putMightContinue(ByteBuffer output) {
		put(OB.getValue(), output);
		put(PT.getValue(), output);
		put(PT.getValue(), output);
		put(PT.getValue(), output);
		put(CB.getValue(), output);
		
		putCRLF(output);
	}
	
	private void putEnd(ByteBuffer output) {
		generateOutput(CrazyProtocolHeader.END, output);
	}
	
	private void put(byte[] input, ByteBuffer output) {

		putRemainingBytes(output);
		
		if (output.remaining() >= input.length)
			output.put(input);
		else {
			int remainingOutput = output.remaining();
			lengthPut(input, output, remainingOutput);
			remainingBytes.put(input, remainingOutput, input.length-remainingOutput);	
		}
	}
	
	private void put(ByteBuffer input, ByteBuffer output) {
		
		putRemainingBytes(output);
		
		if (output.remaining() >= input.remaining())
			output.put(input);
		else {
			lengthPut(input, output, output.remaining());
			remainingBytes.put(input);
		}
	}
	
	private void put(byte c, ByteBuffer output) {
		
		putRemainingBytes(output);
		
		if (output.remaining() >= 1)
			output.put(c);
		else
			remainingBytes.put(c);
	}	

	private void putRemainingBytes(ByteBuffer output) {
		if (remainingBytes()) {
			remainingBytes.flip();
			
			if (output.remaining() >= remainingBytes.remaining())
				output.put(remainingBytes);
			else
				lengthPut(remainingBytes, output, output.remaining());
			
			remainingBytes.compact();
		}
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
	
	private void lengthPut(ByteBuffer input, ByteBuffer output, int length) {
		BytesUtils.lengthPut(input, output, length);
	}
	
	private void lengthPut(byte[] input, ByteBuffer output, int length) {
		BytesUtils.lengthPut(input, output, length);
	}
	
	
	private boolean remainingBytes() {
		return remainingBytes.position() != 0;
	}
	
	private boolean isFlag(CrazyProtocolHeader header) {
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
