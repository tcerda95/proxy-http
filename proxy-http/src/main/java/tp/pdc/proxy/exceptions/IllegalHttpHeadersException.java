package tp.pdc.proxy.exceptions;

@SuppressWarnings("serial")
public class IllegalHttpHeadersException extends Exception {
	
	public IllegalHttpHeadersException() {
		super();
	}
	
    public IllegalHttpHeadersException(String message) {
        super(message);
    }
}
