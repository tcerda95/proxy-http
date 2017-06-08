package tp.pdc.proxy.metric.interfaces;

/**
 * Metrics related to a specific host.
 */
public interface HostMetric {

	/**
	 * Get amount of connections
	 * @return quantity of connections
     */
	public long getConnections ();

	/**
	 * Get bytes read from a certain host.
	 * @return amount of bytes read
     */
	public long getBytesRead ();

	/**
	 * Get bytes written to a certain host
	 * @return amount of bytes written
     */
	public long getBytesWritten ();

	/**
	 * Add one to the amount of connections made to a certain host
	 */
	public void addConnection ();

	/**
	 * Add a quantity of bytes read from a certain host
	 * @param bytes read from a host
     */
	public void addBytesRead (long bytes);

	/**
	 * Add a quantity of bytes written to a certain host
	 * @param bytes written to a host
	 */
	public void addBytesWritten (long bytes);
}
