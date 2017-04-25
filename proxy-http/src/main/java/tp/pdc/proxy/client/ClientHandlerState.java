package tp.pdc.proxy.client;

public enum ClientHandlerState {
	NOT_CONNECTED,
	CONNECTING,
	CONNECTED,
	REQUEST_PROCESSED,
	REQUEST_SENT,
	ERROR;
}
