package tp.pdc.proxy.parser.protocol;

import tp.pdc.proxy.bytes.ByteBufferFactory;
import tp.pdc.proxy.bytes.BytesUtils;
import tp.pdc.proxy.flag.L33tFlag;
import tp.pdc.proxy.header.Method;
import tp.pdc.proxy.header.protocol.PopisHeader;
import tp.pdc.proxy.metric.interfaces.ClientMetric;
import tp.pdc.proxy.metric.interfaces.ServerMetric;
import tp.pdc.proxy.parser.utils.ParseUtils;
import tp.pdc.proxy.properties.ProxyProperties;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Set;

import static tp.pdc.proxy.parser.utils.AsciiConstants.*;


public class PopisOutputGenerator {
	private static final Charset charset = ProxyProperties.getInstance().getCharset();
	private static final ByteBufferFactory BUFFER_FACTORY = ByteBufferFactory.getInstance();
	private static final ProxyProperties PROXY_PROPERTIES = ProxyProperties.getInstance();
	private static final L33tFlag L33TFLAG = L33tFlag.getInstance();

	private static final int PROTOCOL_PARSER_BUFFER_SIZE = PROXY_PROPERTIES.getProtocolParserBufferSize();
	private static final String PONG = "PONG";
	private final ClientMetric clientMetrics;
	private final ServerMetric serverMetrics;
	//bytes that couldn't be put in the output buffer because it was full
	private ByteBuffer remainingBytes;

	public PopisOutputGenerator (ClientMetric clientMetrics, ServerMetric serverMetrics) {
		this.clientMetrics = clientMetrics;
		this.serverMetrics = serverMetrics;
		remainingBytes = ByteBuffer.allocate(PROTOCOL_PARSER_BUFFER_SIZE);
	}

	public void generateOutput (PopisHeader header, ByteBuffer output) {

		if (header != PopisHeader.PING)
			putField(header.getBytes(), output);

		switch (header) {

			case PROXY_BUF_SIZE:

				int proxyBufferSize = BUFFER_FACTORY.getProxyBufferSize();
				putValue(proxyBufferSize, output);
				break;

			case L33TENABLE:

				L33TFLAG.set();
				break;

			case L33TDISABLE:

				L33TFLAG.unset();
				break;

			case ISL33TENABLE:

				String toPut = L33tFlag.getInstance().isSet() ? "YES" : "NO";
				putValue(toPut.getBytes(charset), output);
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

			case SET_PROXY_BUF_SIZE:
				break;

			case METHOD_COUNT:
				break;

			case STATUS_CODE_COUNT:
				break;

			case METRICS:
				addAllMetrics(output);
				break;

			case PING:
				putField(PONG.getBytes(), output);
				break;

			case END:
				break;
		}

		if (header != PopisHeader.METRICS)
			putCRLF(output);
	}

	public void generateOutput (Method method, ByteBuffer output) {

		putField(method.getBytes(), output);

		int methodCount = clientMetrics.getMethodCount(method);
		putValue(methodCount, output);

		putCRLF(output);
	}


	public void generateOutput (int number, ByteBuffer output, PopisHeader currentHeader) {

		putField(number, output);

		if (currentHeader == PopisHeader.SET_PROXY_BUF_SIZE)
			BUFFER_FACTORY.setProxyBufferSize(number);
		else {
			int statusCodeCount = serverMetrics.getResponseCodeCount(number);
			putValue(statusCodeCount, output);
		}

		putCRLF(output);
	}

	public void generateOutput (ByteBuffer input, ByteBuffer output) {

		put(PS.getValue(), output);
		put(input, output);

		putCRLF(output);
	}

