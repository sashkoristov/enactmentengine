package at.enactmentengine.serverless.simulation;

import at.enactmentengine.serverless.exception.MissingComputationalWorkException;
import at.enactmentengine.serverless.exception.MissingSimulationParametersException;
import at.enactmentengine.serverless.exception.RegionDetectionException;
import at.enactmentengine.serverless.object.PairResult;
import at.enactmentengine.serverless.object.Utils;
import at.enactmentengine.serverless.simulation.metadata.MetadataStore;
import at.enactmentengine.serverless.simulation.metadata.model.Cpu;
import at.enactmentengine.serverless.simulation.metadata.model.FunctionDeployment;
import at.enactmentengine.serverless.simulation.metadata.model.FunctionImplementation;
import at.enactmentengine.serverless.simulation.metadata.model.Region;
import at.uibk.dps.databases.MariaDBAccess;
import at.uibk.dps.util.Provider;

import java.sql.SQLException;
import java.util.List;
import java.util.Random;

/**
 * Class that handles the simulation of the round trip time of a function based on various parameters.
 *
 * @author mikahautz
 */
public class SimulationModel {

    /**
     * The entry for the functionDeployment in the Metadata-DB.
     */
    private final FunctionDeployment functionDeployment;

    /**
     * The average RTT from the functionDeployment.
     */
    private long avgRTT;

    /**
     * The average loop counter from the functionDeployment.
     */
    private int avgLoopCounter;

    /**
     * The provider to simulate.
     */
    private Provider provider;

    /**
     * The region to simulate in.
     */
    private String region;

    /**
     * The memory to simulate.
     */
    private int memorySize;

    /**
     * The memory of the functionDeployment.
     */
    private int fdMemorySize;

    /**
     * The loopCounter of the simulationNode.
     */
    private int loopCounter;

    /**
     * Constructs the SimulationModel object.
     *
     * @param functionDeployment the database entry for the functionDeployment to simulate
     * @param provider           the provider to simulate
     * @param region             the region to simulate in
     * @param memorySize         the memorySize to simulate for
     * @param loopCounter        the current loopCounter of the function to simulate
     *
     * @throws SQLException if an error occurs when reading fields from the database entry
     */
    public SimulationModel(FunctionDeployment functionDeployment, Provider provider, String region, int memorySize, int loopCounter) throws SQLException {
        this.functionDeployment = functionDeployment;
        this.provider = provider;
        this.region = region;
        this.memorySize = memorySize;
        this.loopCounter = loopCounter;
        avgRTT = functionDeployment.getAvgRTT().longValue();
        avgLoopCounter = functionDeployment.getAvgLoopCounter();
        fdMemorySize = functionDeployment.getMemorySize();
    }

    /**
     * Applies normal distribution to the given execution time if the parameter success is true. If it is false, it
     * randomly multiplies the execution time with a value between 0 and 1.
     *
     * @param executionTime to apply the distribution on
     * @param success       whether the simulation is successful or not
     *
     * @return the execution time with the applied distribution
     */
    public static long applyDistribution(long executionTime, boolean success) {
        if (success && !SimulationParameters.NO_DISTRIBUTION) {
            // calculate the time as usual
            Random r = new Random();
            executionTime = (long) (r.nextGaussian() * (executionTime * 0.01) + executionTime);
        } else if (!success){
            // get a random double between 0 and 1
            Random random = new Random();
            executionTime *= random.nextDouble();
        }
        return executionTime;
    }

