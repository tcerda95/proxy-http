package tp.pdc.proxy.statistics;

public class ServerStatistics extends HostStatistics {
	
	private static final ServerStatistics INSTANCE = new ServerStatistics();
	
	public static final ServerStatistics getInstance() {
		return INSTANCE;
	}
	
	private ServerStatistics() {
	}
	
}
