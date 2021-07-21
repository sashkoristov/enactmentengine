package at.enactmentengine.serverless.nodes;

import at.enactmentengine.serverless.Simulation.SimulationModel;
import at.enactmentengine.serverless.Simulation.SimulationParameters;
import at.enactmentengine.serverless.exception.*;
import at.enactmentengine.serverless.object.PairResult;
import at.enactmentengine.serverless.object.QuadrupleResult;
import at.enactmentengine.serverless.object.Utils;
import at.uibk.dps.afcl.functions.objects.DataIns;
import at.uibk.dps.afcl.functions.objects.DataOutsAtomic;
import at.uibk.dps.afcl.functions.objects.PropertyConstraint;
import at.uibk.dps.cronjob.ManualUpdate;
import at.uibk.dps.databases.MariaDBAccess;
import at.uibk.dps.databases.MongoDBAccess;
import at.uibk.dps.exception.InvokationFailureException;
import at.uibk.dps.exception.LatestFinishingTimeException;
import at.uibk.dps.exception.LatestStartingTimeException;
import at.uibk.dps.exception.MaxRunningTimeException;
import at.uibk.dps.function.Function;
import at.uibk.dps.util.Event;
import at.uibk.dps.util.Provider;
import at.uibk.dps.util.Type;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

/**
 * Class which handles the simulation of a function.
 *
 * @author mikahautz by adapting from {@link FunctionNode}
 */
public class SimulationNode extends Node {

    /**
     * Logger for a simulation node.
     */
    private static final Logger logger = LoggerFactory.getLogger(SimulationNode.class);

    /**
     * The number of executed functions.
     */
    private static int counter = 0;

    /**
     * The execution id of the workflow (needed to log the execution).
     */
    private int executionId;

    /**
     * The deployment of the Atomic Function.
     */
    private String deployment;

    /**
     * The constraints for the simulation node.
     */
    private List<PropertyConstraint> constraints;

    /**
     * The properties of the simulation node.
     */
    private List<PropertyConstraint> properties;

    /**
     * Output of the simulation node.
     */
    private List<DataOutsAtomic> output;

    /**
     * Input to the simulation node.
     */
    private List<DataIns> input;

    /**
     * The result of the simulation node.
     */
    private Map<String, Object> result;

    /**
     * Constructor for a simulation node.
     *
     * @param name        of the base function.
     * @param type        of the base function (fType).
     * @param deployment  of the base function.
     * @param properties  of the base function.
     * @param constraints of the base function.
     * @param input       to the base function.
     * @param output      of the base function.
     * @param executionId for the logging of the simulation.
     */
    public SimulationNode(String name, String type, String deployment, List<PropertyConstraint> properties, List<PropertyConstraint> constraints,
                          List<DataIns> input, List<DataOutsAtomic> output, int executionId) {
        super(name, type);
        this.deployment = deployment;
        this.output = output;
        this.properties = properties;
        this.constraints = constraints;
        this.input = input;
        this.executionId = executionId;
        if (output == null) {
            this.output = new ArrayList<>();
        }
    }

    /**
     * Extracts the memory size, region and function name of the deployment string.
     *
     * @param deployment to extract the values from
     *
     * @return a list containing 4 elements, the first is the memory size, the second the region, the third the provider
     * and the fourth the function name.
     */
    private static List<String> extractValuesFromDeployment(String deployment) {
        List<String> result = new ArrayList<>();
        String[] parts = deployment.split("_");
        result.add(parts[parts.length - 1]);
        result.add(parts[parts.length - 2]);
        result.add(parts[parts.length - 3]);
        // since the function name could contain underscores as well, we have to concat all remaining elements
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < parts.length - 3; i++) {
            stringBuilder.append(parts[i]).append("_");
        }
        stringBuilder.setLength(stringBuilder.length() - 1);
        result.add(stringBuilder.toString());

