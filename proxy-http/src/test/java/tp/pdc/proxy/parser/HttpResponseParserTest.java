package tp.pdc.proxy.parser;

import org.junit.Before;
import org.junit.Test;
import tp.pdc.proxy.ProxyProperties;
import tp.pdc.proxy.exceptions.ParserFormatException;
import tp.pdc.proxy.parser.interfaces.HttpResponseLineParser;
import tp.pdc.proxy.parser.interfaces.HttpResponseParser;

import java.nio.ByteBuffer;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class HttpResponseParserTest {

    String message, responseLine, emptyHeaders, contentHeaders;
    int statusCode;
    HttpResponseParser parser;
    ByteBuffer output, emptyHeadersInput, fullHeadersInput;

    @Before
    public void init(){
        statusCode = new Random().nextInt(999);
        message = "Hi I'm a responseLine message";
        responseLine = "HTTP/1.1 " + statusCode + " " + message + "\r\n";
        emptyHeaders = "\r\n";
        contentHeaders = "Host: google.com\r\n"
            + "user-agent: internet explorer 2\r\n"
            + "connection: close\r\n"
            + "transfer-encoding: chunked\r\n"
            + "connection-no: close\r\n"
            + "transfer-size: chunked\r\n\r\n";;

        parser = new HttpResponseParserImpl();
        output = ByteBuffer.allocate(1024);
        emptyHeadersInput = ByteBuffer.wrap((responseLine + emptyHeaders).getBytes());
        fullHeadersInput = ByteBuffer.wrap((responseLine + contentHeaders).getBytes());
    }

    @Test
    public void statusCodeTest() throws ParserFormatException {
        parser.parse(fullHeadersInput, output);
        assertEquals(statusCode, parser.getStatusCode());
    }

    @Test
    public void hasFinishedTest() throws ParserFormatException {
        parser.parse(fullHeadersInput, output);
        assertTrue(parser.hasFinished());
    }

    @Test
    public void remainingBufferTest() throws ParserFormatException {
        String remaining = "I'm remaining";
        ByteBuffer inputWithRemaining = ByteBuffer.wrap((responseLine + contentHeaders + remaining).getBytes());

        parser.parse(inputWithRemaining, output);
        assertEquals(remaining,
            new String(inputWithRemaining.array(), inputWithRemaining.position(),
                inputWithRemaining.remaining(), ProxyProperties.getInstance().getCharset()));
    }

}
