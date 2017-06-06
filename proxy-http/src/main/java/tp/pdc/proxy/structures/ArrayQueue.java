package tp.pdc.proxy.structures;

import java.lang.reflect.Array;
import java.util.*;

public class ArrayQueue<T> extends AbstractQueue<T> implements FixedLengthQueue<T> {

    public T arr[];
    private int queueIndex, dequeueIndex;
    private int currentSize;

    @SuppressWarnings("unchecked")
	public ArrayQueue(Class<T> clazz, int length) {
        arr = (T[]) Array.newInstance(clazz, length);
    }

    @Override
    public boolean offer(T e) {
    	Objects.requireNonNull(e);
    	
        if (isFull())
        	poll();

        arr[queueIndex] = e;
        queueIndex = (queueIndex + 1) % arr.length;
        currentSize++;
        return true;
    }

    @Override
    public T poll() {
        if (isEmpty())
            return null;

        T e = arr[dequeueIndex];
        arr[dequeueIndex] = null;
        dequeueIndex = (dequeueIndex + 1) % arr.length;
        currentSize--;
        return e;

    }

    @Override
    public T peek() {
        if (isEmpty())
            return null;
        
        return arr[dequeueIndex];
    }

    @Override
    public int size(){
        return currentSize;
    }
    
    @Override
    public boolean isFull() {
    	return currentSize == arr.length;
    }

	@Override
	public int length() {
		return arr.length;
	}

	@Override
	public Iterator<T> iterator() {
		return new ArrayQueueIterator<>(arr, size(), dequeueIndex);
	}
	
	private static class ArrayQueueIterator<E> implements Iterator<E> {

		private E[] arr;
		private int remainingItems;
		private int dequeueIndex;
		
		public ArrayQueueIterator(E[] arr, int queueSize, int dequeueIndex) {
			this.arr = arr;
			this.remainingItems = queueSize;
			this.dequeueIndex = dequeueIndex;
		}
		
		@Override
		public boolean hasNext() {
			return remainingItems > 0;
		}

		@Override
		public E next() {
            if (!hasNext())
                throw new NoSuchElementException();
			E elem = arr[dequeueIndex];
			dequeueIndex = (dequeueIndex + 1) % arr.length;
			remainingItems--;
			return elem;
		}
	}
}
