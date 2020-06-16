package at.enactmentengine.serverless.exception;

/**
 * Custom exception which is thrown when a function doesn't have a resource
 * link.
 * <p>
 * based on @author markusmoosbrugger, jakobnoeckl
 */
public class MissingResourceLinkException extends Exception {
    private static final long serialVersionUID = 1L;

    public MissingResourceLinkException(String message) {
        super(message);
    }
}
