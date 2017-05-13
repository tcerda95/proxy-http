package tp.pdc.proxy.structures;

public interface FixedLengthQueue<T> extends SimpleQueue<T> {
	int length();
    boolean isFull();
}
