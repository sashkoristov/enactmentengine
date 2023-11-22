package at.enactmentengine.serverless.simulation.metadata.cache.filestorage;

import at.enactmentengine.serverless.simulation.metadata.cache.daos.ProviderDao;
import at.enactmentengine.serverless.simulation.metadata.model.Provider;

import java.io.IOException;
import java.nio.file.Path;

/**
 * This file was originally part of the bachelor thesis 'Tracing and Simulation Framework for AFCL'
 * supervised by Sasko Ristov, written by
 *
 * @author Philipp Gritsch
 * <p>
 * The file was moved to this project by Mika Hautz.
 */
public class JsonProviderDao extends JsonDao<Provider, Integer> implements ProviderDao {

    public JsonProviderDao(final Path file) throws IOException {
        super(file);
    }

    @Override
    protected Class<Provider> getEntityClass() {
        return Provider.class;
    }
}
