package at.enactmentengine.serverless.simulation.metadata.cache.filestorage;

import at.enactmentengine.serverless.simulation.metadata.cache.daos.DataTransferDao;
import at.enactmentengine.serverless.simulation.metadata.model.DataTransfer;

import java.io.IOException;
import java.nio.file.Path;

public class JsonDataTransferDao extends JsonDao<DataTransfer, Integer> implements DataTransferDao {

    public JsonDataTransferDao(final Path file) throws IOException {
        super(file);
    }

    @Override
    protected Class<DataTransfer> getEntityClass() {
        return DataTransfer.class;
    }
}
