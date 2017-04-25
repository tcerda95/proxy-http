package tp.pdc.proxy.client;

public enum ClientHandlerState {
	NOT_CONNECTED (false),
	CONNECTING (false),
	CONNECTED (true),
	REQUEST_PROCESSED (true),
	REQUEST_SENT (true),
	ERROR (false);
	
	private boolean shouldWrite;
	
	private ClientHandlerState(boolean shouldWrite) {
		this.shouldWrite = shouldWrite;
	}
	
	public boolean shouldWrite() {
		return shouldWrite;
	}
}
