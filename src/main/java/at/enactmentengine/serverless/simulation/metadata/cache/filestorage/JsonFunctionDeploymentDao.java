package at.enactmentengine.serverless.simulation.metadata.cache.filestorage;

import at.enactmentengine.serverless.simulation.metadata.cache.daos.FunctionDeploymentDao;
import at.enactmentengine.serverless.simulation.metadata.model.FunctionDeployment;

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
public class JsonFunctionDeploymentDao extends JsonDao<FunctionDeployment, Long>
        implements FunctionDeploymentDao {

    public JsonFunctionDeploymentDao(final Path file) throws IOException {
        super(file);
    }

    @Override
    protected Class<FunctionDeployment> getEntityClass() {
        return FunctionDeployment.class;
    }

}
