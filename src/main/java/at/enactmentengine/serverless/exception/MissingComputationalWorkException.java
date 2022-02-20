package at.enactmentengine.serverless.exception;

/**
 * Custom exception that is thrown when no value for the computational work in the metadata-DB
 * is given for a function implementation.
 */
public class MissingComputationalWorkException extends Exception {
    private static final long serialVersionUID = 1L;

    public MissingComputationalWorkException(String message) {
        super(message);
    }
}