	public void generateErrorOutput (ByteBuffer input, PopisError errorCode, ByteBuffer output) {

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

	private void putField (byte[] bytes, ByteBuffer output) {

		put(PS.getValue(), output);
		put(bytes, output);
	}

	private void putField (int number, ByteBuffer output) {

		put(PS.getValue(), output);

		put(number, output);
	}

	private void putValue (byte[] bytes, ByteBuffer output) {

		put(DP.getValue(), output);
		put(SP.getValue(), output);
		put(bytes, output);
	}

	private void putValue (int number, ByteBuffer output) {

		put(DP.getValue(), output);
		put(SP.getValue(), output);

		put(number, output);
	}

	private void putValue (long number, ByteBuffer output) {

		put(DP.getValue(), output);
		put(SP.getValue(), output);

		put(number, output);
	}

	private void putCRLF (ByteBuffer output) {

		put(CR.getValue(), output);
		put(LF.getValue(), output);
	}

	private void putMightContinue (ByteBuffer output) {
		put(OB.getValue(), output);
		put(PT.getValue(), output);
		put(PT.getValue(), output);
		put(PT.getValue(), output);
		put(CB.getValue(), output);

		putCRLF(output);
	}

	private void putEnd (ByteBuffer output) {
		generateOutput(PopisHeader.END, output);
	}

	private void put (byte[] input, ByteBuffer output) {

		putRemainingBytes(output);

		if (output.remaining() >= input.length)
			output.put(input);
		else {
			int remainingOutput = output.remaining();
			lengthPut(input, output, remainingOutput);
			remainingBytes.put(input, remainingOutput, input.length - remainingOutput);
		}
	}

	private void put (ByteBuffer input, ByteBuffer output) {

		putRemainingBytes(output);

		if (output.remaining() >= input.remaining())
			output.put(input);
		else {
			lengthPut(input, output, output.remaining());
			remainingBytes.put(input);
		}
	}

	private void put (byte c, ByteBuffer output) {

		putRemainingBytes(output);

		if (output.hasRemaining())
			output.put(c);
		else
			remainingBytes.put(c);
	}

	private void put (int number, ByteBuffer output) {
		ParseUtils.parseInt(number, output, remainingBytes);
	}

	private void put (long number, ByteBuffer output) {
		ParseUtils.parseLong(number, output, remainingBytes);
	}

	private void putArgCount (int count, ByteBuffer output) {
		put(PS.getValue(), output);
		put(AS.getValue(), output);
		put(count, output);
		putCRLF(output);
	}

	private void putRemainingBytes (ByteBuffer output) {
		if (remainingBytes()) {
			remainingBytes.flip();

			if (output.remaining() >= remainingBytes.remaining())
				output.put(remainingBytes);
			else
				lengthPut(remainingBytes, output, output.remaining());

			remainingBytes.compact();
		}
	}

	private void addAllMetrics (ByteBuffer output) {

		putCRLF(output);

		for (PopisHeader header : PopisHeader.values()) {

			if (header != PopisHeader.END && header != PopisHeader.METRICS
				&& header != PopisHeader.PING && !setter(header))
				generateOutput(header, output);

			switch (header) {

				case METHOD_COUNT:

					Set<Method> m = clientMetrics.getMethods();

					putArgCount(m.size(), output);

					for (Method method : m)
						generateOutput(method, output);

					break;

				case STATUS_CODE_COUNT:

					Set<Integer> s = serverMetrics.getStatusCodes();

					putArgCount(s.size(), output);

					for (Integer statusCode : s)
						generateOutput(statusCode, output, PopisHeader.STATUS_CODE_COUNT);

					break;

				default:
					break;
			}
		}
	}

	private void lengthPut (ByteBuffer input, ByteBuffer output, int length) {
		BytesUtils.lengthPut(input, output, length);
	}

	private void lengthPut (byte[] input, ByteBuffer output, int length) {
		BytesUtils.lengthPut(input, output, length);
	}

	private boolean setter (PopisHeader header) {
		return (header == PopisHeader.L33TENABLE ||
			header == PopisHeader.L33TDISABLE ||
			header == PopisHeader.SET_PROXY_BUF_SIZE);
	}

	public void reset () {
		remainingBytes.clear();
	}

	private boolean remainingBytes () {
		return remainingBytes.position() != 0;
	}
	
	public boolean hasFinished () {
		//remainingBytes buffer is always in write mode
		return !remainingBytes();
	}
}
