package tp.pdc.proxy.header;

import java.nio.ByteBuffer;
import tp.pdc.proxy.ProxyProperties;


public enum Method {
    GET("GET"), POST("POST"), HEAD("HEAD");

    private String methodName;
    private byte[] methodBytes;

    public static boolean isRelevantHeader(ByteBuffer bytes, int length) {
        return getByBytes(bytes, length) != null;
    }

    public static Method getByBytes(ByteBuffer bytes, int length) {
    	for (Method method : values())
    		if (BytesUtils.equalsBytes(method.methodBytes, bytes, length))
    			return method;
    	return null;
    }
    
    private Method(String method) {
        methodName = method;
        methodBytes = method.getBytes(ProxyProperties.getInstance().getCharset());
    }

    @Override
    public String toString() {
    	return methodName;
    }
}
