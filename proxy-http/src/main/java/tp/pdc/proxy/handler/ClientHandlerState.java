package tp.pdc.proxy.handler;

public enum ClientHandlerState {
	NOT_CONNECTED (false),
	CONNECTING (false),
	CONNECTED (true),
	REQUEST_PROCESSED (true),
	REQUEST_PROCESSED_CONNECTING(false),
	REQUEST_SENT (false),
	ERROR (false);
	
	private boolean shouldWrite;
	
	private ClientHandlerState(boolean shouldWrite) {
		this.shouldWrite = shouldWrite;
	}
	
	public boolean shouldWrite() {
		return shouldWrite;
	}
}
