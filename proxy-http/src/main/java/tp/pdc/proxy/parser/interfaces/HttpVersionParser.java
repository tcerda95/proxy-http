package tp.pdc.proxy.parser.interfaces;

import tp.pdc.proxy.exceptions.ParserFormatException;

import java.nio.ByteBuffer;

public interface HttpVersionParser extends Parser, Reseteable {

    boolean readMinorVersion();

    boolean readMajorVersion();

    int getMajorHttpVersion();

    int getMinorHttpVersion();

    boolean parse(byte c, ByteBuffer outputBuffer) throws ParserFormatException;

}
