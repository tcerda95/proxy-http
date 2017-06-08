package tp.pdc.proxy.parser.interfaces;

import java.io.IOException;
import java.nio.ByteBuffer;

public interface Compressor {
	public byte[] compress (ByteBuffer input) throws IOException;

	public byte[] decompress (ByteBuffer input) throws IOException;
}
