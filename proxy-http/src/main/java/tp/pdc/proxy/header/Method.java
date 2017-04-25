package tp.pdc.proxy.header;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;


public enum Method {
    GET("GET"), POST("POST"), HEAD("HEAD");

    private static final Map<byte[], Method> SUPPORTED_METHODS = new HashMap<>();

    static {
        for (Method method : values())
            SUPPORTED_METHODS.put(method.methodName.getBytes(), method);
    }

    private String methodName;


    public static boolean isRelevantHeader(ByteBuffer bytes, int length) {
        return getByBytes(bytes, length) != null;
    }

    Method(String method) {
        methodName = method;
    }

    public static Method getByBytes(ByteBuffer bytes, int length) {
        return BytesUtils.getByBytes(SUPPORTED_METHODS.entrySet(), bytes, length);
    }
}
