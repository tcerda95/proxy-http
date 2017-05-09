package tp.pdc.proxy.metric;

import java.util.EnumMap;
import java.util.Map;

import tp.pdc.proxy.header.Method;
import tp.pdc.proxy.metric.interfaces.ClientMetric;

public class ClientMetricImpl extends HostMetricImpl implements ClientMetric {
	
	private static final ClientMetricImpl INSTANCE = new ClientMetricImpl();
	
	private final Map<Method, Integer> methodRequests;
	
	public static final ClientMetricImpl getInstance() {
		return INSTANCE;
	}
	
	private ClientMetricImpl() {
		methodRequests = new EnumMap<>(Method.class);
		for (Method m : Method.values())
			methodRequests.put(m, 0);
	}
	
	@Override
	public void addMethodCount(Method m) {
		int count = getMethodCount(m);
		methodRequests.put(m, count + 1);
	}
	
	@Override
	public int getMethodCount(Method m) {
		return methodRequests.get(m);
	}
}
