package at.enactmentengine.serverless.simulation.metadata.exceptions;

public class DatabaseException extends RuntimeException {
    public DatabaseException(String message) {
        super(message);
    }
}
