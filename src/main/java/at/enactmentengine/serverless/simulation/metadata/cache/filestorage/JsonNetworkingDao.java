package at.enactmentengine.serverless.simulation.metadata.cache.filestorage;

import at.enactmentengine.serverless.simulation.metadata.cache.daos.NetworkingDao;
import at.enactmentengine.serverless.simulation.metadata.model.Networking;

import java.io.IOException;
import java.nio.file.Path;

public class JsonNetworkingDao extends JsonDao<Networking, Integer> implements NetworkingDao {
    public JsonNetworkingDao(final Path file) throws IOException {
        super(file);
    }

    @Override
    protected Class<Networking> getEntityClass() {
        return Networking.class;
    }
}
