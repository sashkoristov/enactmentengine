package at.enactmentengine.serverless.simulation;

import at.enactmentengine.serverless.simulation.metadata.MetadataStore;
import at.uibk.dps.afcl.functions.objects.PropertyConstraint;
import com.google.gson.Gson;
import jFaaS.utils.PairResult;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class ServiceSimulationModel {

    private static final Logger logger = LoggerFactory.getLogger(ServiceSimulationModel.class);

    private String serviceString;
    private final String type;
    private final double expectedWork;
    private final double expectedData;
    private Integer typeId;
    private Integer providerId;
    private Integer lambdaRegionId;

    private Integer originallambdaRegionId;
    private Integer serviceRegionId;

    private ServiceSimulationModel(String serviceString) {
        this.serviceString = serviceString;
        try {
            List<String> properties = Arrays.asList(serviceString.split(":"));
            type = properties.get(0);
            String serviceRegionName = properties.get(1);
            expectedWork = Double.parseDouble(properties.get(2));
            expectedData = Double.parseDouble(properties.get(3));
            Pair<Integer, Integer> serviceTypeInfo = MetadataStore.get().getServiceTypeInformation(type);
            typeId = serviceTypeInfo.getLeft();
            providerId = serviceTypeInfo.getRight();
            serviceRegionId = MetadataStore.get().getRegionId(serviceRegionName);
        } catch (RuntimeException e) {
            throw new ServiceStringException("Service deployment string could not be parsed.");
        }
    }

    public ServiceSimulationModel(Integer lambdaRegionId, String serviceString) {
        this(serviceString);
        this.lambdaRegionId = lambdaRegionId;
        this.originallambdaRegionId = lambdaRegionId;
    }

    public ServiceSimulationModel(Integer originalLambdaRegionId, String lambdaRegionName, String serviceString) {
        this(serviceString);
        lambdaRegionId = MetadataStore.get().getRegionId(lambdaRegionName);
        this.originallambdaRegionId = originalLambdaRegionId;
    }

    /**
     * Gets all used services from properties and adds them to the list.
     */
    public static List<String> getUsedServices(List<PropertyConstraint> properties) {
        List<String> serviceStrings = new ArrayList<>();
        for (PropertyConstraint property : properties) {
            if (property.getName().equals("service")) {
                serviceStrings.add(property.getValue());
            }
        }
        return serviceStrings;
    }

    /**
     * Computes the total round trip time for all used services for the lambda region with the given id
     */
    public static PairResult<String, Long> calculateTotalRttForUsedServices(Integer lambdaRegionId, List<String> usedServiceStrings) {
        long rtt = 0;
        int index = 1;

        Map<String, Double> serviceOutput = new HashMap<>();

        for (String serviceString : usedServiceStrings) {
            ServiceSimulationModel serviceSimulationModel = new ServiceSimulationModel(lambdaRegionId, serviceString);

            rtt += serviceSimulationModel.calculateRTT(index, serviceOutput);
            index++;
        }

        return new PairResult<>(new Gson().toJson(serviceOutput), rtt);
    }

    /**
     * Computes the total round trip time for all used services for the lambda region with the given name
     */
    public static PairResult<String, Long> calculateTotalRttForUsedServices(Integer lambdaRegionId, String lambdaRegionName, List<String> usedServiceStrings) {
        long rtt = 0;
        int index = 1;

        Map<String, Double> serviceOutput = new HashMap<>();

        for (String serviceString : usedServiceStrings) {
            ServiceSimulationModel serviceSimulationModel = new ServiceSimulationModel(lambdaRegionId, lambdaRegionName, serviceString);
            rtt += serviceSimulationModel.calculateRTT(index, serviceOutput);
            index++;
        }

        return new PairResult<>(new Gson().toJson(serviceOutput), rtt);
    }

    /**
     * Calculates the simulated round trip time of a service deployment.
     *
     * @return simulated round trip time in seconds
     */
    public long calculateRTT(int index, Map<String, Double> output) {
        double roundTripTime;
        // check if service is of type file transfer
        if (type.equals("FILE_DL") || type.equals("FILE_UP")) {
            Pair<Double, Double> dataTransferParams = MetadataStore.get().getDataTransferParamsFromDB(type, lambdaRegionId,
                    serviceRegionId, originallambdaRegionId, false);
            Double bandwidth = dataTransferParams.getLeft();        // in Mbps
            Double latency = dataTransferParams.getRight();         // in ms

            roundTripTime = expectedWork * latency + ((expectedData / bandwidth) * 1000);
        } else if (type.equals("DT_REMOVE") || type.equals("UT_REMOVE")) {
            Pair<Double, Double> dataTransferParams = MetadataStore.get().getDataTransferParamsFromDB(type, lambdaRegionId,
                    serviceRegionId, originallambdaRegionId, true);
            Double bandwidth = dataTransferParams.getLeft();        // in Mbps
            Double latency = dataTransferParams.getRight();         // in ms

            roundTripTime = -(expectedWork * latency + ((expectedData / bandwidth) * 1000));
        } else {
            // 1. get missing information from DB
            // 1.1 get Networking information
            Triple<Double, Double, Double> networkParams = MetadataStore.get().getNetworkParamsFromDB(lambdaRegionId, serviceRegionId);
            Double bandwidth = networkParams.getLeft();
            Double lambdaLatency = networkParams.getMiddle();
            Double serviceLatency = networkParams.getRight();

            // 1.2 get Service Information
            Pair<Double, Double> serviceParams = MetadataStore.get().getServiceParamsFromDB(typeId, serviceRegionId);
            Double velocity = serviceParams.getLeft();
            Double startUpTime = serviceParams.getRight();

            // 2. calculate RTT according to model
            roundTripTime = (expectedWork / velocity) + lambdaLatency +
                    startUpTime + (expectedData / bandwidth) + serviceLatency;
        }

        Double serviceTime = ((double) Math.round(roundTripTime * 100)) / 100;
        output.put("service_" + index + "_" + serviceString, serviceTime);

        logger.info("Simulated service runtime of " + serviceString + ": " + serviceTime + "ms");

        return (long) roundTripTime;
    }

    public static class ServiceStringException extends RuntimeException {
        public ServiceStringException(String message) {
            super(message);
        }
    }

    public static class DatabaseEntryNotFoundException extends RuntimeException {
        public DatabaseEntryNotFoundException(String message) {
            super(message);
        }
    }
}
