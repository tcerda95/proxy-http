package tp.pdc.proxy.parser;

import org.junit.Before;
import org.junit.Test;
import tp.pdc.proxy.ProxyProperties;
import tp.pdc.proxy.exceptions.ParserFormatException;
import tp.pdc.proxy.header.BytesUtils;
import tp.pdc.proxy.header.Header;
import tp.pdc.proxy.parser.component.HttpHeadersParserImpl;
import tp.pdc.proxy.parser.interfaces.HttpHeaderParser;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.*;

import static org.junit.Assert.*;

public class HttpHeadersParserTest {

    private String headers1, headers2;
    private ByteBuffer inputBuffer1,inputBuffer2, outputBuffer;
    private HttpHeaderParser parser;

    private static final Charset charset = ProxyProperties.getInstance().getCharset();

    @Before
    public void init(){
        headers1 = "host: google.com\r\n"
                + "user-agent: internet explorer 2\r\n"
                + "connection: close\r\n"
                + "transfer-encoding: chunked\r\n"
                + "connection-no: close\r\n"
                + "transfer-size: chunked\r\n\r\n";

        headers2 = "host:google.com\r\n";
        outputBuffer = ByteBuffer.allocate(2000);
        inputBuffer1 = ByteBuffer.wrap(headers1.getBytes(charset));
        inputBuffer2 = ByteBuffer.wrap(headers2.getBytes(charset));

        Set<Header> relevant = EnumSet.of(Header.HOST, Header.TRANSFER_ENCODING, Header.CONTENT_LENGTH, Header.CONNECTION);

        Set<Header> toRemove = Collections.emptySet();
        Map<Header, byte[]> toAdd = Collections.emptyMap();

        parser = new HttpHeadersParserImpl(toAdd, toRemove, relevant);
    }

    @Test
    public void parseTest() throws ParserFormatException {
        assertTrue(parser.parse(inputBuffer1,outputBuffer));
    }

    @Test
    public void HasRelevantHeadersTest() throws ParserFormatException {
        parser.parse(inputBuffer1,outputBuffer);

        assertTrue(parser.hasHeaderValue(Header.HOST));
        assertTrue(parser.hasHeaderValue(Header.CONNECTION));
        assertTrue(parser.hasHeaderValue(Header.TRANSFER_ENCODING));
    }

    @Test
    public void CheckRelevantHeadersValueTest() throws ParserFormatException {
        parser.parse(inputBuffer1,outputBuffer);

        assertArrayEquals("google.com".getBytes(charset), parser.getHeaderValue(Header.HOST));
        assertArrayEquals("close".getBytes(charset), parser.getHeaderValue(Header.CONNECTION));
        assertArrayEquals("chunked".getBytes(charset), parser.getHeaderValue(Header.TRANSFER_ENCODING));

    }

    @Test
    public void RemoveHeaderTest() throws ParserFormatException {
        String expectedOutput = "host: google.com\r\n"
            + "transfer-encoding: chunked\r\n"
            + "connection-no: close\r\n"
            + "transfer-size: chunked\r\n\r\n";

        Set<Header> relevant = Collections.emptySet();
        Set<Header> toRemove = new HashSet<>();
        toRemove.add(Header.CONNECTION);
        toRemove.add(Header.USER_AGENT);
        Map<Header, byte[]> toAdd = Collections.emptyMap();

        HttpHeaderParser removeParser = new HttpHeadersParserImpl(toAdd, toRemove, relevant);

        removeParser.parse(inputBuffer1, outputBuffer);

        outputBuffer.flip();
        int len = outputBuffer.remaining();
        assertTrue(BytesUtils.equalsBytes(expectedOutput.getBytes(charset), outputBuffer.array(), len));
    }

    @Test
    public void ChangeHeaderTest() throws ParserFormatException {
        String expectedOutput = "connection: keep-alive\r\n"
            + "host: google.com\r\n"
            + "user-agent: internet explorer 2\r\n"
            + "transfer-encoding: chunked\r\n"
            + "connection-no: close\r\n"
            + "transfer-size: chunked\r\n\r\n";

        Map<Header, byte[]> toAdd = new HashMap<>();
        toAdd.put(Header.CONNECTION, "keep-alive".getBytes(charset));

        Set<Header> relevant = Collections.emptySet();
        Set<Header> remove = Collections.emptySet();

        HttpHeaderParser addParser = new HttpHeadersParserImpl(toAdd, remove, relevant);

        addParser.parse(inputBuffer1, outputBuffer);

        outputBuffer.flip();
        int len = outputBuffer.remaining();
        assertTrue(BytesUtils.equalsBytes(expectedOutput.getBytes(charset), outputBuffer.array(), len));
    }

    @Test
    public void OutputTest() throws ParserFormatException {
        parser.parse(inputBuffer1, outputBuffer);

        inputBuffer1.flip();
        int len = inputBuffer1.remaining();
        outputBuffer.flip();

        assertTrue(BytesUtils.equalsBytes(inputBuffer1.array(), outputBuffer.array(), len));
    }

    @Test
    public void NotRelevantHeadersTest() throws ParserFormatException {
        parser.parse(inputBuffer1, outputBuffer);
        assertFalse(parser.hasHeaderValue(Header.CONTENT_LENGTH));
    }

    @Test(expected = ParserFormatException.class)
    public void NotHeaderSpaceTest() throws ParserFormatException {
        parser.parse(inputBuffer2, outputBuffer);
    }

    @Test(expected = ParserFormatException.class)
    public void NoColonTest() throws ParserFormatException {
        String request = "Transfer-encoding chunked";
        ByteBuffer wrongInput = ByteBuffer.wrap(request.getBytes(charset));

        parser.parse(wrongInput, outputBuffer);
    }
}
