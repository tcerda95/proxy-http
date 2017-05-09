package tp.pdc.proxy.metric.interfaces;

public interface HostMetric {
	public long getConnections();
	public long getBytesRead();
	public long getBytesWritten();
	public void addConnection();
	public void addBytesRead(long bytes);
	public void addBytesWritten(long bytes);
}
