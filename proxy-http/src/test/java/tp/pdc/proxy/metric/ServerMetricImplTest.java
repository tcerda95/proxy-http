package tp.pdc.proxy.metric;

import static org.junit.Assert.*;

import java.util.Random;
import org.junit.Test;

public class ServerMetricImplTest {
	
	private static final int BOUND = 10000;
	
	private Random rand = new Random();
	private ServerMetricImpl metrics = ServerMetricImpl.getInstance();

	@Test
	public void testStatusCodeCount() {
		assertEquals(0, metrics.getResponseCodeCount(200));
		
		int n = rand.nextInt(BOUND);
		
		for (int i = 0; i < n; i++)
			metrics.addResponseCodeCount(200);
		
		assertEquals(n, metrics.getResponseCodeCount(200));
	}

}
