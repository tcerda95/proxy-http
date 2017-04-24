package tp.pdc.proxy.statistics;

public class ClientStatistics extends HostStatistics {
	
	private static final ClientStatistics INSTANCE = new ClientStatistics();
	
	public static final ClientStatistics getInstance() {
		return INSTANCE;
	}
	
	private ClientStatistics() {
	}
}
