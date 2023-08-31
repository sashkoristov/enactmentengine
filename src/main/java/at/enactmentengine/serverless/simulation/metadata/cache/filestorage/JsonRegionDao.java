package at.enactmentengine.serverless.simulation.metadata.cache.filestorage;

import at.enactmentengine.serverless.simulation.metadata.cache.daos.RegionDao;
import at.enactmentengine.serverless.simulation.metadata.model.Region;

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
public class JsonRegionDao extends JsonDao<Region, Integer> implements RegionDao {

    public JsonRegionDao(final Path file) throws IOException {
        super(file);
    }

    @Override
    protected Class<Region> getEntityClass() {
        return Region.class;
    }
}
