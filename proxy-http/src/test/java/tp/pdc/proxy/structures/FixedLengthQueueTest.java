package tp.pdc.proxy.structures;

import com.sun.source.tree.AssertTree;
import org.junit.Before;
import org.junit.Test;
import tp.pdc.proxy.exceptions.ParserFormatException;
import static org.junit.Assert.*;

public class FixedLengthQueueTest {

    FixedLengthQueue<Integer> q;

    @Before
    public void init(){
        q = new FixedLengthQueue<Integer>(Integer.class, 5);
    }

    @Test(expected = IllegalStateException.class)
    public void emptyDequeue(){
        Integer i = q.dequeue();
    }

    @Test
    public void addElements(){
        int i = 0;

        while(!q.isFull())
            q.queue(i++);

        i=0;
        while(!q.isEmpty())
            assertTrue(q.dequeue().equals(i++));
    }

    @Test
    public void checkSize(){
        q.queue(1);
        assertTrue(q.size() == 1);
        q.queue(2);
        q.queue(3);
        assertTrue(q.size() == 3);
        q.dequeue();
        assertTrue(q.size() == 2);
        q.dequeue();
        assertFalse(q.isEmpty());
        q.dequeue();

    }

    @Test
    public void emptyCheck(){
        q.queue(0);
        assertFalse(q.isEmpty());
        q.dequeue();
        assertTrue(q.isEmpty());
    }

    @Test
    public void fullCheck(){
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

    @Test(expected = IllegalStateException.class)
    public void queueMore(){
        int i = 0;

        while(!q.isFull())
            q.queue(i++);

        q.queue(0);
    }
}
