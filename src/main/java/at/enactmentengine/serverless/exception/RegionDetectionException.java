package at.enactmentengine.serverless.exception;

/**
 * Custom exception which is thrown when a region cannot be detected
 * <p>
 * @author stefanpedratscher
 */
public class RegionDetectionException extends Exception {
    private static final long serialVersionUID = 1L;

    public RegionDetectionException(String message) {
        super(message);
    }
}
