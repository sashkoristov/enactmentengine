package at.enactmentengine.serverless.simulation.metadata.model;

/**
 * This file was originally part of the bachelor thesis <a href="https://github.com/sashkoristov/AFCLDeployer">AFCLDeployer</a>
 * supervised by Sasko Ristov, written by
 *
 * @author Christoph Abenthung and Caroline Haller
 * <p>
 * The file was moved and refactored to a new project in order to extract a common API by Philipp Gritsch as part of the
 * 'Tracing and Simulation Framework for AFCL' bachelor thesis
 */
public interface Entity<ID> {

    ID getId();

}
