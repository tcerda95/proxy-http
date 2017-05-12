package tp.pdc.proxy.parser.protocol;

import java.nio.ByteBuffer;
import java.util.Set;

import tp.pdc.proxy.L33tFlag;
import tp.pdc.proxy.ProxyProperties;
import tp.pdc.proxy.header.Method;
import tp.pdc.proxy.header.protocol.CrazyProtocolHeader;
import tp.pdc.proxy.metric.ClientMetricImpl;
import tp.pdc.proxy.metric.ServerMetricImpl;

public class CrazyProtocolOutputGenerator {
	
	private static ProxyProperties PROPERTIES = ProxyProperties.getInstance();
	
	private static final String REPEATED = "Repeated";
	
	private ClientMetricImpl clientMetrics;
	private ServerMetricImpl serverMetrics;
	
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
		
		output.put((byte) '+');
		output.put(header.getBytes());
				
		switch (header) {
		
			case L33TENABLE:
				
				L33tFlag.getInstance().set();
				putCRLF(output);
				break;
				
			case L33TDISABLE:
				
				L33tFlag.getInstance().unset();
				putCRLF(output);
				break;
				
			case ISL33TENABLE:
				
				put(L33tFlag.getInstance().isSet() ? ": YES" : ": NO", output);
				putCRLF(output);
				break;
				
			case CLIENT_BYTES_READ:
				
				put(": ", output);
				Long clientBytesRead = clientMetrics.getBytesRead();
				output.put(clientBytesRead.byteValue());
				putCRLF(output);
				break;
				
			case CLIENT_BYTES_WRITTEN:
				
				put(": ", output);
				Long clientBytesWritten = clientMetrics.getBytesWritten();
				output.put(clientBytesWritten.byteValue());
				putCRLF(output);
				break;

			case CLIENT_CONNECTIONS:
				
				put(": ", output);
				Long clientConnections = (clientMetrics.getConnections());
				output.put(clientConnections.byteValue());
				putCRLF(output);
				break;
				
			case SERVER_BYTES_READ:
				
				put(": ", output);
				Long serverBytesRead = clientMetrics.getBytesRead();
				output.put(serverBytesRead.byteValue());
				putCRLF(output);
				break;
				
			case SERVER_BYTES_WRITTEN:
				
				put(": ", output);
				Long serverBytesWritten = clientMetrics.getBytesWritten();
				output.put(serverBytesWritten.byteValue());
				putCRLF(output);
				break;

			case SERVER_CONNECTIONS:
				
				put(": ", output);
				Long serverConnections = (clientMetrics.getConnections());
				output.put(serverConnections.byteValue());
				putCRLF(output);
				break;
				
			case METHOD_COUNT:
				
				putCRLF(output);
				break;
				
			case STATUS_CODE_COUNT:
				
				putCRLF(output);
				break;
				
			case METRICS:
				
				putCRLF(output);
				break;
				
			case END:
				
				putCRLF(output);
				break;
				
			default:
				put("What?", output);
		}
	}
	
	public void generateOutput(Method method, ByteBuffer output) {
		
		output.put((byte) '+');
		output.put(method.getBytes());
		
		Integer methodCount = clientMetrics.getMethodCount(method);
		output.put(methodCount.byteValue());
		putCRLF(output);
	}
	
	public void generateOutput(Integer statusCode, ByteBuffer output) {
		
		output.put((byte) '+');
		output.put(statusCode.byteValue());
		
		Integer statusCodeCount = serverMetrics.getResponseCodeCount(statusCode);
		output.put(statusCodeCount.byteValue());
		putCRLF(output);
	}
	
	public void generateOutput(ByteBuffer output) {
		output.put(REPEATED.getBytes(PROPERTIES.getCharset()));
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
	
	private void put(String s, ByteBuffer output) {
		output.put(s.getBytes(PROPERTIES.getCharset()));
	}
	
	private void putCRLF(ByteBuffer output) {
		output.put((byte) '\r');
		output.put((byte) '\n');
	}
}
