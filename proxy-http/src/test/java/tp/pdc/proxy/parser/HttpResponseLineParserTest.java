package tp.pdc.proxy.parser;


import org.junit.Before;
import org.junit.Test;
import tp.pdc.proxy.ProxyProperties;
import tp.pdc.proxy.exceptions.ParserFormatException;
import tp.pdc.proxy.parser.component.HttpResponseLineParserImpl;
import tp.pdc.proxy.parser.interfaces.HttpResponseLineParser;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Random;
import static org.junit.Assert.*;

public class HttpResponseLineParserTest {

    private String message;
    private String response;
    private int statusCode;
    private HttpResponseLineParser parser;
    private ByteBuffer output, input;

    private static final Charset charset = ProxyProperties.getInstance().getCharset();

    @Before
    public void init(){
        statusCode = new Random().nextInt(999);
        message = "Hi I'm a responseLine message";
        response = "HTTP/1.1 " + statusCode + " " + message + "\r\n";
        parser = new HttpResponseLineParserImpl();
        input = ByteBuffer.wrap(response.getBytes(charset));
        output = ByteBuffer.allocate(1024);
    }


    @Test
    public void statusCodeTest() throws ParserFormatException {
        parser.parse(input, output);
        assertEquals(statusCode, parser.getStatusCode());
    }

    @Test
    public void hasFinishedTest() throws ParserFormatException {
        parser.parse(input, output);
        assertTrue(parser.hasFinished());
    }

    @Test(expected = ParserFormatException.class)
    public void wrongStatusCodeTest() throws ParserFormatException {
        String wrongResponse = "HTTP/1.1 " + "3a1" + " " + message + "\r\n";
        ByteBuffer wrongInput = ByteBuffer.wrap(wrongResponse.getBytes(charset));

        parser.parse(wrongInput, output);
    }

    @Test(expected = ParserFormatException.class)
    public void wrongEndMessageTest() throws ParserFormatException {
        String wrongResponse = "HTTP/1.1 " + "300" + " " + message + "\n\r";
        ByteBuffer wrongInput = ByteBuffer.wrap(wrongResponse.getBytes(charset));

        parser.parse(wrongInput, output);
    }

    @Test
    public void remainingBufferTest() throws ParserFormatException {
        String remaining = "I'm remaining";
        ByteBuffer inputWithRemaining = ByteBuffer.wrap((response + remaining).getBytes(charset));

        parser.parse(inputWithRemaining, output);
        assertEquals(remaining,
            new String(inputWithRemaining.array(), inputWithRemaining.position(),
                inputWithRemaining.remaining(), charset));
    }
}
