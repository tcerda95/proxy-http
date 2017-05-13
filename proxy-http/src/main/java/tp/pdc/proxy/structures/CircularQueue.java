package tp.pdc.proxy.structures;

public interface CircularQueue<T> extends SimpleQueue<T> {
	int length();
    boolean isFull();
}
