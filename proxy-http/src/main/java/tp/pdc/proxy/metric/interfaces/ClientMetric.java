package tp.pdc.proxy.metric.interfaces;

import tp.pdc.proxy.header.Method;

import java.util.Set;

public interface ClientMetric extends HostMetric {
	void addMethodCount (Method m);

	int getMethodCount (Method m);

	Set<Method> getMethods ();
}
