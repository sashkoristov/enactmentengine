package at.enactmentengine.serverless.Simulation;

import at.uibk.dps.databases.MariaDBAccess;
import at.uibk.dps.exceptions.RegionDetectionException;
import at.uibk.dps.util.Provider;
import at.uibk.dps.util.Utils;

import java.sql.ResultSet;
import java.sql.SQLException;

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
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    /**
     * Subtracts the overheads from the RTT stored in the MD.
     * @return the raw execution time of the function
     */
    private long getRawExecutionTime() {
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

        // TODO other providers?
        if (mdProvider == Provider.AWS) {
            handshake = 2;
        } else if (mdProvider == Provider.GOOGLE) {
            handshake = 1;
        }

        try {
            faasOverhead = mdProviderEntry.getInt("faasSystemOverheadms");
            cryptoOverhead = mdProviderEntry.getInt("cryptoOverheadms");
            networkOverhead = mdRegionEntry.getInt("networkOverheadms");
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

        if (faasOverhead != 0 && cryptoOverhead != 0 && networkOverhead != 0) {
            int authenticationOverhead = cryptoOverhead + handshake * networkOverhead;
            // TODO add x_cs, CSO, x_a, CO
            return avgRTT - networkOverhead - authenticationOverhead - faasOverhead;
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

        try {
            faasOverhead = providerEntry.getInt("faasSystemOverheadms");
            cryptoOverhead = providerEntry.getInt("cryptoOverheadms");
            networkOverhead = regionEntry.getInt("networkOverheadms");
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

        if (faasOverhead != 0 && cryptoOverhead != 0 && networkOverhead != 0) {
            long rtt = executionTime + networkOverhead + faasOverhead;
            //TODO add x_cs, CSO, x_a, CO
            if (SimulationParameters.USE_COLD_START) {
                //TODO check if the current function is invoked the first time or if new ones have to be created in parallelFor
            }

            if (SimulationParameters.USE_AUTHENTICATION) {
                //TODO authenticate for the first function of a provider
                int handshake = 0;
                // TODO other providers?
                if (provider == Provider.AWS) {
                    handshake = 2;
                } else if (provider == Provider.GOOGLE) {
                    handshake = 1;
                }
                int authenticationOverhead = cryptoOverhead + handshake * networkOverhead;
                // if authentication is required, add it to the RTT
                rtt += authenticationOverhead;
            }

            if (loopCounter != -1) {
                //TODO read CO (=d) from DB and multiply it with the loopCounter
            }

            return rtt;
        } else {
            // TODO throw exception
            return -1;
        }
    }

    public long simulateRoundTripTime() {
        /*
        read from MD-DB the provider and region for the FD and the desired simulation
            from provider get faasSystemOverheadms and cryptoOverheadms
            from region get NO
        from avgRTT subtract the values as in formula
        to remainder add the values as in formula
         */
        long executionTime = getRawExecutionTime();
        long rtt = addOverheads(executionTime);

        return rtt;
    }

    public ResultSet getFunctionDeployment() {
        return functionDeployment;
    }

    public void setFunctionDeployment(ResultSet functionDeployment) {
        this.functionDeployment = functionDeployment;
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
        this.provider = provider;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
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
}
