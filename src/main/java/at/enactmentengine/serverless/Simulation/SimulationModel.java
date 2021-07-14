package at.enactmentengine.serverless.Simulation;

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

    public SimulationModel(ResultSet functionDeployment, Provider provider, String region, int memorySize, int loopCounter) {
        this.functionDeployment = functionDeployment;
        this.provider = provider;
        this.region = region;
        this.memorySize = memorySize;
        this.loopCounter = loopCounter;
        // TODO get other fields??
        try {
            avgRTT = (long) functionDeployment.getDouble("avgRTT");
            avgLoopCounter = functionDeployment.getInt("avgLoopCounter");
            fdMemorySize = functionDeployment.getInt("memorySize");
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    /**
     * Subtracts the overheads from the RTT stored in the MD.
     * @return the raw execution time of the function
     */
    private long getRawExecutionTime() {
        // if the field 'avgRuntime' has a value set, simply use it
        try {
            double avgRuntime = functionDeployment.getDouble("avgRuntime");
            if (avgRuntime > 1) {
                return (long) avgRuntime;
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

        // TODO add x_cs, CSO, x_a, CO
        Provider mdProvider = null;
        String mdRegion = null;
        try {
            String functionId = functionDeployment.getString("KMS_Arn");
            mdProvider = Utils.detectProvider(functionId);
            mdRegion = Utils.detectRegion(functionId);
        } catch (SQLException | RegionDetectionException throwables) {
            throwables.printStackTrace();
        }

        ResultSet mdProviderEntry = MariaDBAccess.getProviderEntry(mdProvider);
        ResultSet mdRegionEntry = MariaDBAccess.getRegionEntry(mdRegion, mdProvider);

        try {
            mdProviderEntry.next();
            mdRegionEntry.next();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

        int faasOverhead = 0;
        int cryptoOverhead = 0;
        int networkOverhead = 0;
        int handshake = 0;
        int authenticationOverhead = 0;
        int concurrencyOverhead = 0;

        try {
            faasOverhead = mdProviderEntry.getInt("faasSystemOverheadms");
            cryptoOverhead = mdProviderEntry.getInt("cryptoOverheadms");
            networkOverhead = mdRegionEntry.getInt("networkOverheadms");
            concurrencyOverhead = mdProviderEntry.getInt("concurrencyOverheadms");
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

        if (avgLoopCounter != 0 && concurrencyOverhead != 0) {
            concurrencyOverhead *= avgLoopCounter;
        }

        // TODO other providers?
        if (mdProvider == Provider.AWS) {
            handshake = 2;

            if (faasOverhead != 0 && cryptoOverhead != 0 && networkOverhead != 0) {
                authenticationOverhead = cryptoOverhead + handshake * networkOverhead;
            } else {
                // TODO throw exception
                return -1;
            }
        } else if (mdProvider == Provider.GOOGLE) {
            handshake = 1;
        }

        if (faasOverhead != 0 && networkOverhead != 0) {
            // TODO add x_cs, CSO, x_a
            return avgRTT - networkOverhead - faasOverhead - authenticationOverhead - concurrencyOverhead;
        } else {
            // TODO throw exception
            return -1;
        }
    }

    /**
     * Adds the required overheads to the given execution time to get the final round-trip time.
     * @param executionTime to add the overheads to
     * @return the overall round-trip time
     */
    private long addOverheads(long executionTime) {
        // O = xcs · CSO + NO + xa · AO + F O + CO
        // TODO add x_cs, CSO, x_a, CO

        ResultSet providerEntry = MariaDBAccess.getProviderEntry(provider);
        ResultSet regionEntry = MariaDBAccess.getRegionEntry(region, provider);

        try {
            providerEntry.next();
            regionEntry.next();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

        int faasOverhead = 0;
        int cryptoOverhead = 0;
        int networkOverhead = 0;
        int concurrencyOverhead = 0;

        try {
            faasOverhead = providerEntry.getInt("faasSystemOverheadms");
            cryptoOverhead = providerEntry.getInt("cryptoOverheadms");
            networkOverhead = regionEntry.getInt("networkOverheadms");
            concurrencyOverhead = providerEntry.getInt("concurrencyOverheadms");
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

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
            // TODO throw exception
            return -1;
        }
    }

    private long applyDistribution(long executionTime, boolean success) {
        if (success) {
            // calculate the time as usual
            Random r = new Random();
            executionTime = (long) (r.nextGaussian() * (executionTime * 0.05) + executionTime);
        } else {
            // get a random double between 0 and 1
            Random random = new Random();
            executionTime *= random.nextDouble();
        }
        return executionTime;
    }

    public PairResult<Long, Double> simulateRoundTripTime(boolean success) {
        /*
        read from MD-DB the provider and region for the FD and the desired simulation
            from provider get faasSystemOverheadms and cryptoOverheadms
            from region get NO
        from avgRTT subtract the values as in formula
        to remainder add the values as in formula
         */
        long rtt = 0;
        double cost = 0;
        // TODO check if memory is the same
        if (memorySize == fdMemorySize) {
            long executionTime = getRawExecutionTime();
            executionTime = applyDistribution(executionTime, success);

            cost = MariaDBAccess.calculateCost(memorySize, executionTime, provider);
            SimulationParameters.workflowCost += cost;
            rtt = addOverheads(executionTime);
        } else {
            // simulate different memory
        }


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
