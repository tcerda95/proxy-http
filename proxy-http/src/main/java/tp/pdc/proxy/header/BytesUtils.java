package tp.pdc.proxy.header;

import java.nio.ByteBuffer;

public class BytesUtils {

	/**
	 * Copies length bytes from input buffer to output buffer using the bulk put operation 
	 * and updates input's position pointer accordingly.
	 * @param input Buffer from which bytes must be read. Must be in read mode.
	 * @param output Buffer from which bytes will be written to.
	 * @param length Amount of bytes to copy.
	 */
	public static void lengthPut(ByteBuffer input, ByteBuffer output, int length) {
		int inputPos = input.position();
		output.put(input.array(), inputPos, length);
		input.position(inputPos + length);
	}
	
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
