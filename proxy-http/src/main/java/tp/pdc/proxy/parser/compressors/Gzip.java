package tp.pdc.proxy.parser.compressors;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class Gzip {

	public static byte[] compress(ByteBuffer input) throws IOException {
    	
		byte[] remainingBytes = new byte[input.remaining()];
		input.get(remainingBytes, 0, remainingBytes.length);
		
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		GZIPOutputStream gzip = new GZIPOutputStream(output);
		
		gzip.write(remainingBytes);
		
		gzip.close();
		output.close();
		
		return output.toByteArray();
    }

      
    public static byte[] decompress(ByteBuffer input) throws IOException {
    	
    	byte[] remainingBytes = new byte[input.remaining()];
		input.get(remainingBytes, 0, remainingBytes.length);
	  	  
		ByteArrayInputStream bin = new ByteArrayInputStream(remainingBytes);
	    GZIPInputStream gzipper = new GZIPInputStream(bin);
		  
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
      
//	public static byte[] decompress(ByteBuffer input) throws Exception {
//    	 
//		byte[] output = new byte[1024];
//    	
//		byte[] remainingBytes = new byte[input.remaining()];
//		input.get(remainingBytes, 0, remainingBytes.length);
//		
//		GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(remainingBytes));
//		
//		gzip.read(output, 0, 1024);
//		gzip.close();
//
//		return output;
//     }

}
