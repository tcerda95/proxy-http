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
	
	private ClientMetricImpl clientMetrics;
	private ServerMetricImpl serverMetrics;
	
	private Set<CrazyProtocolHeader> crazyProtocolheadersFound;
	private Set<Integer> HttpstatusCodesFound;
	private Set<Method> HttpmethodsFound;
	
	private ByteBuffer output;
	
	private boolean parserHasNoErros;
	
	public CrazyProtocolOutputGenerator(Set<CrazyProtocolHeader> headers, Set<Integer> statusCodes,
			Set<Method> methods, ByteBuffer output, boolean parserHasNoErrors) {
		
		this.crazyProtocolheadersFound = headers;
		this.HttpstatusCodesFound = statusCodes;
		this.HttpmethodsFound = methods;
		this.output = output;
	}
	
	public void generateOutput() {
		
		if (crazyProtocolheadersFound.contains(CrazyProtocolHeader.METRICS)) {
			addAllMetrics();
			crazyProtocolheadersFound.remove(CrazyProtocolHeader.METRICS);
		}
		
		for (CrazyProtocolHeader header : crazyProtocolheadersFound) {
			generateMethodOutput(header);
		}
		
	}

	public void generateMethodOutput(CrazyProtocolHeader header) {
		
		output.put((byte) '+');
		output.put(header.getBytes());
				
		switch (header) {
		
			case L33TENABLE:
				
				L33tFlag.getInstance().set();
				putCRLF();
				break;
				
			case L33TDISABLE:
				
				L33tFlag.getInstance().unset();
				putCRLF();
				break;
				
			case ISL33TENABLE:
				
				put(L33tFlag.getInstance().isSet() ? ": YES" : ": NO");
				putCRLF();
				break;
				
			case CLIENT_BYTES_READ:
				
				put(": ");
				Long clientBytesRead = clientMetrics.getBytesRead();
				output.put(clientBytesRead.byteValue());
				putCRLF();
				break;
				
			case CLIENT_BYTES_WRITTEN:
				
				put(": ");
				Long clientBytesWritten = clientMetrics.getBytesWritten();
				output.put(clientBytesWritten.byteValue());
				putCRLF();
				break;

			case CLIENT_CONNECTIONS:
				
				put(": ");
				Long clientConnections = (clientMetrics.getConnections());
				output.put(clientConnections.byteValue());
				putCRLF();
				break;
				
			case SERVER_BYTES_READ:
				
				put(": ");
				Long serverBytesRead = clientMetrics.getBytesRead();
				output.put(serverBytesRead.byteValue());
				putCRLF();
				break;
				
			case SERVER_BYTES_WRITTEN:
				
				put(": ");
				Long serverBytesWritten = clientMetrics.getBytesWritten();
				output.put(serverBytesWritten.byteValue());
				putCRLF();
				break;

			case SERVER_CONNECTIONS:
				
				put(": ");
				Long serverConnections = (clientMetrics.getConnections());
				output.put(serverConnections.byteValue());
				putCRLF();
				break;
				
			case METHOD_COUNT:
				
				putCRLF();
				
				for (Method m :HttpmethodsFound) {
					output.put(m.getBytes());
					put(": ");
					Integer count = clientMetrics.getMethodCount(m);
					output.put(count.byteValue());
					putCRLF();
				}
				
				break;
				
			case STATUS_CODE_COUNT:
				
				putCRLF();
				
				for (Integer statusCode : HttpstatusCodesFound) {
					output.put(statusCode.byteValue());
					put(": ");
					Integer count = serverMetrics.getResponseCodeCount(statusCode);
					output.put(count.byteValue());
					putCRLF();
				}
				
				break;
				
			case METRICS:
				break;
				
			case END:
				break;
				
			default:
				put("What?");
		}
	}
	
	private void addAllMetrics() {
		for (CrazyProtocolHeader h : CrazyProtocolHeader.values()) {
			if (!crazyProtocolheadersFound.contains(h))
				crazyProtocolheadersFound.add(h);
		}
		
		for (Method m : Method.values()) {
			if (!HttpmethodsFound.contains(m))
				HttpmethodsFound.add(m);
		}
		
		for (Integer statusCode : serverMetrics.statusCodesFound()) {
			if (!HttpstatusCodesFound.contains(statusCode))
				HttpstatusCodesFound.add(statusCode);
		}
	}
	
	private void put(String s) {
		output.put(s.getBytes(PROPERTIES.getCharset()));
	}
	
	private void putCRLF() {
		output.put((byte) '\r');
		output.put((byte) '\n');
	}
}
