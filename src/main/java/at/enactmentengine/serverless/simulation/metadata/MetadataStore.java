package at.enactmentengine.serverless.simulation.metadata;

import at.enactmentengine.serverless.simulation.metadata.model.*;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import java.util.List;

public class MetadataStore implements DataStore {

    private static MetadataStore INSTANCE;
    public static boolean USE_JSON_METADATA = false;
    public static boolean FORCE_DATABASE_PROVIDER = false;

    private final DataProvider dataProvider;

    public static synchronized MetadataStore get() {
        if (MetadataStore.INSTANCE == null) {
            MetadataStore.INSTANCE = new MetadataStore(ProviderSelector.getProvider(FORCE_DATABASE_PROVIDER));
        }
        return MetadataStore.INSTANCE;
    }

    private MetadataStore(DataProvider dataProvider) {
        this.dataProvider = dataProvider;
    }

    @Override
    public FunctionDeployment getFunctionIdEntry(String kmsArn) {
        return dataProvider.getFunctionIdEntry(kmsArn);
    }

    @Override
    public FunctionDeployment getDeploymentById(long id) {
        return dataProvider.getDeploymentById(id);
    }

    @Override
    public Provider getProviderEntry(at.uibk.dps.util.Provider providerEnum) {
        return dataProvider.getProviderEntry(providerEnum);
    }

    @Override
    public Region getRegionEntry(String region, at.uibk.dps.util.Provider providerEnum) {
        return dataProvider.getRegionEntry(region, providerEnum);
    }

    @Override
    public int getRegionId(String regionName) {
        return dataProvider.getRegionId(regionName);
    }

    @Override
    public List<FunctionDeployment> getDeploymentsWithImplementationId(long functionImplementationId) {
        return dataProvider.getDeploymentsWithImplementationId(functionImplementationId);
    }

    @Override
    public List<FunctionDeployment> getDeploymentsWithImplementationIdAndMemorySize(long functionImplementationId, int memorySize) {
        return dataProvider.getDeploymentsWithImplementationIdAndMemorySize(functionImplementationId, memorySize);
    }

    @Override
    public FunctionImplementation getImplementationById(long id) {
        return dataProvider.getImplementationById(id);
    }

    @Override
    public Cpu getCpuByProvider(at.uibk.dps.util.Provider provider, int parallel, int percentage) {
        return dataProvider.getCpuByProvider(provider, parallel, percentage);
    }

    @Override
    public Cpu getCpuByProviderAndRegion(at.uibk.dps.util.Provider provider, String region, int parallel, int percentage) {
        return dataProvider.getCpuByProviderAndRegion(provider, region, parallel, percentage);
    }

    @Override
    public Pair<Integer, Integer> getServiceTypeInformation(String type) {
        return dataProvider.getServiceTypeInformation(type);
    }

    @Override
    public Pair<Double, Double> getServiceParamsFromDB(Integer typeId, Integer serviceRegionId) {
        return dataProvider.getServiceParamsFromDB(typeId, serviceRegionId);
    }

    @Override
    public Triple<Double, Double, Double> getNetworkParamsFromDB(Integer lambdaRegionId, Integer serviceRegionId) {
        return dataProvider.getNetworkParamsFromDB(lambdaRegionId, serviceRegionId);
    }

    @Override
    public Pair<Double, Double> getDataTransferParamsFromDB(String type, Integer lambdaRegionId, Integer serviceRegionId, Integer originalLambdaRegionId, boolean useOriginalLambdaRegion) {
        return dataProvider.getDataTransferParamsFromDB(type, lambdaRegionId, serviceRegionId, originalLambdaRegionId, useOriginalLambdaRegion);
    }
}