    /**
     * Subtracts the overheads from the RTT stored in the MD.
     *
     * @return the raw execution time of the function
     *
     * @throws SQLException                         if an error occurs when reading fields from the database entry
     * @throws RegionDetectionException             if detecting the region from the resource link fails
     * @throws MissingSimulationParametersException if not all required fields are filled in in the database
     */
    private long getRawExecutionTime() throws SQLException, MissingSimulationParametersException, RegionDetectionException {
        // if the field 'avgRuntime' has a value set, simply use it
        double avgRuntime = functionDeployment.getAvgRuntime();
        if (avgRuntime > 1) {
            return (long) avgRuntime;
        }

        String functionId = functionDeployment.getKmsArn();
        Provider mdProvider = Utils.detectProvider(functionId);
        String mdRegion = Utils.detectRegion(functionId);

        at.enactmentengine.serverless.simulation.metadata.model.Provider mdProviderEntry = MetadataStore.getProviderEntry(mdProvider);
        Region mdRegionEntry = MetadataStore.getRegionEntry(mdRegion, mdProvider);

        int faasOverhead = mdProviderEntry.getFaasSystemOverheadms();
        int cryptoOverhead = mdProviderEntry.getCryptoOverheadms();
        int networkOverhead = mdRegionEntry.getNetworkOverheadms().intValue();
        int concurrencyOverhead = mdProviderEntry.getConcurrencyOverheadMs();
        int handshake = 0;
        int authenticationOverhead = 0;

        if (avgLoopCounter != 0 && concurrencyOverhead != 0) {
            concurrencyOverhead *= avgLoopCounter;
        }

        if (mdProvider == Provider.AWS || mdProvider == Provider.IBM) {
            if (mdProvider == Provider.AWS) {
                handshake = 3;
            } else {
                handshake = 2;
            }

            if (cryptoOverhead != 0 && networkOverhead != 0) {
                authenticationOverhead = cryptoOverhead + handshake * networkOverhead;
            } else {
                throw new MissingSimulationParametersException("Some fields in the metadata database are not filled in yet." +
                        "Please make sure that for the provider " + provider.toString() + " the field " +
                        "'cryptoOverheadms' and for the region " + region + " the field 'networkOverheadms' is filled in correctly.");
            }
        }

        if (faasOverhead != 0 && networkOverhead != 0) {
            return Math.max(avgRTT - networkOverhead - faasOverhead - authenticationOverhead - concurrencyOverhead, 0);
        } else {
            throw new MissingSimulationParametersException("Some fields in the metadata database are not filled in yet. " +
                    "Please make sure that for the provider " + provider.toString() + " the field 'faasSystemOverheadms' " +
                    "and for the region " + region + " the field 'networkOverheadms' is filled in correctly.");
        }
    }

    /**
     * Adds the required overheads to the given execution time to get the final round-trip time.
     *
     * @param executionTime to add the overheads to
     *
     * @return the overall round-trip time
     *
     * @throws SQLException                         if an error occurs when reading fields from the database entry
     * @throws MissingSimulationParametersException if not all required fields are filled in in the database
     */
    private long addOverheads(long executionTime) throws SQLException, MissingSimulationParametersException {
        // O = xcs · CSO + NO + xa · AO + F O + CO

        at.enactmentengine.serverless.simulation.metadata.model.Provider providerEntry = MetadataStore.getProviderEntry(provider);
        Region regionEntry = MetadataStore.getRegionEntry(region, provider);

        int faasOverhead = providerEntry.getFaasSystemOverheadms();
        int cryptoOverhead = providerEntry.getCryptoOverheadms();
        int networkOverhead = regionEntry.getNetworkOverheadms().intValue();
        int concurrencyOverhead = providerEntry.getConcurrencyOverheadMs();

        if (faasOverhead != 0 && cryptoOverhead != 0 && networkOverhead != 0) {
            long rtt = executionTime + networkOverhead + faasOverhead;
            // add the concurrencyOverhead depending on the loopCounter
            if (loopCounter != -1 && concurrencyOverhead != 0) {
                rtt += (long) loopCounter * concurrencyOverhead;
            }

            int handshake = 0;
            if (provider == Provider.AWS || provider == Provider.IBM) {
                if (provider == Provider.AWS) {
                    handshake = 3;
                } else {
                    handshake = 2;
                }

                int authenticationOverhead = cryptoOverhead + handshake * networkOverhead;
                // if authentication is required, add it to the RTT
                rtt += authenticationOverhead;
            }

            return rtt;
        } else {
            throw new MissingSimulationParametersException("Some fields in the metadata database are not filled in yet. " +
                    "Please make sure that for the provider " + provider.toString() + " the fields 'faasSystemOverheadms' and " +
                    "'cryptoOverheadms' and for the region " + region + " the field 'networkOverheadms' is filled in correctly.");
        }
    }

