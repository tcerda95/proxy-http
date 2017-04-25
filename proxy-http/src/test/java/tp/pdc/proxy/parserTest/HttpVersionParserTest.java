package tp.pdc.proxy.parserTest;

import static org.junit.Assert.*;
import org.junit.Test;
import tp.pdc.proxy.exceptions.ParserFormatException;
import tp.pdc.proxy.parser.HttpVersionParserImpl;
import tp.pdc.proxy.parser.interfaces.HttpVersionParser;
import tp.pdc.proxy.parser.utils.AsciiConstants;

import java.nio.ByteBuffer;

public class HttpVersionParserTest implements AsciiConstants {

    @Test
    public void endCharacterTest() throws ParserFormatException {
        HttpVersionParser parser1 = new HttpVersionParserImpl((byte) CR);
        HttpVersionParser parser2 = new HttpVersionParserImpl((byte) CR);

        String s1 = "HTTP/1.1" + CR;
        String s2 = "HTTP/1.1";

        ByteBuffer b1 = ByteBuffer.wrap(s1.getBytes());
        ByteBuffer b2 = ByteBuffer.wrap(s2.getBytes());

        ByteBuffer o1 = ByteBuffer.allocate(64);
        ByteBuffer o2 = ByteBuffer.allocate(64);

        parser1.parse(b1, o1);
        parser2.parse(b2, o2);
        assertTrue(parser1.hasEndedOk());
        assertFalse(parser2.hasEndedOk());
    }

    @Test(expected = ParserFormatException.class)
    public void wrongEndChar() throws ParserFormatException {
        HttpVersionParser parser = new HttpVersionParserImpl((byte) CR);
        String s = "HTTP/1.1" + LF;

        ByteBuffer b = ByteBuffer.wrap(s.getBytes());
        ByteBuffer o = ByteBuffer.allocate(64);

        parser.parse(b, o);
    }

//    public void majorVersionTest() throws ParserFormatException {
//        HttpVersionParser parser1 = new HttpVersionParserImpl((byte) CR);
//        HttpVersionParser parser2 = new HttpVersionParserImpl((byte) CR);
//        HttpVersionParser parser3 = new HttpVersionParserImpl((byte) CR);
//
//        String s1 = "HTTP/1.1" + CR;
//        String s2 = "HTTP/009.1" + CR;
//        String s3 = "HTTP/0170.1" + CR;
//
//        ByteBuffer b1 = ByteBuffer.wrap(s1.getBytes());
//        ByteBuffer b2 = ByteBuffer.wrap(s2.getBytes());
//        ByteBuffer b3 = ByteBuffer.wrap(s3.getBytes());
//
//        ByteBuffer o = ByteBuffer.allocate(64);
//
//        parser1.parse(b1, o);
//        assertEquals(parser1.getMajorHttpVersion(), 1);
//
//        o.clear();
//
//        parser2.parse(b2, o);
//        assertEquals(parser2.getMajorHttpVersion(), 9);
//
//        o.clear();
//    }
}
