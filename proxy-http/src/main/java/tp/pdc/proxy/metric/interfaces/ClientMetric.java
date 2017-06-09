package tp.pdc.proxy.metric.interfaces;

import tp.pdc.proxy.header.Method;

import java.util.Set;

/**
 * Metrics related to the client's activity, as methods requested.
 */
public interface ClientMetric extends HostMetric {

	/**
	 * Add one to the amount of a specific {@link Method} count.
	 * @param m
     */
	void addMethodCount (Method m);

	/**
	 * Get the quantity of a specific requested {@link Method}
	 * @param m method to get the count
	 */
	int getMethodCount (Method m);

	/**
	 * Gets a set with all the methods
	 * @return a {@link Set} with all the methods
     */
	Set<Method> getMethods ();
}
