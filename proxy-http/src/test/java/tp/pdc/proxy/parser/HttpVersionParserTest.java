package tp.pdc.proxy.parser;

import org.junit.Test;
import tp.pdc.proxy.ProxyProperties;
import tp.pdc.proxy.exceptions.ParserFormatException;
import tp.pdc.proxy.parser.componentParsers.HttpVersionParserImpl;
import tp.pdc.proxy.parser.interfaces.HttpVersionParser;

import java.nio.ByteBuffer;

import static org.junit.Assert.*;
import static tp.pdc.proxy.parser.utils.AsciiConstants.CR;

public class HttpVersionParserTest {

    @Test
    public void endCharacterTrueTest() throws ParserFormatException {
        HttpVersionParser parser1 = new HttpVersionParserImpl(CR.getValue());

        String s1 = "HTTP/1.1\r";

        ByteBuffer b1 = ByteBuffer.wrap(s1.getBytes());

        ByteBuffer o1 = ByteBuffer.allocate(64);

        parser1.parse(b1, o1);
        assertTrue(parser1.hasFinished());
    }

    @Test(expected = ParserFormatException.class)
    public void wrongEndCharTest() throws ParserFormatException {
        HttpVersionParser parser = new HttpVersionParserImpl(CR.getValue());
        String s = "HTTP/1.1\n";

        ByteBuffer b = ByteBuffer.wrap(s.getBytes());
        ByteBuffer o = ByteBuffer.allocate(64);

        parser.parse(b, o);
    }


    @Test(expected = ParserFormatException.class)
    public void wrongCharWhereHttpTest() throws ParserFormatException {
        HttpVersionParser parser = new HttpVersionParserImpl(CR.getValue());
        String s = "HTPP/1.1\r";

        ByteBuffer b = ByteBuffer.wrap(s.getBytes());
        ByteBuffer o = ByteBuffer.allocate(64);

        parser.parse(b, o);
    }

    @Test(expected = ParserFormatException.class)
    public void wrongEscapeCharTest() throws ParserFormatException {
        HttpVersionParser parser = new HttpVersionParserImpl(CR.getValue());
        String s = "HTTP?1.1\r";

        ByteBuffer b = ByteBuffer.wrap(s.getBytes());
        ByteBuffer o = ByteBuffer.allocate(64);

        parser.parse(b, o);
    }



    @Test(expected = ParserFormatException.class)
    public void charInMajorVersionTest() throws ParserFormatException {
        HttpVersionParser parser = new HttpVersionParserImpl(CR.getValue());
        String s = "HTTP/a.9\r";

        ByteBuffer b = ByteBuffer.wrap(s.getBytes());
        ByteBuffer o = ByteBuffer.allocate(64);

        parser.parse(b, o);
    }

    @Test(expected = ParserFormatException.class)
    public void charInMinorVersionTest() throws ParserFormatException {
        HttpVersionParser parser = new HttpVersionParserImpl(CR.getValue());
        String s = "HTTP/7.a\r";

        ByteBuffer b = ByteBuffer.wrap(s.getBytes());
        ByteBuffer o = ByteBuffer.allocate(64);

        parser.parse(b, o);
    }

    @Test(expected = ParserFormatException.class)
    public void charAtEndTest() throws ParserFormatException {
        HttpVersionParser parser = new HttpVersionParserImpl(CR.getValue());
        String s = "HTTP/7.1a\r";

        ByteBuffer b = ByteBuffer.wrap(s.getBytes());
        ByteBuffer o = ByteBuffer.allocate(64);

        parser.parse(b, o);
    }

    @Test
    public void majorVersionTest() throws ParserFormatException {
        HttpVersionParser parser = new HttpVersionParserImpl(CR.getValue());

        String s1 = "HTTP/1.2\r";
        String s2 = "HTTP/009.001\r";
        String s3 = "HTTP/01701.0710\r";

        ByteBuffer b1 = ByteBuffer.wrap(s1.getBytes());
        ByteBuffer b2 = ByteBuffer.wrap(s2.getBytes());
        ByteBuffer b3 = ByteBuffer.wrap(s3.getBytes());

        ByteBuffer o = ByteBuffer.allocate(64);

        parser.parse(b1, o);
        assertEquals(1, parser.getMajorHttpVersion());
        o.clear();

        parser.reset();
        parser.parse(b2, o);
        assertEquals(9, parser.getMajorHttpVersion());
        o.clear();

        parser.reset();
        parser.parse(b3, o);
        assertEquals(1701, parser.getMajorHttpVersion());
        o.clear();
    }

    @Test
    public void minorVersionTest() throws ParserFormatException {
        HttpVersionParser parser = new HttpVersionParserImpl(CR.getValue());

        String s1 = "HTTP/1.2\r";
        String s2 = "HTTP/009.007\r";
        String s3 = "HTTP/0170.0740\r";

        ByteBuffer b1 = ByteBuffer.wrap(s1.getBytes());
        ByteBuffer b2 = ByteBuffer.wrap(s2.getBytes());
        ByteBuffer b3 = ByteBuffer.wrap(s3.getBytes());

        ByteBuffer o = ByteBuffer.allocate(64);

        parser.parse(b1, o);
        assertEquals(2, parser.getMinorHttpVersion());
        o.clear();

        parser.reset();
        parser.parse(b2, o);
        assertEquals(7, parser.getMinorHttpVersion());
        o.clear();

        parser.reset();
        parser.parse(b3, o);
        assertEquals(740, parser.getMinorHttpVersion());
        o.clear();
    }

    @Test
    public void leaveUnreadDataTest() throws ParserFormatException {
        HttpVersionParser parser = new HttpVersionParserImpl(CR.getValue());

        String version = "HTTP/1.2\r";
        String remaining = "HelloWorld";
        String sum = version + remaining;

        ByteBuffer input = ByteBuffer.wrap(sum.getBytes());
        ByteBuffer output = ByteBuffer.allocate(64);

        parser.parse(input, output);

        assertEquals(remaining,
            // Bytes left in inputBuffer
            new String(input.array(), input.position(), input.remaining(), ProxyProperties.getInstance().getCharset()));
    }
}
