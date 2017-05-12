package tp.pdc.proxy.metric;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import tp.pdc.proxy.metric.interfaces.ServerMetric;

public class ServerMetricImpl extends HostMetricImpl implements ServerMetric {
	
	private static final ServerMetricImpl INSTANCE = new ServerMetricImpl();
	
	private final Map<Integer, Integer> responseCodeCount;
	
	public static final ServerMetricImpl getInstance() {
		return INSTANCE;
	}
	
	private ServerMetricImpl() {
		responseCodeCount = new HashMap<>();
	}
	
	@Override
	public void addResponseCodeCount(int responseCode) {
		int count = getResponseCodeCount(responseCode);
		responseCodeCount.put(responseCode, count + 1);
	}

	@Override
	public int getResponseCodeCount(int responseCode) {
		return responseCodeCount.getOrDefault(responseCode, 0);
	}
	
	public Set<Integer> statusCodesFound() {
		return responseCodeCount.keySet();
	}
}