    /**
     * Estimates the execution time based on the computational work and memory size.
     *
     * @return the estimated execution time
     *
     * @throws SQLException                      if an error occurs when reading fields from the database entry
     * @throws MissingComputationalWorkException when the field computationWork for the functionImplementation is not
     *                                           filled
     */
    private long estimateExecutionTime() throws SQLException, MissingComputationalWorkException {
        long implementationId = functionDeployment.getFunctionImplementationId();
        FunctionImplementation implementation = MetadataStore.getImplementationById(implementationId);
        double instructions = implementation.getComputationWork();
        if (instructions == 0) {
            throw new MissingComputationalWorkException("No computational work is given for the functionImplementation " +
                    "with the id " + implementationId + ". Therefore simulating different memory sizes is not possible.");
        }
        List<FunctionDeployment> sameMemoryDeployment = MetadataStore.getDeploymentsWithImplementationIdAndMemorySize(implementationId, memorySize);
        double speedup = 0;
        if (sameMemoryDeployment != null && !sameMemoryDeployment.isEmpty()) {
            speedup = sameMemoryDeployment.get(0).getSpeedup();
        }
        /* The speedup is always measured against the deployment with 128mb ram. If it is NULL, it is assumed
         there is linear speedup relative to 128mb. (e.g. 256mb -> 2, 512mb -> 4, 1024mb -> 8, etc */
        if (speedup == 0) {
            speedup = memorySize / 128.0;
        }
        // get a random double between 0 and 1
        Random random = new Random();
        int randomValue = (int) (random.nextDouble() * 100);
        int parallel = loopCounter == -1 ? 0 : 1;
        Cpu cpu = null;

        switch (provider) {
            case AWS:
                cpu = MetadataStore.getCpuByProvider(provider, parallel, randomValue);
                break;
            case GOOGLE:
                at.enactmentengine.serverless.simulation.metadata.model.Provider providerEntry = MetadataStore.getProviderEntry(provider);
                int maxConcurrency = providerEntry.getMaxConcurrency();
                // if the loopCounter is smaller than the concurrency limit, use the sequential CPU
                if (loopCounter < maxConcurrency) {
                    parallel = 0;
                }
                cpu = MetadataStore.getCpuByProvider(provider, parallel, randomValue);
                break;
            case IBM:
                cpu = MetadataStore.getCpuByProviderAndRegion(provider, region, parallel, randomValue);
                break;
            default:
                break;
        }
        double mips = cpu.getMips();
        double runtimeInSeconds = instructions / mips / speedup;
        return (long) (runtimeInSeconds * 1000);
    }

    /**
     * Simulates the round trip time based on the region and memory size.
     *
     * @param success whether the simulation is successful or not
     *
     * @return a PairResult consisting of the round trip time and the cost
     *
     * @throws SQLException                         if an error occurs when reading fields from the database entry
     * @throws RegionDetectionException             if detecting the region from the resource link fails
     * @throws MissingComputationalWorkException    when the field computationWork for the functionImplementation is not
     *                                              filled
     * @throws MissingSimulationParametersException if not all required fields are filled in in the database
     */
    public PairResult<Long, Double> simulateRoundTripTime(boolean success) throws SQLException, RegionDetectionException,
            MissingComputationalWorkException, MissingSimulationParametersException {
        long executionTime;

        if (memorySize == fdMemorySize) {
            executionTime = getRawExecutionTime();
        } else {
            executionTime = estimateExecutionTime();
        }

        executionTime = applyDistribution(executionTime, success);
        double cost = MariaDBAccess.calculateCost(memorySize, executionTime, provider);
        SimulationParameters.workflowCost += cost;
        long rtt = addOverheads(executionTime);

        return new PairResult<>(rtt, cost);
    }

}
