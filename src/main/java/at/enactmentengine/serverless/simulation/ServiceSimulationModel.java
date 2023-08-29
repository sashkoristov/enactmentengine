package at.enactmentengine.serverless.simulation;

import at.uibk.dps.afcl.functions.objects.PropertyConstraint;
import at.uibk.dps.databases.MariaDBAccess;
import com.google.gson.Gson;
import jFaaS.utils.PairResult;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
            Pair<Integer, Integer> serviceTypeInfo = getServiceTypeInformation();
            typeId = serviceTypeInfo.getLeft();
            providerId = serviceTypeInfo.getRight();
            serviceRegionId = getRegionId(serviceRegionName);
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
        lambdaRegionId = getRegionId(lambdaRegionName);
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
            Pair<Double, Double> dataTransferParams = getDataTransferParamsFromDB(type, false);
            Double bandwidth = dataTransferParams.getLeft();        // in Mbps
            Double latency = dataTransferParams.getRight();         // in ms

            roundTripTime = expectedWork * latency + ((expectedData / bandwidth) * 1000);
        } else if (type.equals("DT_REMOVE") || type.equals("UT_REMOVE")) {
            Pair<Double, Double> dataTransferParams = getDataTransferParamsFromDB(type, true);
            Double bandwidth = dataTransferParams.getLeft();        // in Mbps
            Double latency = dataTransferParams.getRight();         // in ms

            roundTripTime = -(expectedWork * latency + ((expectedData / bandwidth) * 1000));
        } else {
            // 1. get missing information from DB
            // 1.1 get Networking information
            Triple<Double, Double, Double> networkParams = getNetworkParamsFromDB();
            Double bandwidth = networkParams.getLeft();
            Double lambdaLatency = networkParams.getMiddle();
            Double serviceLatency = networkParams.getRight();

            // 1.2 get Service Information
            Pair<Double, Double> serviceParams = getServiceParamsFromDB();
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

    /**
     * Fetches data transfer parameters from the DB.
     *
     * @param type the type of the data transfer, either "FILE_DL" or "FILE_UP"
     * @param useOriginalLambdaRegion if the original lambda region should be used or not
     *
     * @return pair of bandwidth [Mb/s] and latency [ms]
     */
    private Pair<Double, Double> getDataTransferParamsFromDB(String type, boolean useOriginalLambdaRegion) {
        Connection connection = MariaDBAccess.getConnection();
        String query = "SELECT bandwidth, latency AS latency FROM data_transfer WHERE type = ? AND functionRegionID = ? AND storageRegionID = ?";
        ResultSet resultSet = null;
        String dataTransferType = null;

        if (type.equals("FILE_DL") || type.equals("DT_REMOVE")) {
            dataTransferType = "download";
        } else if (type.equals("FILE_UP") || type.equals("UT_REMOVE")) {
            dataTransferType = "upload";
        }

        try {
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, dataTransferType);
            preparedStatement.setInt(2, useOriginalLambdaRegion && originallambdaRegionId != -1 ? originallambdaRegionId : lambdaRegionId);
            preparedStatement.setInt(3, serviceRegionId);
            resultSet = preparedStatement.executeQuery();
            resultSet.next();
            return Pair.of(resultSet.getDouble(1), resultSet.getDouble(2));
        } catch (SQLException e) {
            throw new DatabaseEntryNotFoundException("Could not fetch data transfer parameters from database");
        }
    }

    /**
     * Fetches simulation relevant network parameters from the database.
     *
     * @return triple of bandwidth [Mb/ms], lambda latency [ms] and service latency [ms].
     */
    private Triple<Double, Double, Double> getNetworkParamsFromDB() {
        // Choose which parameters to return according to service type
        // example: FaceRec vs Bucket

        Connection connection = MariaDBAccess.getConnection();
        String query = "SELECT bandwidth / 1000, latency AS latency FROM networking WHERE sourceRegionID = ? AND destinationRegionID = ?";
        ResultSet resultSet = null;
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, lambdaRegionId);
            preparedStatement.setInt(2, serviceRegionId);
            resultSet = preparedStatement.executeQuery();
            resultSet.next();
            double bandwidth = resultSet.getDouble(1);
            double latency = resultSet.getDouble(2);
            if (serviceRegionId.equals(lambdaRegionId)) {
                return Triple.of(bandwidth, latency, latency);
            } else {
                preparedStatement.setInt(1, serviceRegionId);
                resultSet = preparedStatement.executeQuery();
                resultSet.next();
                double serviceLatency = resultSet.getDouble(2);
                return Triple.of(bandwidth, latency, serviceLatency);
            }

        } catch (SQLException e) {
            throw new DatabaseEntryNotFoundException("Could not fetch network parameters from database for " + lambdaRegionId + "," + serviceRegionId);
        }
    }

    /**
     * Fetches the service parameters from the database.
     *
     * @return pair of relevant service parameters (velocity [ms/work], startup time [ms])
     */
    private Pair<Double, Double> getServiceParamsFromDB() {
        Connection connection = MariaDBAccess.getConnection();
        String query = "SELECT velocity, startup AS startup FROM serviceDeployment WHERE serviceID = ? AND regionID = ?";
        ResultSet resultSet = null;

        try {
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, typeId);
            preparedStatement.setInt(2, serviceRegionId);
            resultSet = preparedStatement.executeQuery();
            resultSet.next();
            return Pair.of(resultSet.getDouble(1), resultSet.getDouble(2));
        } catch (SQLException e) {
            throw new DatabaseEntryNotFoundException("Could not fetch service parameters from database");
        }
    }

    /**
     * Fetches the service type information from the database.
     *
     * @return pair of (service-) typeId, providerId
     */
    private Pair<Integer, Integer> getServiceTypeInformation() {
        Connection connection = MariaDBAccess.getConnection();
        String query = "SELECT id, providerID FROM service WHERE type = ?";
        ResultSet resultSet = null;
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, type);
            resultSet = preparedStatement.executeQuery();
            resultSet.next();
            return Pair.of(resultSet.getInt(1), resultSet.getInt(2));
        } catch (SQLException e) {
            throw new DatabaseEntryNotFoundException("Could not fetch service type information from database");
        }
    }

    /**
     * Fetches the regionId for a region with the specified name
     *
     * @param regionName name of the region to fetch the id for
     *
     * @return the corresponding regionId
     */
    private int getRegionId(String regionName) {
        Connection connection = MariaDBAccess.getConnection();
        String query = "SELECT id FROM region WHERE region = ?";
        ResultSet resultSet = null;

        try {
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, regionName);
//            preparedStatement.setString(2, providerId.toString());
            resultSet = preparedStatement.executeQuery();
            resultSet.next();
            return resultSet.getInt(1);
        } catch (SQLException e) {
            throw new DatabaseEntryNotFoundException("No regionID could be determined from regionName '" + regionName + "'");
        }
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
