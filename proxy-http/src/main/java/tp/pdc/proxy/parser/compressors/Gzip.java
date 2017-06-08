package tp.pdc.proxy.parser.compressors;

import tp.pdc.proxy.parser.interfaces.Compressor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class Gzip implements Compressor {

	public byte[] compress (ByteBuffer input) throws IOException {

		byte[] remainingBytes = new byte[input.remaining()];
		input.get(remainingBytes, 0, remainingBytes.length);

		ByteArrayOutputStream output = new ByteArrayOutputStream();
		GZIPOutputStream gzip = new GZIPOutputStream(output);

		gzip.write(remainingBytes);

		gzip.close();
		output.close();

		return output.toByteArray();
	}


	public byte[] decompress (ByteBuffer input) throws IOException {

		byte[] remainingBytes = new byte[input.remaining()];
		input.get(remainingBytes, 0, remainingBytes.length);

		ByteArrayInputStream inputStream = new ByteArrayInputStream(remainingBytes);
		GZIPInputStream gzipper = new GZIPInputStream(inputStream);

		byte[] buffer = new byte[1024];
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		out.close();

		int len;
		while ((len = gzipper.read(buffer)) > 0)
			out.write(buffer, 0, len);

		gzipper.close();
		out.close();

		return out.toByteArray();
	}

}
