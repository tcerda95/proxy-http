package tp.pdc.proxy.metric.interfaces;

import java.util.Set;

public interface ServerMetric extends HostMetric {
	void addResponseCodeCount(int responseCode);
	int getResponseCodeCount(int responseCode);
	Set<Integer> getStatusCodes();
}
