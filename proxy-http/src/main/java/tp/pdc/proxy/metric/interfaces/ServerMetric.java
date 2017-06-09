package tp.pdc.proxy.metric.interfaces;

import java.util.Set;

/**
 * Metrics related to the server's activity, such as response codes information.
 */
public interface ServerMetric extends HostMetric {

	/**
	 * Add one to the count of a certain response code
	 * @param responseCode
     */
	void addResponseCodeCount (int responseCode);

	/**
	 * Get a certainn response code quantity
	 * @param responseCode
	 * @return quantity of a response code returned from the server
     */
	int getResponseCodeCount (int responseCode);

	/**
	 * Gets a set of all the status codes.
	 * @return a {@link Set} of the status codes
     */
	Set<Integer> getStatusCodes ();
}
