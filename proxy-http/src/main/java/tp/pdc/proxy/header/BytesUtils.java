package tp.pdc.proxy.header;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Set;

public class BytesUtils {

    public static <T> T getByBytes(Set<Map.Entry<byte[],T>> set, ByteBuffer bytes, int length) {
        boolean equals;

        for (Map.Entry<byte[], T> e: set) {
            equals = true;
            if (length != e.getKey().length)
                continue;

            bytes.mark();
            for (int i = 0; i < length && equals; i++) {
                if (bytes.get() != e.getKey()[i]) {
                    equals = false;
                }
            }
            bytes.reset();

            if (equals)
                return e.getValue();
        }

        return null;
    }
}
