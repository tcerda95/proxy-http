package tp.pdc.proxy.structures;

import org.junit.Before;
import org.junit.Test;

import java.util.NoSuchElementException;

import static org.junit.Assert.*;

public class ArrayQueueTest {

	private ArrayQueue<Integer> q;

	@Before
	public void init () {
		q = new ArrayQueue<Integer>(Integer.class, 5);
	}

	@Test(expected = NoSuchElementException.class)
	public void emptyDequeue () {
		q.remove();
	}

	@Test
	public void addElements () {
		int i = 0;

		while (!q.isFull())
			q.offer(i++);

		i = 0;
		while (!q.isEmpty())
			assertTrue(q.poll().equals(i++));
	}

	@Test
	public void checkSize () {
		q.offer(1);
		assertEquals(1, q.size());
		q.offer(2);
		assertEquals(2, q.size());
		q.offer(3);
		assertEquals(3, q.size());
		q.poll();
		assertEquals(2, q.size());
		q.poll();
		assertFalse(q.isEmpty());
		q.poll();
		assertTrue(q.isEmpty());
	}

	@Test
	public void emptyCheck () {
		q.offer(0);
		assertFalse(q.isEmpty());
		q.poll();
		assertTrue(q.isEmpty());
	}

	@Test
	public void fullCheck () {
		assertFalse(q.isFull());
		q.offer(1);
		assertFalse(q.isFull());
		q.offer(2);
		assertFalse(q.isFull());
		q.offer(3);
		assertFalse(q.isFull());
		q.offer(4);
		assertFalse(q.isFull());
		q.offer(5);
		assertTrue(q.isFull());
	}

	@Test
	public void stepOverFirstWhenFull () {
		for (int i = 0; i < q.length(); i++)
			q.offer(i);

		assertEquals(0, q.peek().intValue());

		q.offer(q.length());

		for (int i = 1; i <= q.length(); i++)
			assertEquals(i, q.poll().intValue());

		assertTrue(q.isEmpty());
	}

	@Test
	public void iteratorTest () {
		for (int i = 0; i < q.length(); i++)
			q.offer(i);

		int j = 0;
		for (Integer i : q)
			assertEquals(j++, i.intValue());
	}
}
