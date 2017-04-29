package tp.pdc.proxy.exceptions;

@SuppressWarnings("serial")
public class ParserFormatException extends Exception {

	public ParserFormatException() {
		super();
	}
	
    public ParserFormatException(String message) {
        super(message);
    }
}
