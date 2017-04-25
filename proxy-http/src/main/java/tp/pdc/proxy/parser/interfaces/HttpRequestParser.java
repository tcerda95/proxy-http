package tp.pdc.proxy.parser.interfaces;

import com.sun.javafx.tools.packager.PackagerException;
import tp.pdc.proxy.exceptions.ParserFormatException;
import tp.pdc.proxy.header.Header;

import java.nio.ByteBuffer;


public interface HttpRequestParser {

    boolean hasHeaderValue(Header header);

    byte[] getHeaderValue(Header header);

    void parse(ByteBuffer input, ByteBuffer output) throws ParserFormatException;

    boolean hasFinished();
}
