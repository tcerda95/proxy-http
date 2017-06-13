package tp.pdc.proxy.parser.interfaces;

import tp.pdc.proxy.exceptions.ParserFormatException;

import java.nio.ByteBuffer;

public interface Parser {

	boolean parse (ByteBuffer input, ByteBuffer output) throws ParserFormatException;

	/**
	 * Checks if the parser ahs finish its activity
	 * @return true if it has finished, false if not
     */
	boolean hasFinished ();
}
