package at.enactmentengine.serverless.simulation.metadata.cache.daos;

import java.util.List;

/**
 * This file was originally part of the bachelor thesis 'Tracing and Simulation Framework for AFCL'
 * supervised by Sasko Ristov, written by
 *
 * @author Philipp Gritsch
 * <p>
 * The file was moved and adapted to this project by Mika Hautz.
 */
public interface Dao<I, ID> {

    I getById(ID id) throws Exception;

    List<I> getAll() throws Exception;

}
