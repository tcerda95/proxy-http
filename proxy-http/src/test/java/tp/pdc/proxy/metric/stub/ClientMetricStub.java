package tp.pdc.proxy.metric.stub;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

import tp.pdc.proxy.header.Method;
import tp.pdc.proxy.metric.HostMetricImpl;
import tp.pdc.proxy.metric.interfaces.ClientMetric;

public class ClientMetricStub extends HostMetricImpl implements ClientMetric {

	private final Map<Method, Integer> methodRequests;
	
	public ClientMetricStub() {
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
	
	@Override
	public Set<Method> getMethods() {
		return methodRequests.keySet();
	}


}
