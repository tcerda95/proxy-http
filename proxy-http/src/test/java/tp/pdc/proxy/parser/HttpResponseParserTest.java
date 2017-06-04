package tp.pdc.proxy.parser;

import org.junit.Before;
import org.junit.Test;
import tp.pdc.proxy.ProxyProperties;
import tp.pdc.proxy.exceptions.ParserFormatException;
import tp.pdc.proxy.header.Method;
import tp.pdc.proxy.parser.factory.HttpResponseParserFactory;
import tp.pdc.proxy.parser.interfaces.HttpResponseParser;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class HttpResponseParserTest {

    private String message, responseLine, emptyHeaders, contentHeaders;
    private int statusCode;
    private HttpResponseParser parser;
    private ByteBuffer output, emptyHeadersInput, fullHeadersInput;

    private static final Charset charset = ProxyProperties.getInstance().getCharset();

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
            + "transfer-size: chunked\r\n\r\n";

        parser = HttpResponseParserFactory.getInstance().getResponseParser(Method.HEAD);
        output = ByteBuffer.allocate(1024);
        emptyHeadersInput = ByteBuffer.wrap((responseLine + emptyHeaders).getBytes(charset));
        fullHeadersInput = ByteBuffer.wrap((responseLine + contentHeaders).getBytes(charset));
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
        ByteBuffer inputWithRemaining = ByteBuffer.wrap((responseLine + contentHeaders + remaining).getBytes(charset));

        parser.parse(inputWithRemaining, output);
        assertEquals(remaining,
            new String(inputWithRemaining.array(), inputWithRemaining.position(),
                inputWithRemaining.remaining(), charset));
    }

    //TODO: hacer mas. Falta probar mandar sin headers, y mandar cosas en pedazos.

}
