package at.enactmentengine.serverless.simulation.metadata.cache.filestorage;

import at.enactmentengine.serverless.simulation.metadata.cache.daos.ServiceDeploymentDao;
import at.enactmentengine.serverless.simulation.metadata.model.ServiceDeployment;

import java.io.IOException;
import java.nio.file.Path;

public class JsonServiceDeploymentDao extends JsonDao<ServiceDeployment, Integer> implements ServiceDeploymentDao {
    public JsonServiceDeploymentDao(final Path file) throws IOException {
        super(file);
    }

    @Override
    protected Class<ServiceDeployment> getEntityClass() {
        return ServiceDeployment.class;
    }
}
