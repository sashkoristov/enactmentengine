package at.enactmentengine.serverless.exception;

/**
 * Custom exception that is thrown when during simulation a function
 * has not been invoked yet.
 */
public class NotYetInvokedException extends Exception {
    private static final long serialVersionUID = 1L;

    public NotYetInvokedException(String message) {
        super(message);
    }
}
