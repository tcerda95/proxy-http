package tp.pdc.proxy.header;

import tp.pdc.proxy.bytes.BytesUtils;
import tp.pdc.proxy.properties.ProxyProperties;

import java.nio.ByteBuffer;


/**
 * Enum containing every http method
 */
public enum Method {
	GET("GET"), POST("POST"), HEAD("HEAD"), OPTIONS("OPTIONS"), PUT("PUT"),
	DELETE("DELETE"), TRACE("TRACE"), CONNECT("CONNECT");

	private String methodName;
	private byte[] methodBytes;

	Method (String method) {
		methodName = method;
		methodBytes = method.getBytes(ProxyProperties.getInstance().getCharset());
	}

	public static Method getByBytes (ByteBuffer bytes, int length) {
		for (Method method : values())
			if (BytesUtils.equalsBytes(method.methodBytes, bytes, length))
				return method;
		return null;
	}

	public static int maxMethodLen () {
		int maxLength = 0;
		for (Method m : Method.values()) {
			int currentLength = m.toString().length();

			if (currentLength > maxLength)
				maxLength = currentLength;
		}
		return maxLength;
	}

	@Override
	public String toString () {
		return methodName;
	}

	public byte[] getBytes () {
		return methodBytes;
	}
}
