package tp.pdc.proxy.exceptions;

import tp.pdc.proxy.HttpErrorCode;

/**
 * Exception for errors ocurred while parsing.
 * Sets a Bad Request error code.
 * @see HttpErrorCode
 */
@SuppressWarnings("serial")
public class ParserFormatException extends Exception {

	private HttpErrorCode responseErrorCode = HttpErrorCode.BAD_REQUEST_400;

	public ParserFormatException () {
		super();
	}

	public ParserFormatException (String message) {
		super(message);
	}

	public ParserFormatException (String message, HttpErrorCode responseErrorCode) {
		this(message);
		this.responseErrorCode = responseErrorCode;
	}

	public HttpErrorCode getResponseErrorCode () {
		return responseErrorCode;
	}
}
