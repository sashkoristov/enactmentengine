package at.enactmentengine.serverless.simulation.metadata.cache.filestorage;

import at.enactmentengine.serverless.simulation.metadata.cache.daos.FunctionImplementationDao;
import at.enactmentengine.serverless.simulation.metadata.model.FunctionImplementation;

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
public class JsonFunctionImplementationDao  extends JsonDao<FunctionImplementation, Long>
        implements FunctionImplementationDao {

    public JsonFunctionImplementationDao(final Path file) throws IOException {
        super(file);
    }

    @Override
    protected Class<FunctionImplementation> getEntityClass() {
        return FunctionImplementation.class;
    }

}
