package tp.pdc.proxy.time;

import java.util.concurrent.TimeUnit;

public class ExpirableContainer<T> {
	private final T element;
	private final long expirationTime;
	
	public ExpirableContainer(T element, long timeToLive, TimeUnit timeUnit) {
		this.element = element;
		this.expirationTime = System.currentTimeMillis() + timeUnit.toMillis(timeToLive);
	}
	
	public T getElement() {
		return element;
	}
	
	public boolean hasExpired() {
		return System.currentTimeMillis() >= expirationTime;
	}
}
