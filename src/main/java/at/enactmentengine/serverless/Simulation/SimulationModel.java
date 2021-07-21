package at.enactmentengine.serverless.Simulation;

import at.enactmentengine.serverless.exception.MissingComputationalWorkException;
import at.enactmentengine.serverless.exception.MissingSimulationParametersException;
import at.enactmentengine.serverless.object.PairResult;
import at.uibk.dps.databases.MariaDBAccess;
import at.uibk.dps.exceptions.RegionDetectionException;
import at.uibk.dps.util.Provider;
import at.uibk.dps.util.Utils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Random;

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

    public SimulationModel(ResultSet functionDeployment, Provider provider, String region, int memorySize, int loopCounter) throws SQLException {
        this.functionDeployment = functionDeployment;
        this.provider = provider;
        this.region = region;
        this.memorySize = memorySize;
        this.loopCounter = loopCounter;
        // TODO get other fields??
        avgRTT = (long) functionDeployment.getDouble("avgRTT");
        avgLoopCounter = functionDeployment.getInt("avgLoopCounter");
        fdMemorySize = functionDeployment.getInt("memorySize");
    }

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
     */
    private long getRawExecutionTime() throws SQLException, RegionDetectionException, MissingSimulationParametersException {
        // if the field 'avgRuntime' has a value set, simply use it
        double avgRuntime = functionDeployment.getDouble("avgRuntime");
        if (avgRuntime > 1) {
            return (long) avgRuntime;
        }

        // TODO add x_cs, CSO, x_a, CO
        Provider mdProvider = null;
        String mdRegion = null;
        String functionId = functionDeployment.getString("KMS_Arn");
        mdProvider = Utils.detectProvider(functionId);
        mdRegion = Utils.detectRegion(functionId);

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

        // TODO other providers?
        if (mdProvider == Provider.AWS) {
            handshake = 2;

            if (faasOverhead != 0 && cryptoOverhead != 0 && networkOverhead != 0) {
                authenticationOverhead = cryptoOverhead + handshake * networkOverhead;
            } else {
                throw new MissingSimulationParametersException("Some fields in the metadata database are not filled in yet." +
                        "Please make sure that for the provider " + provider.toString() + " the fields 'faasSystemOverheadms' and " +
                        "'cryptoOverheadms' and for the region " + region + " the field 'networkOverheadms' is filled in correctly.");
            }
        } else if (mdProvider == Provider.GOOGLE) {
            handshake = 1;
        }

        if (faasOverhead != 0 && networkOverhead != 0) {
            // TODO add x_cs, CSO, x_a
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
     */
    private long addOverheads(long executionTime) throws SQLException, MissingSimulationParametersException {
        // O = xcs · CSO + NO + xa · AO + F O + CO
        // TODO add x_cs, CSO, x_a, CO

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
            //TODO add x_cs, CSO, x_a, CO
            //TODO check if the current function is invoked the first time or if new ones have to be created in parallelFor

            //TODO authenticate for the first function of a provider (or always for AWS and IBM and never for google??)
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

    public PairResult<Long, Double> simulateRoundTripTime(boolean success) throws SQLException, RegionDetectionException, MissingComputationalWorkException, MissingSimulationParametersException {
        /*
        read from MD-DB the provider and region for the FD and the desired simulation
            from provider get faasSystemOverheadms and cryptoOverheadms
            from region get NO
        from avgRTT subtract the values as in formula
        to remainder add the values as in formula
         */
        long rtt = 0;
        double cost = 0;
        long executionTime = 0;

        if (memorySize == fdMemorySize) {
            executionTime = getRawExecutionTime();
        } else {
            executionTime = estimateExecutionTime();
        }
        executionTime = applyDistribution(executionTime, success);
        cost = MariaDBAccess.calculateCost(memorySize, executionTime, provider);
        SimulationParameters.workflowCost += cost;
        rtt = addOverheads(executionTime);

        return new PairResult<>(rtt, cost);
    }

    public ResultSet getFunctionDeployment() {
        return functionDeployment;
    }

    public void setFunctionDeployment(ResultSet functionDeployment) {
        functionDeployment = functionDeployment;
    }

    public long getAvgRTT() {
        return avgRTT;
    }

    public void setAvgRTT(long avgRTT) {
        this.avgRTT = avgRTT;
    }

    public int getAvgLoopCounter() {
        return avgLoopCounter;
    }

    public void setAvgLoopCounter(int avgLoopCounter) {
        this.avgLoopCounter = avgLoopCounter;
    }

    public Provider getProvider() {
        return provider;
    }

    public void setProvider(Provider provider) {
        provider = provider;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        region = region;
    }

    public int getMemorySize() {
        return memorySize;
    }

    public void setMemorySize(int memorySize) {
        this.memorySize = memorySize;
    }

    public int getLoopCounter() {
        return loopCounter;
    }

    public void setLoopCounter(int loopCounter) {
        this.loopCounter = loopCounter;
    }

    public int getFdMemorySize() {
        return fdMemorySize;
    }

    public void setFdMemorySize(int fdMemorySize) {
        this.fdMemorySize = fdMemorySize;
    }
}
