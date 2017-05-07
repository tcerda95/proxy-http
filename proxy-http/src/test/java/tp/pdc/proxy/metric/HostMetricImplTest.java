package tp.pdc.proxy.metric;

import static org.junit.Assert.assertEquals;

import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.junit.Before;
import org.junit.Test;

public class HostMetricImplTest {

	private static final int BOUND = 10000;
	
	private HostMetricImpl metrics;
	private Random rand = new Random();
	
	@Before
	public void setUp() throws Exception {
		metrics = new HostMetricImpl();
	}

	@Test
	public void testConnections() {
		assertEquals(0, metrics.getConnections());
		
		int n = rand.nextInt(BOUND);
		
		for (int i = 0; i < n; i++)
			metrics.addConnection();
		
		assertEquals(n, metrics.getConnections());
	}
	
	@Test
	public void testAddReadBytes() {
		testByteAddition(metrics::getBytesRead, metrics::addBytesRead);
	}

	@Test
	public void testAddWriteBytes() {
		testByteAddition(metrics::getBytesWritten, metrics::addBytesWritten);
	}
	
	private void testByteAddition(Supplier<Long> getter, Consumer<Integer> adder) {
		assertEquals(0, getter.get().intValue());
		int bytesAdded = addRandomBytes(adder);
		assertEquals(bytesAdded, getter.get().intValue());		
	}
	
	private int addRandomBytes(Consumer<Integer> adder) {
		int n = rand.nextInt(BOUND);
		adder.accept(n);
		return n;
	}
	
}
