package at.enactmentengine.serverless.exception;

/**
 * Custom exception that is thrown when during simulation no database entry in the
 * metadata database was found for a give resourcelink.
 */
public class NoDatabaseEntryForIdException extends Exception {
    private static final long serialVersionUID = 1L;

    public NoDatabaseEntryForIdException(String message) {
        super(message);
    }
}
