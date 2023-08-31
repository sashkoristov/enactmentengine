package at.enactmentengine.serverless.simulation.metadata.database;

import at.enactmentengine.serverless.simulation.metadata.DataProvider;
import at.enactmentengine.serverless.simulation.metadata.cache.JsonProvider;
import at.enactmentengine.serverless.simulation.metadata.exceptions.DatabaseException;
import at.enactmentengine.serverless.simulation.metadata.model.*;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.jdbi.v3.core.Jdbi;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;

public class DatabaseProvider implements DataProvider {
    private static DatabaseProvider INSTANCE;
    private final Jdbi jdbiInstance;
    private final String pathToProperties = "mariaDatabase.properties";

    public static synchronized DatabaseProvider get() {
        if (DatabaseProvider.INSTANCE == null) {
            DatabaseProvider.INSTANCE = new DatabaseProvider();
        }
        return DatabaseProvider.INSTANCE;
    }

    private DatabaseProvider() {
        Properties databaseFile = new Properties();
        try {
            databaseFile.load(Files.newInputStream(Paths.get(pathToProperties)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        final String host = databaseFile.getProperty("host");
        final int port = Integer.parseInt(databaseFile.getProperty("port"));
        final String username = databaseFile.getProperty("username");
        final String password = databaseFile.getProperty("password");
        final String database = databaseFile.getProperty("database");
        final String db_url = "jdbc:mariadb://" + host + ":" + port + "/" + database;

        // Creating a Jdbi instance
        jdbiInstance = Jdbi.create(db_url, username, password);
    }

    @Override
    public FunctionDeployment getFunctionIdEntry(String kmsArn) {
        return jdbiInstance.withHandle(handle ->
                handle.createQuery("SELECT * FROM functiondeployment WHERE KMS_Arn = ?")
                        .bind(0, kmsArn)
                        .mapToBean(FunctionDeployment.class)
                        .findOne()
                        .orElseThrow(() -> new DatabaseException("No functiondeployment found with the given id: " + kmsArn))
        );
    }

    @Override
    public FunctionDeployment getDeploymentById(long id) {
        return jdbiInstance.withHandle(handle ->
                handle.createQuery("SELECT * FROM functiondeployment WHERE id = ?")
                        .bind(0, id)
                        .mapToBean(FunctionDeployment.class)
                        .findOne()
                        .orElseThrow(() -> new DatabaseException("No functiondeployment found with the given id: " + id))
        );
    }

    @Override
    public Provider getProviderEntry(at.uibk.dps.util.Provider providerEnum) {
        return jdbiInstance.withHandle(handle ->
                handle.createQuery("SELECT * FROM provider WHERE name = ?")
                        .bind(0, providerEnum.name())
                        .mapToBean(Provider.class)
                        .findOne()
                        .orElseThrow(() -> new DatabaseException("No provider found with the given name: " + providerEnum.name()))
        );
    }

    @Override
    public Region getRegionEntry(String region, at.uibk.dps.util.Provider providerEnum) {
        return jdbiInstance.withHandle(handle ->
                handle.createQuery("SELECT * FROM region WHERE region = ? AND provider = ?")
                        .bind(0, region)
                        .bind(1, providerEnum.name())
                        .mapToBean(Region.class)
                        .findOne()
                        .orElseThrow(() -> new DatabaseException("No region found with the given code: " + region +
                                " for provider " + providerEnum.name()))
        );
    }

    @Override
    public int getRegionId(String regionName) {
        return jdbiInstance.withHandle(handle ->
                handle.createQuery("SELECT id FROM region WHERE region = ?")
                        .bind(0, regionName)
                        .mapTo(Integer.class)
                        .findOne()
                        .orElseThrow(() -> new DatabaseException("No region found with the given code: " + regionName))
        );
    }

    @Override
    public List<FunctionDeployment> getDeploymentsWithImplementationId(long functionImplementationId) {
        return jdbiInstance.withHandle(handle ->
                handle.createQuery("SELECT * FROM functiondeployment WHERE functionImplementation_id = ? AND invocations > 0")
                        .bind(0, functionImplementationId)
                        .mapToBean(FunctionDeployment.class)
                        .list()
        );
    }

    @Override
    public List<FunctionDeployment> getDeploymentsWithImplementationIdAndMemorySize(long functionImplementationId, int memorySize) {
        return jdbiInstance.withHandle(handle ->
                handle.createQuery("SELECT * FROM functiondeployment WHERE functionImplementation_id = ? AND memorySize = ?")
                        .bind(0, functionImplementationId)
                        .bind(1, memorySize)
                        .mapToBean(FunctionDeployment.class)
                        .list()
        );
    }

    @Override
    public FunctionImplementation getImplementationById(long id) {
        return jdbiInstance.withHandle(handle ->
                handle.createQuery("SELECT * FROM functionimplementation WHERE id = ?")
                        .bind(0, id)
                        .mapToBean(FunctionImplementation.class)
                        .findOne()
                        .orElseThrow(() -> new DatabaseException("No functionimplementation found with the given id: " + id))
        );
    }

    @Override
    public Cpu getCpuByProvider(at.uibk.dps.util.Provider provider, int parallel, int percentage) {
        return jdbiInstance.withHandle(handle ->
                handle.createQuery("SELECT * FROM cpu WHERE provider = ? AND ? >= from_percentage AND ? < to_percentage AND parallel = ?")
                        .bind(0, getProviderEntry(provider).getId())
                        .bind(1, percentage)
                        .bind(2, percentage)
                        .bind(3, parallel)
                        .mapToBean(Cpu.class)
                        .findOne()
                        .orElseThrow(() -> new DatabaseException("No CPU found with the given specifications."))
        );
    }

    @Override
    public Cpu getCpuByProviderAndRegion(at.uibk.dps.util.Provider provider, String region, int parallel, int percentage) {
        return jdbiInstance.withHandle(handle ->
                handle.createQuery("SELECT * FROM cpu WHERE provider = ? AND region = ? AND ? >= from_percentage AND ? < to_percentage AND parallel = ?")
                        .bind(0, getProviderEntry(provider).getId())
                        .bind(1, getRegionEntry(region, provider).getId())
                        .bind(2, percentage)
                        .bind(3, percentage)
                        .bind(4, parallel)
                        .mapToBean(Cpu.class)
                        .findOne()
                        .orElseThrow(() -> new DatabaseException("No CPU found with the given specifications."))
        );
    }

    @Override
    public Pair<Integer, Integer> getServiceTypeInformation(String type) {
        Service service = jdbiInstance.withHandle(handle ->
                handle.createQuery("SELECT * FROM service WHERE type = ?")
                        .bind(0, type)
                        .mapToBean(Service.class)
                        .findOne()
                        .orElseThrow(() -> new DatabaseException("Could not fetch service type information from database."))
        );
        return Pair.of(service.getId(), service.getProviderID());
    }

    @Override
    public Pair<Double, Double> getServiceParamsFromDB(Integer typeId, Integer serviceRegionId) {
        ServiceDeployment serviceDeployment = jdbiInstance.withHandle(handle ->
                handle.createQuery("SELECT velocity, startup AS startup FROM serviceDeployment WHERE serviceID = ? AND regionID = ?")
                        .bind(0, typeId)
                        .bind(1, serviceRegionId)
                        .mapToBean(ServiceDeployment.class)
                        .findOne()
                        .orElseThrow(() -> new DatabaseException("Could not fetch service parameters from database."))
        );
        return Pair.of(serviceDeployment.getVelocity(), serviceDeployment.getStartup());
    }

    @Override
    public Triple<Double, Double, Double> getNetworkParamsFromDB(Integer lambdaRegionId, Integer serviceRegionId) {
        Networking networking = jdbiInstance.withHandle(handle ->
                handle.createQuery("SELECT * FROM networking WHERE sourceRegionID = ? AND destinationRegionID = ?")
                        .bind(0, lambdaRegionId)
                        .bind(1, serviceRegionId)
                        .mapToBean(Networking.class)
                        .findOne()
                        .orElseThrow(() -> new DatabaseException("Could not fetch network parameters from database for " + lambdaRegionId + "," + serviceRegionId))
        );

        double bandwidth = networking.getBandwidth() / 1000;
        double latency = networking.getLatency();

        if (serviceRegionId.equals(lambdaRegionId)) {
            return Triple.of(bandwidth, latency, latency);
        } else {
            networking = jdbiInstance.withHandle(handle ->
                    handle.createQuery("SELECT * FROM networking WHERE sourceRegionID = ? AND destinationRegionID = ?")
                            .bind(0, serviceRegionId)
                            .bind(1, serviceRegionId)
                            .mapToBean(Networking.class)
                            .findOne()
                            .orElseThrow(() -> new DatabaseException("Could not fetch network parameters from database for " + lambdaRegionId + "," + serviceRegionId))
            );

            return Triple.of(bandwidth, latency, networking.getLatency());
        }
    }

    @Override
    public Pair<Double, Double> getDataTransferParamsFromDB(String type, Integer lambdaRegionId, Integer serviceRegionId,
                                                                   Integer originalLambdaRegionId, boolean useOriginalLambdaRegion) {
        String dataTransferType;
        if (type.equals("FILE_DL") || type.equals("DT_REMOVE")) {
            dataTransferType = "download";
        } else if (type.equals("FILE_UP") || type.equals("UT_REMOVE")) {
            dataTransferType = "upload";
        } else {
            dataTransferType = null;
        }

        DataTransfer dataTransfer = jdbiInstance.withHandle(handle ->
                handle.createQuery("SELECT * FROM data_transfer WHERE type = ? AND functionRegionID = ? AND storageRegionID = ?")
                        .bind(0, dataTransferType)
                        .bind(1, useOriginalLambdaRegion && originalLambdaRegionId != -1 ? originalLambdaRegionId : lambdaRegionId)
                        .bind(2, serviceRegionId)
                        .mapToBean(DataTransfer.class)
                        .findOne()
                        .orElseThrow(() -> new DatabaseException("Could not fetch data transfer parameters from database."))
        );

        return Pair.of(dataTransfer.getBandwidth(), dataTransfer.getLatency());
    }
}
