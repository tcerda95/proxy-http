package tp.pdc.proxy.structures;

import java.util.Queue;

public interface FixedLengthQueue<T> extends Queue<T> {
	int length();
    boolean isFull();
}
