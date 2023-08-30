package at.enactmentengine.serverless.simulation.metadata;

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

public class MetadataStore {
    private static Jdbi jdbiInstance;
    private static final String PATH_TO_PROPERTIES = "mariaDatabase.properties";

    private static void createJdbiInstance() {
        Properties databaseFile = new Properties();
        try {
            databaseFile.load(Files.newInputStream(Paths.get(PATH_TO_PROPERTIES)));
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

    public static synchronized Jdbi getJdbiInstance() {
        if (jdbiInstance == null) {
            createJdbiInstance();
        }
        return jdbiInstance;
    }

    /**
     * Get the entry with the KMSArn (e.g. ARN) of the document in the functiondeployment table of the metadata DB.
     *
     * @param kmsArn the KMS_Arn of the functiondeployment
     *
     * @return the {@link FunctionDeployment}
     */
    public static FunctionDeployment getFunctionIdEntry(String kmsArn) {
        return getJdbiInstance().withHandle(handle ->
                handle.createQuery("SELECT * FROM functiondeployment WHERE KMS_Arn = ?")
                        .bind(0, kmsArn)
                        .mapToBean(FunctionDeployment.class)
                        .findOne()
                        .orElseThrow(() -> new DatabaseException("No functiondeployment found with the given id: " + kmsArn))
        );
    }

    /**
     * Get the entry with the id of the record in the functiondeployment table of the metadata DB.
     *
     * @param id the id of the functiondeployment entry
     *
     * @return the the {@link FunctionDeployment}
     */
    public static FunctionDeployment getDeploymentById(long id) {
        return getJdbiInstance().withHandle(handle ->
                handle.createQuery("SELECT * FROM functiondeployment WHERE id = ?")
                        .bind(0, id)
                        .mapToBean(FunctionDeployment.class)
                        .findOne()
                        .orElseThrow(() -> new DatabaseException("No functiondeployment found with the given id: " + id))
        );
    }

    /**
     * Gets the id from the metadata DB for the functiontype with the given name and type.
     *
     * @param functionImplementationId the id of the functionimplementation
     *
     * @return the id from the functiontype in the DB
     */
    private static int getFunctionTypeId(int functionImplementationId) {
        return getJdbiInstance().withHandle(handle ->
                handle.createQuery("SELECT functionType_id FROM functionimplementation WHERE id = ?")
                        .bind(0, functionImplementationId)
                        .mapTo(Integer.class)
                        .findOne()
                        .orElseThrow(() -> new DatabaseException("No functionimplementation found with the given id: " + functionImplementationId))
        );
    }

    /**
     * Gets the entry from the metadata DB for the given provider.
     *
     * @param providerEnum the provider to find
     *
     * @return the entry from the provider in the DB
     */
    public static Provider getProviderEntry(at.uibk.dps.util.Provider providerEnum) {
        return getJdbiInstance().withHandle(handle ->
                handle.createQuery("SELECT * FROM provider WHERE name = ?")
                        .bind(0, providerEnum.name())
                        .mapToBean(Provider.class)
                        .findOne()
                        .orElseThrow(() -> new DatabaseException("No provider found with the given name: " + providerEnum.name()))
        );
    }

    /**
     * Gets the entry from the metadata DB for the given region and provider.
     *
     * @param region       the region code
     * @param providerEnum the provider of the region
     *
     * @return the entry from the region in the DB
     */
    public static Region getRegionEntry(String region, at.uibk.dps.util.Provider providerEnum) {
        return getJdbiInstance().withHandle(handle ->
                handle.createQuery("SELECT * FROM region WHERE region = ? AND provider = ?")
                        .bind(0, region)
                        .bind(1, providerEnum.name())
                        .mapToBean(Region.class)
                        .findOne()
                        .orElseThrow(() -> new DatabaseException("No region found with the given code: " + region +
                                " for provider " + providerEnum.name()))
        );
    }

    /**
     * Fetches the regionId for a region with the specified name
     *
     * @param regionName name of the region to fetch the id for
     *
     * @return the corresponding regionId
     */
    public static int getRegionId(String regionName) {
        return getJdbiInstance().withHandle(handle ->
                handle.createQuery("SELECT id FROM region WHERE region = ?")
                        .bind(0, regionName)
                        .mapTo(Integer.class)
                        .findOne()
                        .orElseThrow(() -> new DatabaseException("No region found with the given code: " + regionName))
        );
    }

    /**
     * Gets all entries that have the given functionImplementationId.
     *
     * @param functionImplementationId the id of the functionimplementation
     *
     * @return the entries with the given functionImplementationId.
     */
    public static List<FunctionDeployment> getDeploymentsWithImplementationId(long functionImplementationId) {
        return getJdbiInstance().withHandle(handle ->
                handle.createQuery("SELECT * FROM functiondeployment WHERE functionImplementation_id = ? AND invocations > 0")
                        .bind(0, functionImplementationId)
                        .mapToBean(FunctionDeployment.class)
                        .list()
        );
    }

    /**
     * Gets all entries that have the given functionImplementationId and memorySize;
     *
     * @param functionImplementationId the id of the functionimplementation
     * @param memorySize               the memory size in MB
     *
     * @return the entries with the given functionImplementationId and memorySize
     */
    public static List<FunctionDeployment> getDeploymentsWithImplementationIdAndMemorySize(long functionImplementationId, int memorySize) {
        return getJdbiInstance().withHandle(handle ->
                handle.createQuery("SELECT * FROM functiondeployment WHERE functionImplementation_id = ? AND memorySize = ?")
                        .bind(0, functionImplementationId)
                        .bind(1, memorySize)
                        .mapToBean(FunctionDeployment.class)
                        .list()  // This will retrieve all matching rows as a List<FunctionDeployment>
        );
    }

    /**
     * Gets a functionImplementation entry with the given id.
     *
     * @param id the id of the functionimplementation
     *
     * @return the entry with the given id
     */
    public static FunctionImplementation getImplementationById(long id) {
        return getJdbiInstance().withHandle(handle ->
                handle.createQuery("SELECT * FROM functionimplementation WHERE id = ?")
                        .bind(0, id)
                        .mapToBean(FunctionImplementation.class)
                        .findOne()
                        .orElseThrow(() -> new DatabaseException("No functionimplementation found with the given id: " + id))
        );
    }

    /**
     * Gets a set of CPUs for the given provider.
     *
     * @param provider   the provider to get CPUs from
     * @param parallel   whether the CPU is for parallel use or not
     * @param percentage the probability of getting a CPU
     *
     * @return a CPU for the given provider
     */
    public static Cpu getCpuByProvider(at.uibk.dps.util.Provider provider, int parallel, int percentage) {
        return getJdbiInstance().withHandle(handle ->
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

    /**
     * Gets a set of CPUs for the given provider and region.
     *
     * @param provider   the provider to get CPUs from
     * @param region     the region code
     * @param parallel   whether the CPU is for parallel use or not
     * @param percentage the probability of getting a CPU
     *
     * @return a CPU for the given provider
     */
    public static Cpu getCpuByProviderAndRegion(at.uibk.dps.util.Provider provider, String region, int parallel, int percentage) {
        return getJdbiInstance().withHandle(handle ->
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

    /**
     * Fetches the service type information from the database.
     *
     * @return pair of (service-) typeId, providerId
     */
    public static Pair<Integer, Integer> getServiceTypeInformation(String type) {
        Service service = getJdbiInstance().withHandle(handle ->
                handle.createQuery("SELECT * FROM service WHERE type = ?")
                        .bind(0, type)
                        .mapToBean(Service.class)
                        .findOne()
                        .orElseThrow(() -> new DatabaseException("Could not fetch service type information from database."))
        );
        return Pair.of(service.getId(), service.getProviderID());
    }

    /**
     * Fetches the service parameters from the database.
     *
     * @return pair of relevant service parameters (velocity [ms/work], startup time [ms])
     */
    public static Pair<Double, Double> getServiceParamsFromDB(Integer typeId, Integer serviceRegionId) {
        ServiceDeployment serviceDeployment = getJdbiInstance().withHandle(handle ->
                handle.createQuery("SELECT velocity, startup AS startup FROM serviceDeployment WHERE serviceID = ? AND regionID = ?")
                        .bind(0, typeId)
                        .bind(1, serviceRegionId)
                        .mapToBean(ServiceDeployment.class)
                        .findOne()
                        .orElseThrow(() -> new DatabaseException("Could not fetch service parameters from database."))
        );
        return Pair.of(serviceDeployment.getVelocity(), serviceDeployment.getStartup());
    }

    /**
     * Fetches simulation relevant network parameters from the database.
     *
     * @return triple of bandwidth [Mb/ms], lambda latency [ms] and service latency [ms].
     */
    public static Triple<Double, Double, Double> getNetworkParamsFromDB(Integer lambdaRegionId, Integer serviceRegionId) {
        Networking networking = getJdbiInstance().withHandle(handle ->
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
            networking = getJdbiInstance().withHandle(handle ->
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

    /**
     * Fetches data transfer parameters from the DB.
     *
     * @param type the type of the data transfer, either "FILE_DL" or "FILE_UP"
     * @param useOriginalLambdaRegion if the original lambda region should be used or not
     *
     * @return pair of bandwidth [Mb/s] and latency [ms]
     */
    public static Pair<Double, Double> getDataTransferParamsFromDB(String type, Integer lambdaRegionId, Integer serviceRegionId,
                                                             Integer originalLambdaRegionId, boolean useOriginalLambdaRegion) {
        String dataTransferType;
        if (type.equals("FILE_DL") || type.equals("DT_REMOVE")) {
            dataTransferType = "download";
        } else if (type.equals("FILE_UP") || type.equals("UT_REMOVE")) {
            dataTransferType = "upload";
        } else {
            dataTransferType = null;
        }

        DataTransfer dataTransfer = getJdbiInstance().withHandle(handle ->
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
