package tp.pdc.proxy.parser.protocol;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

import tp.pdc.proxy.L33tFlag;
import tp.pdc.proxy.ProxyProperties;
import tp.pdc.proxy.header.Method;
import tp.pdc.proxy.header.protocol.CrazyProtocolHeader;
import tp.pdc.proxy.metric.ClientMetricImpl;
import tp.pdc.proxy.metric.ServerMetricImpl;

public class CrazyProtocolOutputGenerator {
	
	private static ProxyProperties PROPERTIES = ProxyProperties.getInstance();
	
	private static final String REPEATED = "repeated";
	
	private ClientMetricImpl clientMetrics;
	private ServerMetricImpl serverMetrics;
	
	// bytes that couldn't be put in the output buffer because it was full
	private List<Byte> remainingBytes;
	
//	private Set<CrazyProtocolHeader> crazyProtocolheadersFound;
//	private Set<Integer> HttpstatusCodesFound;
//	private Set<Method> HttpmethodsFound;
	
		
//	public CrazyProtocolOutputGenerator(Set<CrazyProtocolHeader> headers, Set<Integer> statusCodes,
//			Set<Method> methods, ByteBuffer output) {
//		
//		this.crazyProtocolheadersFound = headers;
//		this.HttpstatusCodesFound = statusCodes;
//		this.HttpmethodsFound = methods;
//		this.output = output;
//	}
	
	public CrazyProtocolOutputGenerator() {
		clientMetrics = ClientMetricImpl.getInstance();
		serverMetrics = ServerMetricImpl.getInstance();
		remainingBytes = new LinkedList<Byte>();
	}
	
//	public void generateOutput() {
//		
//		if (crazyProtocolheadersFound.contains(CrazyProtocolHeader.METRICS)) {
//			addAllMetrics();
//			crazyProtocolheadersFound.remove(CrazyProtocolHeader.METRICS);
//		}
//		
//		for (CrazyProtocolHeader header : crazyProtocolheadersFound) {
//			generateMethodOutput(header);
//		}
//		
//	}
	
	/*Cachear cosas que no entren todo de una en el buffer, ponerlas hasta llenarlo y la proxima
	 * vez que se llame a esta clase la misma deberia poner el header
	 */

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
				break;
				
			case END:				
				break;
				
			default:
				break;
		}
		
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
	
	public void generateOutput(ByteBuffer output) {
		
		putHeader(REPEATED.getBytes(PROPERTIES.getCharset()), output);
		putCRLF(output);
	}
	
	private void putHeader(byte[] bytes, ByteBuffer output) {
		
		putRemainingBytes(output);
		
		put((byte) '+', output);
		put(bytes, output);
	}
	
	private void putValue(byte[] bytes, ByteBuffer output) {
		
		putRemainingBytes(output);
		
		put((byte) ':', output);
		put((byte) ' ', output);
		put(bytes, output);
	}
	
	
	private void put(byte[] bytes, ByteBuffer output) {
		
		for (byte c : bytes)
			put(c, output);
	}
	
	private void put(byte c, ByteBuffer output) {
		
		if (!output.hasRemaining())
			remainingBytes.add(c);
		
		output.put(c);
	}
	
	private void putRemainingBytes(ByteBuffer output) {
		
		for (byte c : remainingBytes) {
			
			if (output.hasRemaining())
				output.put(c);
			else
				return;
		}
	}
	
	private void putCRLF(ByteBuffer output) {
		put((byte) '\r', output);
		put((byte) '\n', output);
	}
	
	public void reset() {
		remainingBytes.clear();
	}
	
	public boolean hasFinished() {
		return remainingBytes.isEmpty();
	}

//	private void addAllMetrics() {
//		for (CrazyProtocolHeader h : CrazyProtocolHeader.values()) {
//			if (!crazyProtocolheadersFound.contains(h))
//				crazyProtocolheadersFound.add(h);
//		}
//		
//		for (Method m : Method.values()) {
//			if (!HttpmethodsFound.contains(m))
//				HttpmethodsFound.add(m);
//		}
//		
//		for (Integer statusCode : serverMetrics.statusCodesFound()) {
//			if (!HttpstatusCodesFound.contains(statusCode))
//				HttpstatusCodesFound.add(statusCode);
//		}
//	}
}
