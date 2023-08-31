package at.enactmentengine.serverless.simulation.metadata.cache.filestorage;

import at.enactmentengine.serverless.simulation.metadata.cache.daos.CpuDao;
import at.enactmentengine.serverless.simulation.metadata.model.Cpu;

import java.io.IOException;
import java.nio.file.Path;

public class JsonCpuDao extends JsonDao<Cpu, Integer> implements CpuDao {

    public JsonCpuDao(final Path file) throws IOException {
        super(file);
    }

    @Override
    protected Class<Cpu> getEntityClass() {
        return Cpu.class;
    }
}