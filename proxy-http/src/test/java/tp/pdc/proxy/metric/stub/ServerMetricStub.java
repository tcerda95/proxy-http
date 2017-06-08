package tp.pdc.proxy.metric.stub;

import tp.pdc.proxy.metric.HostMetricImpl;
import tp.pdc.proxy.metric.interfaces.ServerMetric;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ServerMetricStub extends HostMetricImpl implements ServerMetric {

	private final Map<Integer, Integer> responseCodeCount;

	public ServerMetricStub () {
		responseCodeCount = new HashMap<>();
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
