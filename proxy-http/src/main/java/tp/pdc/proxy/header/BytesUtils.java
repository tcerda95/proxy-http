package tp.pdc.proxy.header;

import java.nio.ByteBuffer;

public class BytesUtils {

     public static boolean equalsBytes(byte[] array, ByteBuffer byteBuffer, int length) {
    	byte[] bufferArray = byteBuffer.array();
    	int offset = byteBuffer.position();
    	
    	if (length == array.length && equalsBytes(array, 0, bufferArray, offset, length))
    		return true;
    	
    	return false;
    }
    
    public static boolean equalsBytes(byte[] arr1, byte[] arr2, int length) {
    	return equalsBytes(arr1, 0, arr2, 0, length);
    }
    
    public static boolean equalsBytes(byte[] arr1, int offset1, byte[] arr2, int offset2, int length) {
    	if (arr1.length - offset1 < length)
    		return false;
    	
    	if (arr2.length - offset2 < length)
    		return false;
    	
    	for (int i = offset1, j = offset2; length > 0; i++, j++, length--)
    		if (arr1[i] != arr2[j])
    			return false;
    	
    	return true;
    }
}
