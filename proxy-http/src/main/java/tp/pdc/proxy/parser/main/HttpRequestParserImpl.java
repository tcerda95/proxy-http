package tp.pdc.proxy.parser.main;

import tp.pdc.proxy.exceptions.ParserFormatException;
import tp.pdc.proxy.header.Header;
import tp.pdc.proxy.header.Method;
import tp.pdc.proxy.parser.body.HttpNullBodyParser;
import tp.pdc.proxy.parser.component.HttpHeaderParserImpl;
import tp.pdc.proxy.parser.component.HttpRequestLineParserImpl;
import tp.pdc.proxy.parser.factory.HttpBodyParserFactory;
import tp.pdc.proxy.parser.interfaces.HttpBodyParser;
import tp.pdc.proxy.parser.interfaces.HttpHeaderParser;
import tp.pdc.proxy.parser.interfaces.HttpRequestLineParser;
import tp.pdc.proxy.parser.interfaces.HttpRequestParser;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

public class HttpRequestParserImpl implements HttpRequestParser {

	private static final HttpBodyParserFactory BODY_PARSER_FACTORY =
		HttpBodyParserFactory.getInstance();

	private HttpRequestLineParser requestLineParser;
	private HttpHeaderParser headersParser;
	private HttpBodyParser bodyParser;

	public HttpRequestParserImpl (Map<Header, byte[]> toAdd, Set<Header> toRemove,
		Set<Header> toSave) {
		headersParser = new HttpHeaderParserImpl(toAdd, toRemove, toSave);
		requestLineParser = new HttpRequestLineParserImpl();
		bodyParser = HttpNullBodyParser.getInstance();
	}

	@Override
	public boolean hasHeaderValue (Header header) {
		return headersParser.hasHeaderValue(header);
	}

	@Override
	public byte[] getHeaderValue (Header header) {
		return headersParser.getHeaderValue(header);
	}

	@Override
	public byte[] getHostValue () {
		if (!hasHost())
			throw new NoSuchElementException("Host not read yet");
		if (requestLineParser.hasHost())
			return requestLineParser.getHostValue();
		else
			return headersParser.getHeaderValue(Header.HOST);
	}

	@Override
	public byte[] getWholeRequestLine () {
		return requestLineParser.getWholeRequestLine();
	}

	@Override
	public boolean hasRequestLineFinished () {
		return requestLineParser.hasFinished();
	}

	@Override
	public boolean hasHeadersFinished () {
		return hasRequestLineFinished() && headersParser.hasFinished();
	}

	@Override
	public boolean hasFinished () {
		return hasRequestLineFinished() && hasHeadersFinished() && bodyParser.hasFinished();
	}

	@Override
	public boolean hasMethod () {
		return requestLineParser.hasMethod();
	}

	@Override
	public boolean hasHost () {
		return requestLineParser.hasHost() || headersParser.hasHeaderValue(Header.HOST);
	}

	@Override
	public void reset () {
		headersParser.reset();
		requestLineParser.reset();
		bodyParser = HttpNullBodyParser.getInstance();
	}

	@Override
	public boolean parse (final ByteBuffer input, final ByteBuffer output)
		throws ParserFormatException {
		while (input.hasRemaining() && output.hasRemaining()) {

			if (!requestLineParser.hasFinished()) {
				requestLineParser.parse(input, output);

			} else if (!headersParser.hasFinished()) {

				if (headersParser.parse(input, output)) {
					bodyParser = BODY_PARSER_FACTORY.getClientHttpBodyParser(this);
					return bodyParser.parse(input, output);
				}

			} else if (!bodyParser.hasFinished()) {

				if (bodyParser.parse(input, output))
					return true;
			} else {
				throw new IllegalStateException("Already finished parsing");
			}
		}

		return false;
	}

	@Override
	public Method getMethod () {
		return requestLineParser.getMethod();
	}

	@Override
	public boolean readMinorVersion () {
		return requestLineParser.readMinorVersion();
	}

	@Override
	public boolean readMajorVersion () {
		return requestLineParser.readMajorVersion();
	}

	@Override
	public int getMajorHttpVersion () {
		return requestLineParser.getMajorHttpVersion();
	}

	@Override
	public int getMinorHttpVersion () {
		return requestLineParser.getMinorHttpVersion();
	}

	@Override
	public byte[] getWholeVersionBytes () {
		return requestLineParser.getWholeVersionBytes();
	}
}
