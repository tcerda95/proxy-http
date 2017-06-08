package tp.pdc.proxy.structures;

import java.util.Queue;

/**
 * A {@link Queue} with a fixed length.
 * @param <T>
 */
public interface FixedLengthQueue <T> extends Queue<T> {
	/**
	 * Checks the queue length
	 * @return the queue length
     */
	int length ();

	/**
	 * Checks if the queue is full and there is no more space left
	 * @return true if the queue is full, false on the contrary
     */
	boolean isFull ();
}
