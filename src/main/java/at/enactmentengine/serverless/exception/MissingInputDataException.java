package at.enactmentengine.serverless.exception;

/**
 * Custom exception which is thrown when a specified data input is missing.
 * <p>
 * based on @author markusmoosbrugger, jakobnoeckl
 */
public class MissingInputDataException extends Exception {
    private static final long serialVersionUID = 1L;

    public MissingInputDataException(String message) {
        super(message);
    }
}
