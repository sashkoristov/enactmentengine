package at.enactmentengine.serverless.simulation.metadata.cache;

import at.enactmentengine.serverless.simulation.metadata.DataProvider;
import at.enactmentengine.serverless.simulation.metadata.cache.filestorage.FileStorageMetaDataProvider;
import at.enactmentengine.serverless.simulation.metadata.exceptions.DatabaseException;
import at.enactmentengine.serverless.simulation.metadata.model.*;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class JsonProvider implements DataProvider {
    private static JsonProvider INSTANCE;

    private final List<FunctionImplementation> functionImplementations;
    private final List<FunctionDeployment> functionDeployments;
    private final List<Region> regions;
    private final List<Provider> providers;
    private final List<Cpu> cpus;
    private final List<DataTransfer> dataTransfers;
    private final List<Networking> networkings;
    private final List<Service> services;
    private final List<ServiceDeployment> serviceDeployments;

    private final Map<Long, FunctionDeployment> deploymentsById;

    private final Map<String, FunctionDeployment> deploymentsByResourceLink;

    private final Map<Long, FunctionImplementation> implementationsById;

    private final Map<String, Provider> providersByName;

    private final Map<String, Integer> regionIdByRegionName;

    private final Map<String, Service> servicesByType;

    public static synchronized JsonProvider get() {
        if (JsonProvider.INSTANCE == null) {
            JsonProvider.INSTANCE = new JsonProvider();
        }
        return JsonProvider.INSTANCE;
    }

    private JsonProvider() {
        try (final FileStorageMetaDataProvider metadata = FileStorageMetaDataProvider.get()) {
            this.functionImplementations = metadata.functionImplementationDao().getAll();
            this.functionDeployments = metadata.functionDeploymentDao().getAll();
            this.regions = metadata.regionDao().getAll();
            this.providers = metadata.providerDao().getAll();
            this.cpus = metadata.cpuDao().getAll();
            this.dataTransfers = metadata.dataTransferDao().getAll();
            this.networkings = metadata.networkingDao().getAll();
            this.services = metadata.serviceDao().getAll();
            this.serviceDeployments = metadata.serviceDeploymentDao().getAll();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        this.deploymentsById = this.functionDeployments.stream()
                .collect(Collectors.toMap(FunctionDeployment::getId, f -> f));

        this.deploymentsByResourceLink = this.functionDeployments.stream()
                .collect(Collectors.toMap(FunctionDeployment::getKmsArn, f -> f));

        this.implementationsById = this.functionImplementations.stream()
                .collect(Collectors.toMap(FunctionImplementation::getId, f -> f));

        this.providersByName = this.providers.stream()
                .collect(Collectors.toMap(Provider::getName, p -> p));

        this.regionIdByRegionName = this.regions.stream()
                .collect(Collectors.toMap(Region::getRegion, Region::getId));

        this.servicesByType = this.services.stream()
                .collect(Collectors.toMap(Service::getType, s -> s));
    }

    @Override
    public FunctionDeployment getFunctionIdEntry(String kmsArn) {
        return this.deploymentsByResourceLink.get(kmsArn);
    }

    @Override
    public FunctionDeployment getDeploymentById(long id) {
        return this.deploymentsById.get(id);
    }

    @Override
    public Provider getProviderEntry(at.uibk.dps.util.Provider providerEnum) {
        return this.providersByName.get(providerEnum.name());
    }

    @Override
    public Region getRegionEntry(String region, at.uibk.dps.util.Provider providerEnum) {
        return this.regions.stream()
                .filter(r -> r.getProvider() == providerEnum && r.getRegion().equals(region))
                .findFirst()
                .orElseThrow(() -> new DatabaseException("No region found with the given code: " + region +
                        " for provider " + providerEnum.name()));
    }

    @Override
    public int getRegionId(String regionName) {
        return this.regionIdByRegionName.get(regionName);
    }

    @Override
    public List<FunctionDeployment> getDeploymentsWithImplementationId(long functionImplementationId) {
        return this.functionDeployments.stream()
                .filter(fd -> fd.getFunctionImplementationId().equals(functionImplementationId) && fd.getInvocations() > 0)
                .collect(Collectors.toList());
    }

    @Override
    public List<FunctionDeployment> getDeploymentsWithImplementationIdAndMemorySize(long functionImplementationId, int memorySize) {
        return this.functionDeployments.stream()
                .filter(fd -> fd.getFunctionImplementationId().equals(functionImplementationId) && fd.getMemorySize() == memorySize)
                .collect(Collectors.toList());
    }

    @Override
    public FunctionImplementation getImplementationById(long id) {
        return this.implementationsById.get(id);
    }

    @Override
    public Cpu getCpuByProvider(at.uibk.dps.util.Provider provider, int parallel, int percentage) {
        return this.cpus.stream()
                .filter(c -> c.getProvider() == getProviderEntry(provider).getId().longValue() && parallel == c.getParallel() &&
                        percentage >= c.getFrom_percentage() && percentage < c.getTo_percentage())
                .findFirst()
                .orElseThrow(() -> new DatabaseException("No CPU found with the given specifications."));
    }

    @Override
    public Cpu getCpuByProviderAndRegion(at.uibk.dps.util.Provider provider, String region, int parallel, int percentage) {
        return this.cpus.stream()
                .filter(c -> c.getProvider() == getProviderEntry(provider).getId().longValue() && c.getRegion() == getRegionId(region) &&
                        percentage >= c.getFrom_percentage() && percentage < c.getTo_percentage() && parallel == c.getParallel())
                .findFirst()
                .orElseThrow(() -> new DatabaseException("No CPU found with the given specifications."));
    }

    @Override
    public Pair<Integer, Integer> getServiceTypeInformation(String type) {
        Service service = this.servicesByType.get(type);
        return Pair.of(service.getId(), service.getProviderID());
    }

    @Override
    public Pair<Double, Double> getServiceParamsFromDB(Integer typeId, Integer serviceRegionId) {
        ServiceDeployment serviceDeployment = this.serviceDeployments.stream()
                .filter(s -> s.getServiceID().equals(typeId) && s.getRegionID().equals(serviceRegionId))
                .findFirst()
                .orElseThrow(() -> new DatabaseException("Could not fetch service parameters from JSON files."));
        return Pair.of(serviceDeployment.getVelocity(), serviceDeployment.getStartup());
    }

    @Override
    public Triple<Double, Double, Double> getNetworkParamsFromDB(Integer lambdaRegionId, Integer serviceRegionId) {
        Networking networking = findNetworkingByRegionIds(lambdaRegionId, serviceRegionId);

        double bandwidth = networking.getBandwidth() / 1000;
        double latency = networking.getLatency();

        if (serviceRegionId.equals(lambdaRegionId)) {
            return Triple.of(bandwidth, latency, latency);
        } else {
            networking = findNetworkingByRegionIds(serviceRegionId, serviceRegionId);
            return Triple.of(bandwidth, latency, networking.getLatency());
        }
    }

    private Networking findNetworkingByRegionIds(Integer sourceId, Integer destinationId) {
        return this.networkings.stream()
                .filter(n -> n.getSourceRegionID().equals(sourceId) && n.getDestinationRegionID().equals(destinationId))
                .findFirst()
                .orElseThrow(() -> new DatabaseException(
                        "Could not fetch network parameters from JSON file for " + sourceId + "," + destinationId
                ));
    }

    @Override
    public Pair<Double, Double> getDataTransferParamsFromDB(String type, Integer lambdaRegionId, Integer serviceRegionId,
                                                            Integer originalLambdaRegionId, boolean useOriginalLambdaRegion) {
        String dataTransferType = determineDataTransferType(type);
        Integer functionRegionId = useOriginalLambdaRegion && originalLambdaRegionId != -1 ? originalLambdaRegionId : lambdaRegionId;

        DataTransfer dataTransfer = this.dataTransfers.stream()
                .filter(d -> d.getType().equals(dataTransferType) && d.getFunctionRegionID().equals(functionRegionId) &&
                        d.getStorageRegionID().equals(serviceRegionId))
                .findFirst()
                .orElseThrow(() -> new DatabaseException("Could not fetch data transfer parameters from Json file."));

        return Pair.of(dataTransfer.getBandwidth(), dataTransfer.getLatency());
    }

    private String determineDataTransferType(String type) {
        if (type.equals("FILE_DL") || type.equals("DT_REMOVE")) {
            return "download";
        } else if (type.equals("FILE_UP") || type.equals("UT_REMOVE")) {
            return "upload";
        } else {
            return null;
        }
    }
}
