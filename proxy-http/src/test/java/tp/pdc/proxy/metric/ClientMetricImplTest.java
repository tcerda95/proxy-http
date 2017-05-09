package tp.pdc.proxy.metric;

import static org.junit.Assert.assertEquals;

import java.util.Random;
import org.junit.Test;

import tp.pdc.proxy.header.Method;

public class ClientMetricImplTest {

	private static final int BOUND = 10000;
	
	private ClientMetricImpl metrics = ClientMetricImpl.getInstance();
	private Random rand = new Random();
	
	@Test
	public void testMethodCount() {
		for (Method m : Method.values())
			testMethodCount(m);
	}
	
	private void testMethodCount(Method m) {
		assertEquals(0, metrics.getMethodCount(m));
		
		int n = rand.nextInt(BOUND);
		for (int i = 0; i < n; i++)
			metrics.addMethodCount(m);
		
		assertEquals(n, metrics.getMethodCount(m));		
	}
}
