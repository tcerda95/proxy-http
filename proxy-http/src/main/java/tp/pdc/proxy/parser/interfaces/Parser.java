package tp.pdc.proxy.parser.interfaces;

import java.nio.ByteBuffer;

import tp.pdc.proxy.exceptions.ParserFormatException;

public interface Parser {
    boolean parse(ByteBuffer input, ByteBuffer output) throws ParserFormatException;
    boolean hasFinished();
    void reset();
}
