package at.enactmentengine.serverless.Simulation;

import at.enactmentengine.serverless.exception.MissingComputationalWorkException;
import at.enactmentengine.serverless.exception.MissingSimulationParametersException;
import at.enactmentengine.serverless.exception.RegionDetectionException;
import at.enactmentengine.serverless.object.PairResult;
import at.enactmentengine.serverless.object.Utils;
import at.uibk.dps.databases.MariaDBAccess;
import at.uibk.dps.util.Provider;

import java.sql.ResultSet;
import java.sql.SQLException;
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
    private ResultSet functionDeployment;

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
    public SimulationModel(ResultSet functionDeployment, Provider provider, String region, int memorySize, int loopCounter) throws SQLException {
        this.functionDeployment = functionDeployment;
        this.provider = provider;
        this.region = region;
        this.memorySize = memorySize;
        this.loopCounter = loopCounter;
        avgRTT = (long) functionDeployment.getDouble("avgRTT");
        avgLoopCounter = functionDeployment.getInt("avgLoopCounter");
        fdMemorySize = functionDeployment.getInt("memorySize");
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
        if (success) {
            // calculate the time as usual
            Random r = new Random();
            executionTime = (long) (r.nextGaussian() * (executionTime * 0.01) + executionTime);
        } else {
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
        double avgRuntime = functionDeployment.getDouble("avgRuntime");
        if (avgRuntime > 1) {
            return (long) avgRuntime;
        }

        // TODO add x_cs, CSO, x_a, CO
        String functionId = functionDeployment.getString("KMS_Arn");
        Provider mdProvider = Utils.detectProvider(functionId);
        String mdRegion = Utils.detectRegion(functionId);

        ResultSet mdProviderEntry = MariaDBAccess.getProviderEntry(mdProvider);
        ResultSet mdRegionEntry = MariaDBAccess.getRegionEntry(mdRegion, mdProvider);
        mdProviderEntry.next();
        mdRegionEntry.next();

        int faasOverhead = mdProviderEntry.getInt("faasSystemOverheadms");
        int cryptoOverhead = mdProviderEntry.getInt("cryptoOverheadms");
        int networkOverhead = mdRegionEntry.getInt("networkOverheadms");
        int concurrencyOverhead = mdProviderEntry.getInt("concurrencyOverheadms");
        int handshake = 0;
        int authenticationOverhead = 0;

        if (avgLoopCounter != 0 && concurrencyOverhead != 0) {
            concurrencyOverhead *= avgLoopCounter;
        }

        if (mdProvider == Provider.AWS) {
            handshake = 2;

            if (cryptoOverhead != 0 && networkOverhead != 0) {
                authenticationOverhead = cryptoOverhead + handshake * networkOverhead;
            } else {
                throw new MissingSimulationParametersException("Some fields in the metadata database are not filled in yet." +
                        "Please make sure that for the provider " + provider.toString() + " the field " +
                        "'cryptoOverheadms' and for the region " + region + " the field 'networkOverheadms' is filled in correctly.");
            }
        } else if (mdProvider == Provider.GOOGLE) {
            // TODO
            handshake = 1;
        } else if (mdProvider == Provider.IBM) {
            // TODO
        }

        if (faasOverhead != 0 && networkOverhead != 0) {
            return avgRTT - networkOverhead - faasOverhead - authenticationOverhead - concurrencyOverhead;
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

        ResultSet providerEntry = MariaDBAccess.getProviderEntry(provider);
        ResultSet regionEntry = MariaDBAccess.getRegionEntry(region, provider);
        providerEntry.next();
        regionEntry.next();

        int faasOverhead = providerEntry.getInt("faasSystemOverheadms");
        int cryptoOverhead = providerEntry.getInt("cryptoOverheadms");
        int networkOverhead = regionEntry.getInt("networkOverheadms");
        int concurrencyOverhead = providerEntry.getInt("concurrencyOverheadms");

        if (faasOverhead != 0 && cryptoOverhead != 0 && networkOverhead != 0) {
            long rtt = executionTime + networkOverhead + faasOverhead;
            // add the concurrencyOverhead depending on the loopCounter
            if (loopCounter != -1 && concurrencyOverhead != 0) {
                rtt += (long) loopCounter * concurrencyOverhead;
            }

            int handshake = 0;
            // TODO other providers?
            if (provider == Provider.AWS) {
                handshake = 2;

                int authenticationOverhead = cryptoOverhead + handshake * networkOverhead;
                // if authentication is required, add it to the RTT
                rtt += authenticationOverhead;
            }
//            } else if (provider == Provider.GOOGLE) {
//                handshake = 1;
//            }

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
        int implementationId = functionDeployment.getInt("functionImplementation_id");
        ResultSet implementation = MariaDBAccess.getImplementationById(implementationId);
        implementation.next();
        double instructions = implementation.getDouble("computationWork");
        if (instructions == 0) {
            throw new MissingComputationalWorkException("No computational work is given for the functionImplementation " +
                    "with the id " + implementationId + ". Therefore simulating different memory sizes is not possible.");
        }
        ResultSet sameMemoryDeployment = MariaDBAccess.getDeploymentsWithImplementationIdAndMemorySize(implementationId, memorySize);
        double speedup = 0;
        if (sameMemoryDeployment.next()) {
            speedup = sameMemoryDeployment.getDouble("speedup");
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
        ResultSet cpu = null;

        switch (provider) {
            case AWS:
                cpu = MariaDBAccess.getCpuByProvider(provider, parallel, randomValue);
                cpu.next();
                break;
            case GOOGLE:
                ResultSet providerEntry = MariaDBAccess.getProviderEntry(provider);
                providerEntry.next();
                int maxConcurrency = providerEntry.getInt("maxConcurrency");
                // if the loopCounter is smaller than the concurrency limit, use the sequential CPU
                if (loopCounter < maxConcurrency) {
                    parallel = 0;
                }
                cpu = MariaDBAccess.getCpuByProvider(provider, parallel, randomValue);
                cpu.next();
                break;
            case IBM:
                cpu = MariaDBAccess.getCpuByProviderAndRegion(provider, region, parallel, randomValue);
                cpu.next();
                break;
            default:
                break;
        }
        double mips = cpu.getDouble("MIPS");
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
