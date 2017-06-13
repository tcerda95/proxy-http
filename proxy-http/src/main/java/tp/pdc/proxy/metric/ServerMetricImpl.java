package tp.pdc.proxy.metric;

import tp.pdc.proxy.metric.interfaces.ServerMetric;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of the servers metrics.
 */
public class ServerMetricImpl extends HostMetricImpl implements ServerMetric {

	private static final ServerMetricImpl INSTANCE = new ServerMetricImpl();

	private final Map<Integer, Integer> responseCodeCount;

	private ServerMetricImpl () {
		responseCodeCount = new HashMap<>();
	}

	public static final ServerMetricImpl getInstance () {
		return INSTANCE;
	}

	@Override
	public void addResponseCodeCount (int responseCode) {
		int count = getResponseCodeCount(responseCode);
		responseCodeCount.put(responseCode, count + 1);
	}

	@Override
	public int getResponseCodeCount (int responseCode) {
		return responseCodeCount.getOrDefault(responseCode, 0);
	}

	@Override
	public Set<Integer> getStatusCodes () {
		return responseCodeCount.keySet();
	}
}
