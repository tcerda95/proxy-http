package tp.pdc.proxy;

import tp.pdc.proxy.exceptions.ParserFormatException;

import java.nio.ByteBuffer;

public interface HttpVersionParser {

    boolean hasError();

    boolean hasEndedOk ();

    boolean readMinorVersion();

    boolean readMajorVersion();

    int getMajorHttpVersion();

    int getMinorHttpVersion();

    boolean parse(ByteBuffer inputBuffer, ByteBuffer outputBuffer) throws ParserFormatException;

    boolean parse(byte b, ByteBuffer outputBuffer) throws ParserFormatException;
}
