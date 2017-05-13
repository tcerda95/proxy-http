package tp.pdc.proxy.structures;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class FixedLengthQueueTest {

    private FixedLengthQueue<Integer> q;

    @Before
    public void init() {
        q = new FixedLengthQueue<Integer>(Integer.class, 5);
    }

    @Test(expected = IllegalStateException.class)
    public void emptyDequeue(){
        q.dequeue();
    }

    @Test
    public void addElements() {
        int i = 0;

        while(!q.isFull())
            q.queue(i++);

        i=0;
        while(!q.isEmpty())
            assertTrue(q.dequeue().equals(i++));
    }

    @Test
    public void checkSize() {
        q.queue(1);
        assertEquals(1, q.size());
        q.queue(2);
        assertEquals(2, q.size());
        q.queue(3);
        assertEquals(3, q.size());
        q.dequeue();
        assertEquals(2, q.size());
        q.dequeue();
        assertFalse(q.isEmpty());
        q.dequeue();
        assertTrue(q.isEmpty());
    }

    @Test
    public void emptyCheck() {
        q.queue(0);
        assertFalse(q.isEmpty());
        q.dequeue();
        assertTrue(q.isEmpty());
    }

    @Test
    public void fullCheck() {
        assertFalse(q.isFull());
        q.queue(1);
        assertFalse(q.isFull());
        q.queue(2);
        assertFalse(q.isFull());
        q.queue(3);
        assertFalse(q.isFull());
        q.queue(4);
        assertFalse(q.isFull());
        q.queue(5);
        assertTrue(q.isFull());
    }
    
    @Test
    public void stepOverFirstWhenFull() {
    	for (int i = 0; i < q.length(); i++) {
    		q.queue(i);
    	}
    	
    	assertEquals(0, q.peek().intValue());
    	
    	q.queue(q.length());
    	
    	for (int i = 1; i <= q.length(); i++)
    		assertEquals(i, q.dequeue().intValue());
    	
    	assertTrue(q.isEmpty());
    }
}
