package tp.pdc.proxy.metric.interfaces;

public interface ServerMetric extends HostMetric {
	public void addResponseCodeCount(int responseCode);
	public int getResponseCodeCount(int responseCode);
}
