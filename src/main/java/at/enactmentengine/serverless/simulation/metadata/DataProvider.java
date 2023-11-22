package at.enactmentengine.serverless.simulation.metadata;

import at.enactmentengine.serverless.simulation.metadata.model.*;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import java.util.List;

public interface DataProvider {
    /**
     * Get the entry with the KMSArn (e.g. ARN) of the document in the functiondeployment table of the metadata DB.
     *
     * @param kmsArn the KMS_Arn of the functiondeployment
     *
     * @return the {@link FunctionDeployment}
     */
    FunctionDeployment getFunctionIdEntry(String kmsArn);

    /**
     * Get the entry with the id of the record in the functiondeployment table of the metadata DB.
     *
     * @param id the id of the functiondeployment entry
     *
     * @return the the {@link FunctionDeployment}
     */
    FunctionDeployment getDeploymentById(long id);

    /**
     * Gets the entry from the metadata DB for the given provider.
     *
     * @param providerEnum the provider to find
     *
     * @return the entry from the provider in the DB
     */
    Provider getProviderEntry(at.uibk.dps.util.Provider providerEnum);

    /**
     * Gets the entry from the metadata DB for the given region and provider.
     *
     * @param region       the region code
     * @param providerEnum the provider of the region
     *
     * @return the entry from the region in the DB
     */
    Region getRegionEntry(String region, at.uibk.dps.util.Provider providerEnum);

    /**
     * Fetches the regionId for a region with the specified name
     *
     * @param regionName name of the region to fetch the id for
     *
     * @return the corresponding regionId
     */
    int getRegionId(String regionName);

    /**
     * Gets all entries that have the given functionImplementationId.
     *
     * @param functionImplementationId the id of the functionimplementation
     *
     * @return the entries with the given functionImplementationId.
     */
    List<FunctionDeployment> getDeploymentsWithImplementationId(long functionImplementationId);

    /**
     * Gets all entries that have the given functionImplementationId and memorySize;
     *
     * @param functionImplementationId the id of the functionimplementation
     * @param memorySize               the memory size in MB
     *
     * @return the entries with the given functionImplementationId and memorySize
     */
    List<FunctionDeployment> getDeploymentsWithImplementationIdAndMemorySize(long functionImplementationId, int memorySize);

    /**
     * Gets a functionImplementation entry with the given id.
     *
     * @param id the id of the functionimplementation
     *
     * @return the entry with the given id
     */
    FunctionImplementation getImplementationById(long id);

    /**
     * Gets a set of CPUs for the given provider.
     *
     * @param provider   the provider to get CPUs from
     * @param parallel   whether the CPU is for parallel use or not
     * @param percentage the probability of getting a CPU
     *
     * @return a CPU for the given provider
     */
    Cpu getCpuByProvider(at.uibk.dps.util.Provider provider, int parallel, int percentage);

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
    Cpu getCpuByProviderAndRegion(at.uibk.dps.util.Provider provider, String region, int parallel, int percentage);

    /**
     * Fetches the service type information from the database.
     *
     * @return pair of (service-) typeId, providerId
     */
    Pair<Integer, Integer> getServiceTypeInformation(String type);

    /**
     * Fetches the service parameters from the database.
     *
     * @param typeId          the id of the service type
     * @param serviceRegionId the id of the service region
     *
     * @return pair of relevant service parameters (velocity [ms/work], startup time [ms])
     */
    Pair<Double, Double> getServiceParamsFromDB(Integer typeId, Integer serviceRegionId);

    /**
     * Fetches simulation relevant network parameters from the database.
     *
     * @param lambdaRegionId  the id of the lambda region
     * @param serviceRegionId the id of the service region
     *
     * @return triple of bandwidth [Mb/ms], lambda latency [ms] and service latency [ms].
     */

    Triple<Double, Double, Double> getNetworkParamsFromDB(Integer lambdaRegionId, Integer serviceRegionId);

    /**
     * Fetches data transfer parameters from the DB.
     *
     * @param type                    the type of the data transfer, either "FILE_DL" or "FILE_UP"
     * @param lambdaRegionId          the id of the lambda region
     * @param serviceRegionId         the id of the service region
     * @param originalLambdaRegionId  the id of the original lambda region
     * @param useOriginalLambdaRegion if the original lambda region should be used or not
     *
     * @return pair of bandwidth [Mb/s] and latency [ms]
     */

    Pair<Double, Double> getDataTransferParamsFromDB(String type, Integer lambdaRegionId, Integer serviceRegionId,
                                                     Integer originalLambdaRegionId, boolean useOriginalLambdaRegion);

}