        return result;
    }

    /**
     * Sets the dataValues and passes the result to all children.
     *
     * @param input to the child functions.
     */
    @Override
    public void passResult(Map<String, Object> input) {
        synchronized (this) {
            try {
                dataValues = input;
                for (Node node : children) {
                    node.passResult(input);
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    /**
     * Get the result of a simulation node.
     *
     * @return result of the base function.
     */
    @Override
    public Map<String, Object> getResult() {
        // TODO
        return result;
    }

    /**
     * Checks the inputs, invokes function and passes results to children.
     *
     * @return boolean representing success of the node execution.
     *
     * @throws Exception on failure.
     */
    @Override
    public Boolean call() throws Exception {

        /* The identifier for the current function */
        int id;
        synchronized (this) {
            id = counter++;
        }

        /* Read the resource link of the base function */
        String resourceLink = Utils.getResourceLink(properties, this);
        Provider provider = Utils.detectProvider(resourceLink);
        Provider deploymentProvider = null;
        if (deployment != null) {
            List<String> elements = extractValuesFromDeployment(deployment);
            deploymentProvider = Provider.valueOf(elements.get(2));
        }
        if ((provider != Provider.AWS && provider != Provider.GOOGLE && provider != Provider.IBM) ||
                (deploymentProvider != null && deploymentProvider != Provider.AWS && deploymentProvider != Provider.GOOGLE &&
                        deploymentProvider != Provider.IBM)) {
            throw new Exception("Simulating is currently only supported for AWS, Google and IBM.");
        }
        if (deploymentProvider != null && provider != deploymentProvider) {
            throw new Exception("Simulating across providers is currently not supported.");
        }
        String loopId = "";
        if (inLoop()) {
            loopId = ", loopId=" + loopCounter;
        }
        logger.info("Simulating function " + name + " at resource: " + resourceLink + " [" + System.currentTimeMillis()
                + "ms], id=" + id + loopId);

        // TODO as in FunctionNode, log function inputs by getting the values from the mdDB?
        // logFunctionInput(actualFunctionInputs, id);

        /* Parse function with optional constraints and properties */
        Function functionToInvoke = Utils.parseFTConstraints(resourceLink, null, constraints, type, name, loopCounter);

        // parseFTConstraints returns null if there are no constraints set
        // since it will be executed without FT, only the resourceLink is needed
        if (functionToInvoke == null) {
            functionToInvoke = new Function(resourceLink, name, type, loopCounter, null);
        }

        //TODO check if function is stored in metadataDB
        // if not exists then log error and return

        /* Simulate function and measure duration */
        long start = System.currentTimeMillis();
        QuadrupleResult<Long, Double, Map<String, Object>, Boolean> simResult = simulateFunction(functionToInvoke);
        long end = System.currentTimeMillis();

        // TODO as in FunctionNode, log function output by getting the values from the mdDB?
        // logFunctionOutput(start, end, resultString, id);

        result = simResult.getOutput();

        /* Pass the output to the next node */
        for (Node node : children) {
            node.passResult(result);
            if (getLoopCounter() != -1) {
                node.setLoopCounter(loopCounter);
                node.setMaxLoopCounter(maxLoopCounter);
            }
            node.call();
        }

        /* Set the result of the function node */
//        result = functionOutputs;

        // TODO
        /*
         * Check if the execution identifier is specified (check if execution should be
         * stored in the database)
         */
//        if (executionId != -1) {
//
//            /* Create a function invocation object */
//            Invocation functionInvocation = new Invocation(resourceLink, Utils.detectProvider(resourceLink).toString(),
//                    Utils.detectRegion(resourceLink),
//                    new Timestamp(start + TimeZone.getTimeZone("Europe/Rome").getOffset(start)),
//                    new Timestamp(end + TimeZone.getTimeZone("Europe/Rome").getOffset(start)), (end - start),
//                    Utils.checkResultSuccess(simResult.getOutput()).toString(), null, executionId);
//
//            /* Store the invocation in the database */
//            Utils.storeInDBFunctionInvocation(logger, functionInvocation, executionId);
//        }

        return true;
    }

    /**
     * Simulates the base function.
     *
     * @param functionToSimulate the base function which should be simulated.
     *
     * @return a TripleResult containing the RTT, output and success of the simulated function
     */
    private QuadrupleResult<Long, Double, Map<String, Object>, Boolean> simulateFunction(Function functionToSimulate) throws NoDatabaseEntryForIdException,
            NotYetInvokedException, InvokationFailureException, LatestFinishingTimeException, LatestStartingTimeException, MaxRunningTimeException, SQLException, RegionDetectionException, at.uibk.dps.exceptions.RegionDetectionException, MissingResourceLinkException, MissingComputationalWorkException, MissingSimulationParametersException, AlternativeStrategyException {
        // TODO simulate here
        String resourceLink = functionToSimulate.getUrl();
        QuadrupleResult<Long, Double, Map<String, Object>, Boolean> result = null;

        // check if the function should be simulated with fault tolerance
        if (functionToSimulate.hasConstraintSet() || functionToSimulate.hasFTSet()) {
            // simulate with FT
            logger.info("Simulating function with fault tolerance...");
            result = simulateFunctionFT(functionToSimulate);

        } else {
            long startTime = getStartingTime();
            result = getSimulationResult(resourceLink);
            if (result.isSuccess()) {
                logger.info("Simulating function {} took {}ms.", resourceLink, result.getRTT());
            } else {
                logger.info("Simulating function {} failed.", resourceLink);
            }
            MongoDBAccess.saveLog(Event.FUNCTION_END, resourceLink, getName(), functionToSimulate.getType(), null,
                    result.getRTT(), result.getCost(), result.isSuccess(), loopCounter, maxLoopCounter, startTime, Type.SIM);
        }

        return result;
    }

    /**
     * Simulates a function with FT.
     *
     * @param function to simulate
     *
     * @return a TripleResult containing the RTT, output and success of the simulated function
     *
     * @throws LatestStartingTimeException  on latest start time exceeded
     * @throws InvokationFailureException   on failed invocation
     * @throws LatestFinishingTimeException on latest finish time exceeded
     * @throws MaxRunningTimeException      on maximum runtime exceeded
     */
    private QuadrupleResult<Long, Double, Map<String, Object>, Boolean> simulateFunctionFT(Function function) throws LatestStartingTimeException, InvokationFailureException, LatestFinishingTimeException, MaxRunningTimeException, NoDatabaseEntryForIdException, NotYetInvokedException, SQLException, RegionDetectionException, at.uibk.dps.exceptions.RegionDetectionException, MissingResourceLinkException, MissingComputationalWorkException, AlternativeStrategyException, MissingSimulationParametersException {
        QuadrupleResult<Long, Double, Map<String, Object>, Boolean> quadrupleResult;

        if (function != null) {
            if (function.hasConstraintSet()) {
                Timestamp timeAtStart = new Timestamp(getStartingTime());
                if (function.getConstraints().hasLatestStartingTime()) {
                    if (timeAtStart.after(function.getConstraints().getLatestStartingTime())) {
                        throw new LatestStartingTimeException("latestStartingTime constraint missed!");
                    }
                    if (!function.getConstraints().hasLatestFinishingTime()
                            && !function.getConstraints().hasMaxRunningTime()) {
                        quadrupleResult = simulateFT(function);

                        if (!quadrupleResult.isSuccess()) {
                            throw new InvokationFailureException("Invocation has failed");
                        } else {
                            return quadrupleResult;
                        }
                    }
                }

                quadrupleResult = simulateFT(function);

                if (quadrupleResult.isSuccess()) {
                    // check maxRunningTime
                    Timestamp newTime = new Timestamp(timeAtStart.getTime() + quadrupleResult.getRTT());

                    if (function.getConstraints().hasLatestFinishingTime()) {
                        if (newTime.after(function.getConstraints().getLatestFinishingTime())) {
                            // missed LatestFinishingTime deadline
                            throw new LatestFinishingTimeException("Missed LatestFinishingTime deadline");
                        }
                    }
                    if (function.getConstraints().hasMaxRunningTime()) {
                        if (newTime.getTime() - timeAtStart.getTime() > function.getConstraints().getMaxRunningTime()) {
                            // Missed MaxRunningTime deadline
                            throw new MaxRunningTimeException("MaxRunningTime has passed");
                        }
                    }
                    return quadrupleResult;
                } else {
                    throw new InvokationFailureException("Invocation has failed after entire alternative strategy");
                }
            } else {
                // no constraints. Just invoke in current thread. (we do not need to cancel)
                quadrupleResult = simulateFT(function);
                if (!quadrupleResult.isSuccess()) {
                    throw new InvokationFailureException("Invocation has failed");
                } else {
                    return quadrupleResult;
                }
            }
        } else {
            throw new InvokationFailureException("Function is null");
        }

    }

    /**
     * Helper method to simulate a function with FT.
     *
     * @param function to simulate
     *
     * @return a TripleResult containing the RTT, output and success of the simulated function
     */
    private QuadrupleResult<Long, Double, Map<String, Object>, Boolean> simulateFT(Function function) throws NoDatabaseEntryForIdException, NotYetInvokedException, SQLException, RegionDetectionException, at.uibk.dps.exceptions.RegionDetectionException, MissingResourceLinkException, MissingComputationalWorkException, AlternativeStrategyException, MissingSimulationParametersException {
        long startTime = getStartingTime();
        String resourceLink = function.getUrl();
        QuadrupleResult<Long, Double, Map<String, Object>, Boolean> result = getSimulationResult(resourceLink);

        if (!result.isSuccess()) {
            logger.info("Simulating function {} failed.", resourceLink);
            MongoDBAccess.saveLog(Event.FUNCTION_FAILED, resourceLink, getName(), function.getType(), null, result.getRTT(),
                    result.getCost(), result.isSuccess(), loopCounter, maxLoopCounter, startTime, Type.SIM);
            if (function.hasFTSet()) {
                logger.info("##############  First invocation has failed, retrying " + function.getFTSettings().getRetries() +
                        " times.  ##############");
                for (int i = 0; i < function.getFTSettings().getRetries(); i++) {
                    // increment the starting time by the previous RTT
                    startTime += result.getRTT();
                    result = getSimulationResult(resourceLink);
                    if (result.isSuccess()) {
                        logger.info("Simulating function {} took {}ms.", resourceLink, result.getRTT());
                        MongoDBAccess.saveLog(Event.FUNCTION_END, resourceLink, getName(), function.getType(), null, result.getRTT(),
                                result.getCost(), result.isSuccess(), loopCounter, maxLoopCounter, startTime, Type.SIM);
                        return result;
                    }
                    logger.info("Simulating function {} failed.", resourceLink);
                    MongoDBAccess.saveLog(Event.FUNCTION_FAILED, resourceLink, getName(), function.getType(), null, result.getRTT(),
                            result.getCost(), result.isSuccess(), loopCounter, maxLoopCounter, startTime, Type.SIM);
                }
                // Failed after all retries. Check for alternative Strategy
                if (function.getFTSettings().hasAlternativeStartegy()) {
                    // AlternativeStrategy has correct Result
                    return simulateAlternativeStrategy(function);
                } else {
                    // no alternativeStrategy set so failure
                    return new QuadrupleResult<>(null, null, null, false);
                }
            } else {
                // function failed and no FT is set
                return result;
            }
        }
        logger.info("Simulating function {} took {}ms.", resourceLink, result.getRTT());
        MongoDBAccess.saveLog(Event.FUNCTION_END, resourceLink, getName(), function.getType(), null, result.getRTT(),
                result.getCost(), result.isSuccess(), loopCounter, maxLoopCounter, startTime, Type.SIM);
        return result;
    }

    /**
     * Simulates the alternativeStrategy of a given function.
     *
     * @param function to simulate the alternativeStrategy
     *
     * @return a TripleResult containing the RTT, output and success of the simulated function
     *
     * @throws Exception if the alternativeStrategies have been executed without success
     */
    private QuadrupleResult<Long, Double, Map<String, Object>, Boolean> simulateAlternativeStrategy(Function function) throws NoDatabaseEntryForIdException, NotYetInvokedException, AlternativeStrategyException, SQLException, RegionDetectionException, at.uibk.dps.exceptions.RegionDetectionException, MissingResourceLinkException, MissingComputationalWorkException, MissingSimulationParametersException {

        if (function.getFTSettings().getAltStrategy() != null) {
            int i = 0;
            for (List<Function> alternativePlan : function.getFTSettings().getAltStrategy()) {
                // create a map to store the intermediate results
                HashMap<String, QuadrupleResult<Long, Double, Map<String, Object>, Boolean>> tempResults = new HashMap<>();
                QuadrupleResult<Long, Double, Map<String, Object>, Boolean> result = null;
                long startTime = getStartingTime();
                int j = 0;
                logger.info("##############  Trying Alternative Plan " + i + "  ##############");
                for (Function alternativeFunction : alternativePlan) {
                    logger.info("##############  Trying Alternative Function " + j + "  ##############");
                    result = getSimulationResult(alternativeFunction.getUrl());
                    tempResults.put(alternativeFunction.getUrl(), result);
                    j++;
                }
                // go through all executed alternative functions to determine which of the successful ones was the fastest
                result = null;
                String url = null;
                for (Map.Entry<String, QuadrupleResult<Long, Double, Map<String, Object>, Boolean>> set : tempResults.entrySet()) {
                    if (set.getValue().isSuccess()) {
                        // check if result is null or if the RTT in the result is greater than the current RTT
                        if (result == null || (result.getRTT() > set.getValue().getRTT())) {
                            result = set.getValue();
                            url = set.getKey();
                        }
                    }
                }

                // check if at least one function simulated successfully
                if (result != null) {
                    // go through the executed functions to log that they were "canceled"
                    for (Map.Entry<String, QuadrupleResult<Long, Double, Map<String, Object>, Boolean>> set : tempResults.entrySet()) {
                        if (!set.getKey().equals(url) && (set.getValue().getRTT() >= result.getRTT())) {
                            // they were "canceled" after the fastest function finished, therefore the RTT of the
                            // result is the RTT of the canceled function
                            logger.info("Canceled simulation of function {} after {}ms.", set.getKey(), result.getRTT());
                            // TODO which value to set here for success? True or false?
                            MongoDBAccess.saveLog(Event.FUNCTION_CANCELED, set.getKey(), getName(), function.getType(), null,
                                    result.getRTT(), result.getCost(), set.getValue().isSuccess(), loopCounter, maxLoopCounter, startTime, Type.SIM);
                        } else if (!set.getValue().isSuccess()) {
                            // if a function was unsuccessful AND it ran shorter than the fastest successful one
                            logger.info("Simulating function {} failed.", set.getKey());
                            MongoDBAccess.saveLog(Event.FUNCTION_FAILED, set.getKey(), getName(), function.getType(), null,
                                    set.getValue().getRTT(), set.getValue().getCost(), set.getValue().isSuccess(), loopCounter, maxLoopCounter, startTime, Type.SIM);
                        }
                    }
                    // log the fastest successful function
                    logger.info("Simulating function {} took {}ms.", url, result.getRTT());
                    MongoDBAccess.saveLog(Event.FUNCTION_END, url, getName(), function.getType(), null, result.getRTT(),
                            result.getCost(), result.isSuccess(), loopCounter, maxLoopCounter, startTime, Type.SIM);
                    return result;
                } else {
                    // no function was successful, log their failures
                    for (Map.Entry<String, QuadrupleResult<Long, Double, Map<String, Object>, Boolean>> set : tempResults.entrySet()) {
                        logger.info("Simulating function {} failed.", set.getKey());
                        MongoDBAccess.saveLog(Event.FUNCTION_FAILED, set.getKey(), getName(), function.getType(), null,
                                set.getValue().getRTT(), set.getValue().getCost(), set.getValue().isSuccess(), loopCounter, maxLoopCounter, startTime, Type.SIM);
                    }
                }

                i++;
            }
            throw new AlternativeStrategyException("Failed after entire Alternative Strategy");
        }
        throw new AlternativeStrategyException("No alternative Strategy defined");
    }

    /**
     * Checks if the functionDeployment has the same memory-size, provider and region.
     *
     * @param functionDeployment to get the parameters
     * @param memorySize         of the simulationDeployment
     * @param provider           of the simulationDeployment
     * @param region             of the simulationDeployment
     *
     * @return true if they are the same, false otherwise
     */
    private boolean deploymentsAreTheSame(ResultSet functionDeployment, int memorySize, Provider provider, String region) throws SQLException, RegionDetectionException {
        // TODO also check for same name in DB if different ARN??
        String functionId = null;
        int fdMemorySize = 0;
        Provider fdProvider = null;
        String fdRegion = null;

        functionId = functionDeployment.getString("KMS_Arn");
        fdMemorySize = functionDeployment.getInt("memorySize");
        fdProvider = Utils.detectProvider(functionId);
        fdRegion = Utils.detectRegion(functionId);

        if (fdProvider != null && fdRegion != null && fdMemorySize != 0) {
            return fdMemorySize == memorySize && fdProvider == provider && fdRegion.equals(region);
        }
        return false;
    }

    /**
     * Checks if there is a functionDeployment stored with the same functionImplementation as the given
     * functionDeployment and the same provider, region and memorySize as given.
     *
     * @param functionDeployment to get the parameters
     * @param memorySize         of the simulationDeployment
     * @param provider           of the simulationDeployment
     * @param region             of the simulationDeployment
     *
     * @return entries with the same functionImplementationId
     */
    private ResultSet sameImplementationStored(ResultSet functionDeployment, int memorySize, Provider provider, String region) throws SQLException {
        ResultSet entries = null;
        int functionImplementationId = functionDeployment.getInt("functionImplementation_id");
        entries = MariaDBAccess.getDeploymentsWithImplementationId(functionImplementationId);

        return entries;
    }

    /**
     * Calculates the RTT of a given function.
     *
     * @param entry   the entry from the database
     * @param success if the simulation is success or not
     *
     * @return the RTT in ms
     */
    private PairResult<Long, Double> calculateRoundTripTime(ResultSet entry, Boolean success) throws SQLException, RegionDetectionException, at.uibk.dps.exceptions.RegionDetectionException, MissingComputationalWorkException, MissingSimulationParametersException {
        //TODO
        PairResult<Long, Double> result = null;
        int averageLoopCounter = 0;
        List<String> elements = null;
        int memory = 0;
        String region = null;
        Provider provider = null;
        String functionName = null;
        int concurrencyOverhead = 0;

        if (deployment != null) {
            elements = extractValuesFromDeployment(deployment);
            memory = Integer.parseInt(elements.get(0));
            region = elements.get(1);
            provider = Provider.valueOf(elements.get(2));
            functionName = elements.get(3);

            ResultSet providerEntry = MariaDBAccess.getProviderEntry(provider);
            providerEntry.next();
            concurrencyOverhead = providerEntry.getInt("concurrencyOverheadms");
        } else {
            ResultSet providerEntry = MariaDBAccess.getProviderEntry(Utils.detectProvider(entry.getString("KMS_Arn")));
            providerEntry.next();
            concurrencyOverhead = providerEntry.getInt("concurrencyOverheadms");
        }

        // if the deployment is null or deployment is already saved in the MD-DB,
        // simulate in the same region and with the same memory
        if (deployment == null || deploymentsAreTheSame(entry, memory, provider, region)) {
            // simply read from the values from the DB without calculating them again
            long rtt = (long) entry.getDouble("avgRTT");
            double cost = entry.getDouble("avgCost");
            SimulationParameters.workflowCost += cost;
            averageLoopCounter = entry.getInt("avgLoopCounter");
            if (concurrencyOverhead != 0 && averageLoopCounter != 0) {
                rtt -= (long) concurrencyOverhead * averageLoopCounter;
            }
            if (loopCounter != -1 && concurrencyOverhead != 0) {
                rtt += (long) loopCounter * concurrencyOverhead;
            }

            rtt = SimulationModel.applyDistribution(rtt, success);

            result = new PairResult<>(rtt, cost);
        } else {
            ResultSet similarDeployment = sameImplementationStored(entry, memory, provider, region);
            // indicates if a similar deployment was found
            boolean similar = false;

            if (similarDeployment != null) {
                Integer sameRegionAndMemory = null;
//                Integer sameRegion = null;
                Integer sameMemory = null;
                ResultSet regionEntry = MariaDBAccess.getRegionEntry(region, provider);

                if (regionEntry.next()) {
                    while (similarDeployment.next()) {
                        similar = true;

                        int givenRegionID = regionEntry.getInt("id");
                        int similarRegionID = similarDeployment.getInt("regionID");
                        int similarMemorySize = similarDeployment.getInt("memorySize");

                        if (givenRegionID == similarRegionID && memory == similarMemorySize) {
                            sameRegionAndMemory = similarDeployment.getInt("id");
//                        } else if (givenRegionID == similarRegionID) {
//                            sameRegion = similarDeployment.getInt("id");
                        } else if (memory == similarMemorySize) {
                            sameMemory = similarDeployment.getInt("id");
                        }
                    }
                }
                ResultSet similarResult = null;
                if (sameRegionAndMemory != null) {
                    similarResult = MariaDBAccess.getDeploymentById(sameRegionAndMemory);
                    similarResult.next();
                    // TODO
                    long rtt = (long) similarResult.getDouble("avgRTT");
                    double cost = similarResult.getDouble("avgCost");
                    SimulationParameters.workflowCost += cost;
                    averageLoopCounter = similarResult.getInt("avgLoopCounter");
                    if (concurrencyOverhead != 0 && averageLoopCounter != 0) {
                        rtt -= (long) concurrencyOverhead * averageLoopCounter;
                    }
                    if (loopCounter != -1 && concurrencyOverhead != 0) {
                        rtt += (long) loopCounter * concurrencyOverhead;
                    }
                    rtt = SimulationModel.applyDistribution(rtt, success);
                    result = new PairResult<>(rtt, cost);
                } else if (sameMemory != null) {
                    similarResult = MariaDBAccess.getDeploymentById(sameMemory);
                    similarResult.next();
                    SimulationModel model = new SimulationModel(similarResult, provider, region, memory, loopCounter);
                    result = model.simulateRoundTripTime(success);
//                } else if (sameRegion != null) {
//                    similarResult = MariaDBAccess.getDeploymentById(sameRegion);
//                    similarResult.next();
//                    SimulationModel model = new SimulationModel(similarResult, provider, region, memory, loopCounter);
//                    result = model.simulateRoundTripTime(success);
                } else {
                    similar = false;
                }
            }

            if (!similar) {
                // simulate
                SimulationModel model = new SimulationModel(entry, provider, region, memory, loopCounter);
                result = model.simulateRoundTripTime(success);
            }
        }
        return result;
    }

    /**
     * Retrieves the simValue from the yaml file if present in the properties of the data-outs of the function or
     * returns default values.
     *
     * @return the output of the function
     */
    private Map<String, Object> getFunctionOutput() {
        // TODO read default values from a config file?
        HashMap<String, Object> outputs = new HashMap<>();
        for (DataOutsAtomic out : output) {
            if (out.getProperties() != null && !out.getProperties().isEmpty()) {
                for (PropertyConstraint constraint : out.getProperties()) {
                    if (constraint.getName().equals("simValue")) {
                        parseOutputValues(out, constraint, outputs, false);
                    }
                }
            } else {
                // if no properties are set, fill with default values
                parseOutputValues(out, null, outputs, true);
            }
        }

        return outputs;
    }

    /**
     * Adapted from FunctionNode's method getValuesParsed to parse output values. The default values for the output are:
     * Number: 1 String: "" Collection: [] Boolean: False
     *
     * @param out        the DataOutsAtomic
     * @param constraint the constraint of a DataOutsAtomic
     * @param outputs    the map to put the results
     * @param useDefault if it is set, a default value is used for the output
     */
    private void parseOutputValues(DataOutsAtomic out, PropertyConstraint constraint, HashMap<String, Object> outputs, boolean useDefault) {
        String numStr = null;
        if (!useDefault) {
            numStr = constraint.getValue();
        }
        switch (out.getType()) {
            case "number":
                Number num = null;
                if (useDefault) {
                    num = 1;
                } else if (numStr != null && numStr.matches("[0-9.]+")) {
                    if (numStr.contains(".")) {
                        num = Double.parseDouble(numStr);
                    } else {
                        num = Integer.parseInt(numStr);
                    }
                } else {
                    throw new NumberFormatException("Given value is not a number.");
                }
                outputs.put(name + "/" + out.getName(), num);
                break;
            case "string":
                if (useDefault) {
                    outputs.put(name + "/" + out.getName(), "");
                } else {
                    outputs.put(name + "/" + out.getName(), JsonParser.parseString(constraint.getValue()));
                }
                break;
            case "collection":
                if (useDefault) {
                    outputs.put(name + "/" + out.getName(), JsonParser.parseString("[]").getAsJsonArray());
                } else {
                    // array stays array to later decide which type
                    outputs.put(name + "/" + out.getName(), JsonParser.parseString(numStr).getAsJsonArray());
                }
                break;
            case "bool":
                if (useDefault) {
                    outputs.put(name + "/" + out.getName(), Boolean.FALSE);
                } else {
                    outputs.put(name + "/" + out.getName(), Boolean.valueOf(constraint.getValue()));
                }
                break;
            default:
                logger.error("Error while trying to parse key in function {}. Type: {}", name, out.getType());
                break;
        }
    }

    /**
     * Simulates whether the function returns as expected or yields an error. If the parameter IGNORE_FT in {@link
     * SimulationParameters} is true, it always returns true.
     *
     * @param entry the entry from the database
     *
     * @return true if function simulation is successful, false otherwise
     */
    private Boolean simulateOutcome(ResultSet entry) throws SQLException {
        if (SimulationParameters.IGNORE_FT) {
            return true;
        }
        double successRate = 0;
        successRate = entry.getDouble("successRate");
        // get a random double between 0 and 1
        Random random = new Random();
        double randomValue = random.nextDouble();

        // if the random value is smaller than the success rate, the invocation was successful
        return randomValue < successRate;
    }

    /**
     * Returns the RTT, output and success of the simulation of a function.
     *
     * @param resourceLink the url of the function to simulate
     *
     * @return a TripleResult containing the RTT, output and success of the simulated function
     */
    private QuadrupleResult<Long, Double, Map<String, Object>, Boolean> getSimulationResult(String resourceLink) throws NoDatabaseEntryForIdException, NotYetInvokedException, SQLException, RegionDetectionException, at.uibk.dps.exceptions.RegionDetectionException, MissingComputationalWorkException, MissingSimulationParametersException {
        ResultSet entry = MariaDBAccess.getFunctionIdEntry(resourceLink);

        if (!entry.next()) {
            // TODO change message?
            throw new NoDatabaseEntryForIdException("No entry for '" + resourceLink + "' found. Make sure the resource link is correct and the "
                    + "function has been added to the database.");
        }

        // TODO check if invocations > 0, maybe perform manual update here?
        if (entry.getInt("invocations") == 0) {
            ManualUpdate.main(null);
            entry = MariaDBAccess.getFunctionIdEntry(resourceLink);
            entry.next();
            if (entry.getInt("invocations") == 0) {
                ResultSet implementation = MariaDBAccess.getImplementationById(entry.getInt("functionImplementation_id"));
                implementation.next();
                if (implementation.getDouble("computationWork") == 0) {
                    // TODO change message?
                    throw new NotYetInvokedException("The function with id '" + resourceLink + "' has not been executed yet and " +
                            "no computation work is given for the function implementation. Either execute the function at least " +
                            "once or enter the computation work (in million instructions).");
                }
            }
        }

        Boolean success = simulateOutcome(entry);
        PairResult<Long, Double> result = calculateRoundTripTime(entry, success);
        return new QuadrupleResult<>(result.getRtt(), result.getCost(), getFunctionOutput(), success);
    }

    /**
     * Checks if the current node is within a parallelFor.
     *
     * @return true if it is within a parallelFor, false otherwise
     */
    private boolean inLoop() {
        return loopCounter != -1;
    }

    /**
     * Returns the starting time of a function depending on the already executed functions of the workflow.
     *
     * @return the start time in milliseconds
     */
    private long getStartingTime() throws MissingResourceLinkException, SQLException {
        long startTime = 0;
        if (loopCounter == -1) {
            startTime = MongoDBAccess.getLastEndDateOverall();
        } else {
            String resourceLink = Utils.getResourceLink(properties, this);
            Provider provider = Utils.detectProvider(resourceLink);
            ResultSet providerEntry = MariaDBAccess.getProviderEntry(provider);
            providerEntry.next();
            int maxConcurrency = providerEntry.getInt("maxConcurrency");
            if (loopCounter > maxConcurrency - 1) {
                startTime = MongoDBAccess.getFirstAvailableStartTime(resourceLink);
            } else {
                startTime = MongoDBAccess.getLastEndDateOutOfLoop();
            }
        }
        if (startTime == 0) {
            startTime = System.currentTimeMillis();
        }
        return startTime;
    }
}
