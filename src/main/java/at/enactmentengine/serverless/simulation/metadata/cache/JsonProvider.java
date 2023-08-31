package at.enactmentengine.serverless.simulation.metadata.cache;

import at.enactmentengine.serverless.simulation.metadata.DataProvider;
import at.enactmentengine.serverless.simulation.metadata.model.*;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import java.util.List;

public class JsonProvider implements DataProvider {
    private static JsonProvider INSTANCE;

    public static synchronized JsonProvider get() {
        if (JsonProvider.INSTANCE == null) {
            JsonProvider.INSTANCE = new JsonProvider();
        }
        return JsonProvider.INSTANCE;
    }

    private JsonProvider() {

    }

    @Override
    public FunctionDeployment getFunctionIdEntry(String kmsArn) {
        return null;
    }

    @Override
    public FunctionDeployment getDeploymentById(long id) {
        return null;
    }

    @Override
    public Provider getProviderEntry(at.uibk.dps.util.Provider providerEnum) {
        return null;
    }

    @Override
    public Region getRegionEntry(String region, at.uibk.dps.util.Provider providerEnum) {
        return null;
    }

    @Override
    public int getRegionId(String regionName) {
        return 0;
    }

    @Override
    public List<FunctionDeployment> getDeploymentsWithImplementationId(long functionImplementationId) {
        return null;
    }

    @Override
    public List<FunctionDeployment> getDeploymentsWithImplementationIdAndMemorySize(long functionImplementationId, int memorySize) {
        return null;
    }

    @Override
    public FunctionImplementation getImplementationById(long id) {
        return null;
    }

    @Override
    public Cpu getCpuByProvider(at.uibk.dps.util.Provider provider, int parallel, int percentage) {
        return null;
    }

    @Override
    public Cpu getCpuByProviderAndRegion(at.uibk.dps.util.Provider provider, String region, int parallel, int percentage) {
        return null;
    }

    @Override
    public Pair<Integer, Integer> getServiceTypeInformation(String type) {
        return null;
    }

    @Override
    public Pair<Double, Double> getServiceParamsFromDB(Integer typeId, Integer serviceRegionId) {
        return null;
    }

    @Override
    public Triple<Double, Double, Double> getNetworkParamsFromDB(Integer lambdaRegionId, Integer serviceRegionId) {
        return null;
    }

    @Override
    public Pair<Double, Double> getDataTransferParamsFromDB(String type, Integer lambdaRegionId, Integer serviceRegionId, Integer originalLambdaRegionId, boolean useOriginalLambdaRegion) {
        return null;
    }
}
