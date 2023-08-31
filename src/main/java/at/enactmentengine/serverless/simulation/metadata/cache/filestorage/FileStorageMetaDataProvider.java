package at.enactmentengine.serverless.simulation.metadata.cache.filestorage;

import at.enactmentengine.serverless.simulation.metadata.cache.daos.*;

import java.nio.file.Path;

/**
 * This file was originally part of the bachelor thesis 'Tracing and Simulation Framework for AFCL' supervised by Sasko
 * Ristov, written by
 *
 * @author Philipp Gritsch
 * <p>
 * The file was moved and adapted to this project by Mika Hautz.
 */
public class FileStorageMetaDataProvider implements AutoCloseable {
    private static FileStorageMetaDataProvider INSTANCE;

    private FunctionImplementationDao functionImplementationDao;

    private FunctionDeploymentDao functionDeploymentDao;

    private ProviderDao providerDao;

    private RegionDao regionDao;

    private CpuDao cpuDao;

    private DataTransferDao dataTransferDao;

    private NetworkingDao networkingDao;

    private ServiceDao serviceDao;

    private ServiceDeploymentDao serviceDeploymentDao;

    public static synchronized FileStorageMetaDataProvider get() {
        if (FileStorageMetaDataProvider.INSTANCE == null) {
            FileStorageMetaDataProvider.INSTANCE = new FileStorageMetaDataProvider();
        }
        return FileStorageMetaDataProvider.INSTANCE;
    }

    private FileStorageMetaDataProvider() {
    }

    public FunctionImplementationDao functionImplementationDao() {
        if (this.functionImplementationDao == null) {
            try {
                this.functionImplementationDao = new JsonFunctionImplementationDao(Path.of("metadata/functionimplementation.json"));
            } catch (final Exception e) {
                throw new IllegalStateException("an error occured while loading function implementations");
            }
        }
        return this.functionImplementationDao;
    }

    public FunctionDeploymentDao functionDeploymentDao() {
        if (this.functionDeploymentDao == null) {
            try {
                this.functionDeploymentDao = new JsonFunctionDeploymentDao(Path.of("metadata/functiondeployment.json"));
            } catch (final Exception e) {
                throw new IllegalStateException("an error occured while loading function deployments");
            }
        }
        return this.functionDeploymentDao;
    }

    public synchronized ProviderDao providerDao() {
        if (this.providerDao == null) {
            try {
                this.providerDao = new JsonProviderDao(Path.of("metadata/provider.json"));
            } catch (final Exception e) {
                throw new IllegalStateException("an error occured while loading providers");
            }
        }
        return this.providerDao;
    }

    public RegionDao regionDao() {
        if (this.regionDao == null) {
            try {
                this.regionDao = new JsonRegionDao(Path.of("metadata/region.json"));
            } catch (final Exception e) {
                throw new IllegalStateException("an error occured while loading regions");
            }
        }
        return this.regionDao;
    }

    public CpuDao cpuDao() {
        if (this.cpuDao == null) {
            try {
                this.cpuDao = new JsonCpuDao(Path.of("metadata/cpu.json"));
            } catch (final Exception e) {
                throw new IllegalStateException("an error occured while loading cpus");
            }
        }
        return this.cpuDao;
    }

    public DataTransferDao dataTransferDao() {
        if (this.dataTransferDao == null) {
            try {
                this.dataTransferDao = new JsonDataTransferDao(Path.of("metadata/datatransfer.json"));
            } catch (final Exception e) {
                throw new IllegalStateException("an error occured while loading datatransfers");
            }
        }
        return this.dataTransferDao;
    }

    public NetworkingDao networkingDao() {
        if (this.networkingDao == null) {
            try {
                this.networkingDao = new JsonNetworkingDao(Path.of("metadata/networking.json"));
            } catch (final Exception e) {
                throw new IllegalStateException("an error occured while loading networking");
            }
        }
        return this.networkingDao;
    }

    public ServiceDao serviceDao() {
        if (this.serviceDao == null) {
            try {
                this.serviceDao = new JsonServiceDao(Path.of("metadata/service.json"));
            } catch (final Exception e) {
                throw new IllegalStateException("an error occured while loading service");
            }
        }
        return this.serviceDao;
    }

    public ServiceDeploymentDao serviceDeploymentDao() {
        if (this.serviceDeploymentDao == null) {
            try {
                this.serviceDeploymentDao = new JsonServiceDeploymentDao(Path.of("metadata/servicedeployment.json"));
            } catch (final Exception e) {
                throw new IllegalStateException("an error occured while loading networking");
            }
        }
        return this.serviceDeploymentDao;
    }

    @Override
    public void close() throws Exception {
        // noop
    }
}
