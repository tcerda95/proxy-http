package tp.pdc.proxy.parser.interfaces;


import tp.pdc.proxy.exceptions.ParserFormatException;
import tp.pdc.proxy.header.Header;

import java.nio.ByteBuffer;
import java.util.Map;

public interface HttpHeaderParser {

    boolean hasError();

    boolean hasFinished();

    boolean hasRelevantHeaderValue(Header header);

    byte[] getRelevantHeaderValue(Header header);

    Map<Header, byte[]> getRelevantHeaders();

    boolean parse(ByteBuffer inputBuffer, ByteBuffer outputBuffer) throws ParserFormatException;

    boolean parse(byte c, ByteBuffer outputBuffer) throws ParserFormatException;

}
