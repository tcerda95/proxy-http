package tp.pdc.proxy.parser;

import org.junit.Before;
import org.junit.Test;
import tp.pdc.proxy.exceptions.ParserFormatException;
import tp.pdc.proxy.header.Header;
import tp.pdc.proxy.parser.componentParsers.HttpHeadersParserImpl;
import tp.pdc.proxy.parser.interfaces.HttpHeaderParser;

import java.nio.ByteBuffer;

import static org.junit.Assert.*;

public class HttpHeadersParserTest {

    String headers1, headers2;
    ByteBuffer inputBuffer1,inputBuffer2, outputBuffer;
    HttpHeaderParser parser;

    @Before
    public void init(){
        headers1 = "Host: google.com\r\n"
                + "user-agent: internet explorer 2\r\n"
                + "connection: close\r\n"
                + "transfer-encoding: chunked\r\n"
                + "connection-no: close\r\n"
                + "transfer-size: chunked\r\n\r\n";

        headers2 = "Host:google.com\r\n";
        parser = new HttpHeadersParserImpl();
        outputBuffer = ByteBuffer.allocate(2000);
        inputBuffer1 = ByteBuffer.wrap(headers1.getBytes());
        inputBuffer2 = ByteBuffer.wrap(headers2.getBytes());
    }

    @Test
    public void parseTest() throws ParserFormatException {
        assertTrue(parser.parse(inputBuffer1,outputBuffer));
    }

    @Test
    public void HasRelevantHeadersTest() throws ParserFormatException {
        parser.parse(inputBuffer1,outputBuffer);

        assertTrue(parser.hasRelevantHeaderValue(Header.HOST));
        assertTrue(parser.hasRelevantHeaderValue(Header.CONNECTION));
        assertTrue(parser.hasRelevantHeaderValue(Header.TRANSFER_ENCODING));
    }

    @Test
    public void CheckRelevantHeadersValueTest() throws ParserFormatException {
        parser.parse(inputBuffer1,outputBuffer);

        assertArrayEquals("google.com".getBytes(), parser.getRelevantHeaderValue(Header.HOST));
        assertArrayEquals("close".getBytes(), parser.getRelevantHeaderValue(Header.CONNECTION));
        assertArrayEquals("chunked".getBytes(), parser.getRelevantHeaderValue(Header.TRANSFER_ENCODING));

    }

    @Test
    public void NotRelevantHeadersTest() throws ParserFormatException {
        parser.parse(inputBuffer1,outputBuffer);
        assertFalse(parser.hasRelevantHeaderValue(Header.CONTENT_LENGTH));
    }

    @Test(expected = ParserFormatException.class)
    public void NotHeaderSpaceTest() throws ParserFormatException {
        parser.parse(inputBuffer2,outputBuffer);
    }

    @Test(expected = ParserFormatException.class)
    public void NoColonTest() throws ParserFormatException {
        String request = "Transfer-encoding chunked";
        ByteBuffer wrongInput = ByteBuffer.wrap(request.getBytes());

        parser.parse(wrongInput, outputBuffer);
    }

}
