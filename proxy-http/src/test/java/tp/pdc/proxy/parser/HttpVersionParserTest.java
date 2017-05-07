package tp.pdc.proxy.parser;

import org.junit.Before;
import org.junit.Test;
import tp.pdc.proxy.ProxyProperties;
import tp.pdc.proxy.exceptions.ParserFormatException;
import tp.pdc.proxy.parser.component.HttpVersionParserImpl;
import tp.pdc.proxy.parser.interfaces.HttpVersionParser;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import static org.junit.Assert.*;
import static tp.pdc.proxy.parser.utils.AsciiConstants.CR;

public class HttpVersionParserTest {

    private static final Charset charset = ProxyProperties.getInstance().getCharset();
    private ByteBuffer output;
    private HttpVersionParser parser;

    @Before
    public void init() {
        parser = new HttpVersionParserImpl(CR.getValue());
        output = ByteBuffer.allocate(64);
    }

    @Test
    public void endCharacterTrueTest() throws ParserFormatException {
        String s1 = "HTTP/1.1\r";

        ByteBuffer b1 = ByteBuffer.wrap(s1.getBytes(charset));

        parser.parse(b1, output);
        assertTrue(parser.hasFinished());
    }

    @Test(expected = ParserFormatException.class)
    public void wrongEndCharTest() throws ParserFormatException {
        String s = "HTTP/1.1\n";
        ByteBuffer b = ByteBuffer.wrap(s.getBytes(charset));
        parser.parse(b, output);
    }


    @Test(expected = ParserFormatException.class)
    public void wrongCharWhereHttpTest() throws ParserFormatException {
        String s = "HTPP/1.1\r";
        ByteBuffer b = ByteBuffer.wrap(s.getBytes(charset));
        parser.parse(b, output);
    }

    @Test(expected = ParserFormatException.class)
    public void wrongEscapeCharTest() throws ParserFormatException {
        String s = "HTTP?1.1\r";
        ByteBuffer b = ByteBuffer.wrap(s.getBytes(charset));
        parser.parse(b, output);
    }

    @Test(expected = ParserFormatException.class)
    public void charInMajorVersionTest() throws ParserFormatException {
        String s = "HTTP/a.9\r";
        ByteBuffer b = ByteBuffer.wrap(s.getBytes(charset));
        parser.parse(b, output);
    }

    @Test(expected = ParserFormatException.class)
    public void charInMinorVersionTest() throws ParserFormatException {
        String s = "HTTP/7.a\r";
        ByteBuffer b = ByteBuffer.wrap(s.getBytes(charset));

        parser.parse(b, output);
    }

    @Test(expected = ParserFormatException.class)
    public void charAtEndTest() throws ParserFormatException {
        String s = "HTTP/7.1a\r";
        ByteBuffer b = ByteBuffer.wrap(s.getBytes(charset));
        parser.parse(b, output);
    }

    @Test
    public void majorVersionTest() throws ParserFormatException {
        String s1 = "HTTP/1.2\r";
        String s2 = "HTTP/009.001\r";
        String s3 = "HTTP/01701.0710\r";

        ByteBuffer b1 = ByteBuffer.wrap(s1.getBytes(charset));
        ByteBuffer b2 = ByteBuffer.wrap(s2.getBytes(charset));
        ByteBuffer b3 = ByteBuffer.wrap(s3.getBytes(charset));

        parser.parse(b1, output);
        assertEquals(1, parser.getMajorHttpVersion());
        output.clear();

        parser.reset();
        parser.parse(b2, output);
        assertEquals(9, parser.getMajorHttpVersion());
        output.clear();

        parser.reset();
        parser.parse(b3, output);
        assertEquals(1701, parser.getMajorHttpVersion());
        output.clear();
    }

    @Test
    public void minorVersionTest() throws ParserFormatException {
        String s1 = "HTTP/1.2\r";
        String s2 = "HTTP/009.007\r";
        String s3 = "HTTP/0170.0740\r";

        ByteBuffer b1 = ByteBuffer.wrap(s1.getBytes(charset));
        ByteBuffer b2 = ByteBuffer.wrap(s2.getBytes(charset));
        ByteBuffer b3 = ByteBuffer.wrap(s3.getBytes(charset));

        parser.parse(b1, output);
        assertEquals(2, parser.getMinorHttpVersion());
        output.clear();

        parser.reset();
        parser.parse(b2, output);
        assertEquals(7, parser.getMinorHttpVersion());
        output.clear();

        parser.reset();
        parser.parse(b3, output);
        assertEquals(740, parser.getMinorHttpVersion());
        output.clear();
    }

    @Test
    public void leaveUnreadDataTest() throws ParserFormatException {
        String version = "HTTP/1.2\r";
        String remaining = "HelloWorld";
        String sum = version + remaining;

        ByteBuffer input = ByteBuffer.wrap(sum.getBytes(charset));

        parser.parse(input, output);

        assertEquals(remaining,
            // Bytes left in inputBuffer
            new String(input.array(), input.position(), input.remaining(), charset));
    }
}
