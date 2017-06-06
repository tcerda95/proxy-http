package tp.pdc.proxy.header;

import java.nio.ByteBuffer;
import tp.pdc.proxy.ProxyProperties;


public enum Method {
    GET("GET"), POST("POST"), HEAD("HEAD"), OPTIONS("OPTIONS"), PUT("PUT"),
        DELETE("DELETE"), TRACE("TRACE"), CONNECT("CONNECT");

    private String methodName;
    private byte[] methodBytes;

    public static Method getByBytes(ByteBuffer bytes, int length) {
    	for (Method method : values())
    		if (BytesUtils.equalsBytes(method.methodBytes, bytes, length))
    			return method;
    	return null;
    }
    
    Method(String method) {
        methodName = method;
        methodBytes = method.getBytes(ProxyProperties.getInstance().getCharset());
    }

    @Override
    public String toString() {
    	return methodName;
    }
    
    public byte[] getBytes() {
    	return methodBytes;
    }
    
	public static int maxMethodLen() {	
		int maxLength = 0;
		for (Method m : Method.values()) {
			int currentLength = m.toString().length();
			
			if (currentLength > maxLength)
					maxLength = currentLength;
		}
		return maxLength;
	}
}
