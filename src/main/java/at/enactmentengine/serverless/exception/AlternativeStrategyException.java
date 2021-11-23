package at.enactmentengine.serverless.exception;

/**
 * Custom exception that is thrown when during simulating an alternative strategy
 * all strategies have been tried without success.
 */
public class AlternativeStrategyException extends Exception {
    private static final long serialVersionUID = 1L;

    public AlternativeStrategyException(String message) {
        super(message);
    }
}
