package at.enactmentengine.serverless.simulation.metadata.cache.filestorage;

import at.enactmentengine.serverless.simulation.metadata.cache.daos.ServiceDao;
import at.enactmentengine.serverless.simulation.metadata.model.Service;

import java.io.IOException;
import java.nio.file.Path;

public class JsonServiceDao extends JsonDao<Service, Integer> implements ServiceDao {
    public JsonServiceDao(final Path file) throws IOException {
        super(file);
    }

    @Override
    protected Class<Service> getEntityClass() {
        return Service.class;
    }
}

