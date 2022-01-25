package at.enactmentengine.serverless.exception;

/**
 * Custom exception which is thrown when a specified data output is missing.
 * <p>
 * based on @author andreasreheis
 */
public class MissingOutputDataException extends Exception {
    private static final long serialVersionUID = 1L;

    public MissingOutputDataException(String message) {
        super(message);
    }
}
