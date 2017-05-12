package tp.pdc.proxy.structures;

import java.lang.reflect.Array;

public class FixedLengthQueue<T> implements CircularQueue<T> {

    public T arr[];
    private int queueIndex, dequeueIndex;
    private int length;
    private int currentSize;

    public FixedLengthQueue(Class<T> clazz, int length) {
        arr = (T[]) Array.newInstance(clazz, length);
        this.length = length;

    }

    @Override
    public boolean isFull() {
        if(currentSize == length)
            return true;
        return false;
    }

    @Override
    public void queue(T e) {
        if (isFull())
            throw new IllegalStateException("Queue is full");

        arr[queueIndex] = e;
        queueIndex = (queueIndex + 1) % length;
        currentSize++;
    }

    @Override
    public T dequeue() {
        if(isEmpty())
            throw new IllegalStateException("Queue is empty");

        T e = arr[dequeueIndex];
        arr[dequeueIndex] = null;
        dequeueIndex = (dequeueIndex + 1) % length;
        currentSize--;
        return e;

    }

    @Override
    public boolean isEmpty() {
        return currentSize == 0;
    }

    @Override
    public T peek() {
        if (isEmpty())
            throw new IllegalStateException("Queue is Empty");
        return arr[queueIndex];
    }

    public int size(){
        return currentSize;
    }
}
