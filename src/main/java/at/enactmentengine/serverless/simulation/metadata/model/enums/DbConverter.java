package at.enactmentengine.serverless.simulation.metadata.model.enums;

/**
 * This file was originally part of the bachelor thesis 'Tracing and Simulation Framework for AFCL'
 * supervised by Sasko Ristov, written by
 *
 * @author Philipp Gritsch
 * <p>
 * The file was moved to this project by Mika Hautz.
 */
public interface DbConverter {

    Object from(String dbValue);

    String to(Object object);

}
