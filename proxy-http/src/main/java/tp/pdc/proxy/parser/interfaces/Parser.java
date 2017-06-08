package tp.pdc.proxy.parser.interfaces;

import tp.pdc.proxy.exceptions.ParserFormatException;

import java.nio.ByteBuffer;

public interface Parser {

	boolean parse (ByteBuffer input, ByteBuffer output) throws ParserFormatException;

	boolean hasFinished ();
}
