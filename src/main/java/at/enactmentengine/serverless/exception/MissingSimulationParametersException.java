package at.enactmentengine.serverless.exception;

/**
 * Custom exception that is thrown if there are some fields missing for simulation in the
 * metadata-DB, e.g. networkOverheadms for the region, or faasSytemOverheadms / cryptoOverheadms for the provider.
 */
public class MissingSimulationParametersException extends Exception {
    private static final long serialVersionUID = 1L;

    public MissingSimulationParametersException(String message) {
        super(message);
    }
}
