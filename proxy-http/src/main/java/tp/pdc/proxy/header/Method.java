package tp.pdc.proxy.header;

import java.nio.ByteBuffer;
import tp.pdc.proxy.ProxyProperties;


public enum Method {
    GET("GET"), POST("POST"), HEAD("HEAD"), OPTIONS("OPTIONS"), PUT("PUT"),
        DELETE("DELETE"), TRACE("TRACE"), CONNECT("CONNECT");

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

    public static void main (String[] args) {
        ByteBuffer b = ByteBuffer.allocate(10);

        b.put((byte) 'a');
        b.put((byte) 'b');
        b.put((byte) 'c');
        b.put((byte) 'd');

        b.flip();
        System.out.println(b.get());
        System.out.println(b);

        b.position(b.position() - 1);
        System.out.println(b.get());
        System.out.println(b);
    }
}
