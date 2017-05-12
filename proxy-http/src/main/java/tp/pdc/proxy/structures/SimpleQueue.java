package tp.pdc.proxy.structures;

public interface SimpleQueue<T> {

    void queue(T e);
    T dequeue();
    boolean isEmpty();
    T peek();

}
