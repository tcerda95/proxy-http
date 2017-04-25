package tp.pdc.proxy;

import java.nio.ByteBuffer;

import tp.pdc.proxy.exceptions.ParserFormatException;

public interface Parser {
    void parse(ByteBuffer input, ByteBuffer output) throws ParserFormatException;
    boolean hasFinished();
}
