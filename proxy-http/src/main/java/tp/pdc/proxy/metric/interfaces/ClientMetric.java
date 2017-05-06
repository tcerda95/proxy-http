package tp.pdc.proxy.metric.interfaces;

import tp.pdc.proxy.header.Method;

public interface ClientMetric extends HostMetric {
	void addMethodCount(Method m);
	int getMethodCount(Method m);
}
