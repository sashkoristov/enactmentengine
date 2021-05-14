package at.enactmentengine.serverless.nodes;

import at.enactmentengine.serverless.exception.AlternativeStrategyException;
import at.enactmentengine.serverless.exception.NoDatabaseEntryForIdException;
import at.enactmentengine.serverless.exception.NotYetInvokedException;
import at.enactmentengine.serverless.object.TripleResult;
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
     * The number of execution in a parallelFor loop.
     */
    private int loopCounter = -1;

    /**
     * The execution id of the workflow (needed to log the execution).
     */
    private int executionId;

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
     * The memory size of the function.
     */
    private Integer memorySize = null; //TODO

    /**
     * Constructor for a simulation node.
     *
     * @param name        of the base function.
     * @param type        of the base function (fType).
     * @param properties  of the base function.
     * @param constraints of the base function.
     * @param input       to the base function.
     * @param output      of the base function.
     * @param executionId for the logging of the simulation.
     */
    public SimulationNode(String name, String type, List<PropertyConstraint> properties, List<PropertyConstraint> constraints,
                          List<DataIns> input, List<DataOutsAtomic> output, int executionId) {
        super(name, type);
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
        TripleResult<Long, Map<String, Object>, Boolean> simResult = simulateFunction(functionToInvoke);
        long end = System.currentTimeMillis();

        // TODO as in FunctionNode, log function output by getting the values from the mdDB?
        // logFunctionOutput(start, end, resultString, id);

        result = simResult.getOutput();

        /* Pass the output to the next node */
        for (Node node : children) {
            node.passResult(result);
            if (node instanceof SimulationNode && loopCounter != -1) {
                ((SimulationNode) node).setLoopCounter(loopCounter);
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
    private TripleResult<Long, Map<String, Object>, Boolean> simulateFunction(Function functionToSimulate) throws NoDatabaseEntryForIdException, NotYetInvokedException {
        // TODO simulate here
        String resourceLink = functionToSimulate.getUrl();
        TripleResult<Long, Map<String, Object>, Boolean> result = null;

        // check if the function should be simulated with fault tolerance
        if (functionToSimulate.hasConstraintSet() || functionToSimulate.hasFTSet()) {
            // simulate with FT
            logger.info("Simulating function with fault tolerance...");
            // TODO create separate class?
            try {
                result = simulateFunctionFT(functionToSimulate);
            } catch (LatestStartingTimeException | InvokationFailureException | LatestFinishingTimeException | MaxRunningTimeException e) {
                e.printStackTrace();
            }

        } else {
            long startTime = getStartingTime();
            result = getSimulationResult(resourceLink);
            if (result.isSuccess()) {
                logger.info("Simulating function {} took {}ms.", resourceLink, result.getRTT());
            } else {
                logger.info("Simulating function {} failed.", resourceLink);
            }
            MongoDBAccess.saveLog(Event.FUNCTION_END, resourceLink, getName(), functionToSimulate.getType(), null, result.getRTT(), result.isSuccess(), memorySize, loopCounter, startTime, Type.SIM);
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
    private TripleResult<Long, Map<String, Object>, Boolean> simulateFunctionFT(Function function) throws LatestStartingTimeException, InvokationFailureException, LatestFinishingTimeException, MaxRunningTimeException, NoDatabaseEntryForIdException, NotYetInvokedException {
        TripleResult<Long, Map<String, Object>, Boolean> tripleResult;

        if (function != null) {
            if (function.hasConstraintSet()) {
                Timestamp timeAtStart = new Timestamp(getStartingTime());
                if (function.getConstraints().hasLatestStartingTime()) {
                    if (timeAtStart.after(function.getConstraints().getLatestStartingTime())) {
                        throw new LatestStartingTimeException("latestStartingTime constraint missed!");
                    }
                    if (!function.getConstraints().hasLatestFinishingTime()
                            && !function.getConstraints().hasMaxRunningTime()) {
                        tripleResult = simulateFT(function);

                        if (!tripleResult.isSuccess()) {
                            throw new InvokationFailureException("Invocation has failed");
                        } else {
                            return tripleResult;
                        }
                    }
                }

                tripleResult = simulateFT(function);

                if (tripleResult.isSuccess()) {
                    // check maxRunningTime
                    Timestamp newTime = new Timestamp(timeAtStart.getTime() + tripleResult.getRTT());

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
                    return tripleResult;
                } else {
                    throw new InvokationFailureException("Invocation has failed alter entire alternative strategy");
                }
            } else {
                // no constraints. Just invoke in current thread. (we do not need to cancel)
                tripleResult = simulateFT(function);
                if (!tripleResult.isSuccess()) {
                    throw new InvokationFailureException("Invocation has failed");
                } else {
                    return tripleResult;
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
    private TripleResult<Long, Map<String, Object>, Boolean> simulateFT(Function function) throws NoDatabaseEntryForIdException, NotYetInvokedException {
        long startTime = getStartingTime();
        String resourceLink = function.getUrl();
        TripleResult<Long, Map<String, Object>, Boolean> result = getSimulationResult(resourceLink);

        if (!result.isSuccess()) {
            logger.info("Simulating function {} failed.", resourceLink);
            MongoDBAccess.saveLog(Event.FUNCTION_FAILED, resourceLink, getName(), function.getType(), null, result.getRTT(), result.isSuccess(), memorySize, loopCounter, startTime, Type.SIM);
            if (function.hasFTSet()) {
                logger.info("##############  First invocation has failed, retrying " + function.getFTSettings().getRetries() +
                        " times.  ##############");
                for (int i = 0; i < function.getFTSettings().getRetries(); i++) {
                    // increment the starting time by the previous RTT
                    startTime += result.getRTT();
                    result = getSimulationResult(resourceLink);
                    if (result.isSuccess()) {
                        logger.info("Simulating function {} took {}ms.", resourceLink, result.getRTT());
                        MongoDBAccess.saveLog(Event.FUNCTION_END, resourceLink, getName(), function.getType(), null, result.getRTT(), result.isSuccess(), memorySize, loopCounter, startTime, Type.SIM);
                        return result;
                    }
                    logger.info("Simulating function {} failed.", resourceLink);
                    MongoDBAccess.saveLog(Event.FUNCTION_FAILED, resourceLink, getName(), function.getType(), null, result.getRTT(), result.isSuccess(), memorySize, loopCounter, startTime, Type.SIM);
                }
                // Failed after all retries. Check for alternative Strategy
                if (function.getFTSettings().hasAlternativeStartegy()) {
                    try {
                        // AlternativeStrategy has correct Result
                        return simulateAlternativeStrategy(function);
                    } catch (AlternativeStrategyException e) {
                        return new TripleResult<>(null, null, false);
                    }
                } else {
                    // no alternativeStrategy set so failure
                    return new TripleResult<>(null, null, false);
                }
            } else {
                // function failed and no FT is set
                return result;
            }
        }
        logger.info("Simulating function {} took {}ms.", resourceLink, result.getRTT());
        MongoDBAccess.saveLog(Event.FUNCTION_END, resourceLink, getName(), function.getType(), null, result.getRTT(), result.isSuccess(), memorySize, loopCounter, startTime, Type.SIM);
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
    private TripleResult<Long, Map<String, Object>, Boolean> simulateAlternativeStrategy(Function function) throws NoDatabaseEntryForIdException, NotYetInvokedException, AlternativeStrategyException {

        if (function.getFTSettings().getAltStrategy() != null) {
            int i = 0;
            for (List<Function> alternativePlan : function.getFTSettings().getAltStrategy()) {
                // create a map to store the intermediate results
                HashMap<String, TripleResult<Long, Map<String, Object>, Boolean>> tempResults = new HashMap<>();
                TripleResult<Long, Map<String, Object>, Boolean> result = null;
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
                for (Map.Entry<String, TripleResult<Long, Map<String, Object>, Boolean>> set : tempResults.entrySet()) {
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
                    for (Map.Entry<String, TripleResult<Long, Map<String, Object>, Boolean>> set : tempResults.entrySet()) {
                        if (!set.getKey().equals(url) && (set.getValue().getRTT() >= result.getRTT())) {
                            // they were "canceled" after the fastest function finished, therefore the RTT of the
                            // result is the RTT of the canceled function
                            logger.info("Canceled simulation of function {} after {}ms.", set.getKey(), result.getRTT());
                            // TODO which value to set here for success? True or false?
                            MongoDBAccess.saveLog(Event.FUNCTION_CANCELED, set.getKey(), getName(), function.getType(), null, result.getRTT(), set.getValue().isSuccess(), memorySize, loopCounter, startTime, Type.SIM);
                        } else if (!set.getValue().isSuccess()) {
                            // if a function was unsuccessful AND it ran shorter than the fastest successful one
                            logger.info("Simulating function {} failed.", set.getKey());
                            MongoDBAccess.saveLog(Event.FUNCTION_FAILED, set.getKey(), getName(), function.getType(), null, set.getValue().getRTT(), set.getValue().isSuccess(), memorySize, loopCounter, startTime, Type.SIM);
                        }
                    }
                    // log the fastest successful function
                    logger.info("Simulating function {} took {}ms.", url, result.getRTT());
                    MongoDBAccess.saveLog(Event.FUNCTION_END, url, getName(), function.getType(), null, result.getRTT(), result.isSuccess(), memorySize, loopCounter, startTime, Type.SIM);
                    return result;
                } else {
                    // no function was successful, log their failures
                    for (Map.Entry<String, TripleResult<Long, Map<String, Object>, Boolean>> set : tempResults.entrySet()) {
                        logger.info("Simulating function {} failed.", set.getKey());
                        MongoDBAccess.saveLog(Event.FUNCTION_FAILED, set.getKey(), getName(), function.getType(), null, set.getValue().getRTT(), set.getValue().isSuccess(), memorySize, loopCounter, startTime, Type.SIM);
                    }
                }

                i++;
            }
            throw new AlternativeStrategyException("Failed after entire Alternative Strategy");
        }
        throw new AlternativeStrategyException("No alternative Strategy defined");
    }

    /**
     * Calculates the RTT of a given function.
     *
     * @param entry the entry from the database
     *
     * @return the RTT in ms
     */
    private Long calculateRoundTripTime(ResultSet entry) {
        //TODO

        return 1000L;
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
     * Simulates whether the function returns as expected or yields an error.
     *
     * @param entry the entry from the database
     *
     * @return true if function simulation is successful, false otherwise
     */
    private Boolean simulateOutcome(ResultSet entry) {
        // TODO simulate whether the function is successful or yields an error
//        if (resourceLink.endsWith("HW")) {
//            return false;
//        }
        double successRate = 0;
        try {
            successRate = entry.getDouble("successRate");
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
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
    private TripleResult<Long, Map<String, Object>, Boolean> getSimulationResult(String resourceLink) throws NoDatabaseEntryForIdException, NotYetInvokedException {
        ResultSet entry = MariaDBAccess.getFunctionIdEntry(resourceLink);
        try {
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
                    // TODO change message?
                    throw new NotYetInvokedException("The function with id '" + resourceLink + "' has not been executed yet. It has to be executed "
                            + "at least once.");
                }
            }

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return new TripleResult<Long, Map<String, Object>, Boolean>(calculateRoundTripTime(entry), getFunctionOutput(), simulateOutcome(entry));
    }

    public int getLoopCounter() {
        return loopCounter;
    }

    public void setLoopCounter(int loopCounter) {
        this.loopCounter = loopCounter;
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
    private long getStartingTime() {
        long startTime = 0;
        if (loopCounter == -1) {
            startTime = MongoDBAccess.getLastEndDateOverall();
        } else {
            startTime = MongoDBAccess.getLastEndDateOutOfLoop();
        }
        if (startTime == 0) {
            startTime = System.currentTimeMillis();
        }
        return startTime;
    }
}
