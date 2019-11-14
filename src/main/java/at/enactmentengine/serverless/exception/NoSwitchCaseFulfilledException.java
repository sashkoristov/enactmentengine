package at.enactmentengine.serverless.exception;

/**
 * Custom exception which is thrown when no switch case is fulfilled.
 * 
 * based on @author markus
 */
public class NoSwitchCaseFulfilledException extends Exception {
	private static final long serialVersionUID = 1L;

	public NoSwitchCaseFulfilledException(String message) {
		super(message);
	}
}
