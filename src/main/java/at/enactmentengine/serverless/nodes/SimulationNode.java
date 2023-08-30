package at.enactmentengine.serverless.nodes;

import at.enactmentengine.serverless.exception.*;
import at.enactmentengine.serverless.object.PairResult;
import at.enactmentengine.serverless.object.QuadrupleResult;
import at.enactmentengine.serverless.object.Utils;
import at.enactmentengine.serverless.simulation.ServiceSimulationModel;
import at.enactmentengine.serverless.simulation.SimulationModel;
import at.enactmentengine.serverless.simulation.SimulationParameters;
import at.enactmentengine.serverless.simulation.metadata.MetadataStore;
import at.enactmentengine.serverless.simulation.metadata.model.FunctionDeployment;
import at.enactmentengine.serverless.simulation.metadata.model.FunctionImplementation;
import at.enactmentengine.serverless.simulation.metadata.model.Region;
import at.uibk.dps.afcl.functions.objects.DataIns;
import at.uibk.dps.afcl.functions.objects.DataOutsAtomic;
import at.uibk.dps.afcl.functions.objects.PropertyConstraint;
import at.uibk.dps.cronjob.ManualUpdate;
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
     * The deployment of the Atomic Function.
     */
    private final String deployment;
    /**
     * The constraints for the simulation node.
     */
    private final List<PropertyConstraint> constraints;
    /**
     * The properties of the simulation node.
     */
    private final List<PropertyConstraint> properties;
    /**
     * The id of the current function.
     */
    private int id;
    /**
     * The execution id of the workflow (needed to log the execution).
     */
    private int executionId;
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
     * String containing some info that is used for logging.
     */
    private String simInfo;

    /**
     * Counts the amount of functions that are executed in parallel in a parallel section.
     */
    private long amountParallelFunctions = -1;

    private List<String> serviceStrings;

    /**
     * String containing the times of the simulated services.
     */
    private String serviceOutput;

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
        this.serviceStrings = ServiceSimulationModel.getUsedServices(this.properties);
    }

    /**
     * Extracts the memory size, region, provider and function name of the deployment string.
     *
     * @param deployment to extract the values from
     *
     * @return a list containing 4 elements, the first is the memory size, the second the region, the third the provider
     * and the fourth the function name.
     */
    public static List<String> extractValuesFromDeployment(String deployment) {
        List<String> result = new ArrayList<>();
        String[] parts = deployment.split("_");
        result.add(parts[parts.length - 1]);
        result.add(parts[parts.length - 2]);
        result.add(parts[parts.length - 3].toUpperCase());
        // since the function name could contain underscores as well, we have to concat all remaining elements
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < parts.length - 3; i++) {
            stringBuilder.append(parts[i]).append("_");
        }
        if (stringBuilder.length() > 1) {
            stringBuilder.setLength(stringBuilder.length() - 1);
        }
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
        synchronized (this) {
            id = counter++;
        }

        /* Read the resource link of the base function */
        String resourceLink = Utils.getResourceLink(properties, this);
        Provider provider = Utils.detectProvider(resourceLink);
        String region = Utils.detectRegion(resourceLink);
        Provider deploymentProvider = null;
        String deploymentRegion;
        simInfo = " for provider '" + provider.toString() + "' in region '" + region + "'";
        if (deployment != null) {
            List<String> elements = extractValuesFromDeployment(deployment);
            deploymentRegion = elements.get(1);
            deploymentProvider = Provider.valueOf(elements.get(2));
            simInfo = " for provider '" + deploymentProvider + "' in region '" + deploymentRegion + "'";
        }
        // Check that the provider is either AWS, Google or IBM
        if ((provider != Provider.AWS && provider != Provider.GOOGLE && provider != Provider.IBM) ||
                (deploymentProvider != null && deploymentProvider != Provider.AWS && deploymentProvider != Provider.GOOGLE &&
                        deploymentProvider != Provider.IBM)) {
            throw new Exception("Simulating is currently only supported for AWS, Google and IBM.");
        }
        // check that the provider of the given function and the provider to simulate is the same
        if (deploymentProvider != null && provider != deploymentProvider) {
            throw new Exception("Simulating across providers is currently not supported.");
        }
        // if the function is within a parallelFor, add the loopId to the logs
        String loopId = "";
        if (inLoop()) {
            loopId = ", loopId=" + loopCounter;
        }
        simInfo += ", id=" + id + loopId;
        logger.info("Simulating function " + name + " at resource: " + resourceLink + simInfo);

        /* Parse function with optional constraints and properties */
        Function functionToInvoke = Utils.parseFTConstraints(resourceLink, null, constraints, type, name, loopCounter);

        // parseFTConstraints returns null if there are no constraints set
        // since it will be executed without FT, only the resourceLink is needed
        if (functionToInvoke == null) {
            functionToInvoke = new Function(resourceLink, name, type, loopCounter, null);
        }
        functionToInvoke.setDeployment(deployment);

        /* Simulate function */
        QuadrupleResult<Long, Double, Map<String, Object>, Boolean> simResult = simulateFunction(functionToInvoke);

        // set the result of the simulation as the result of the SimulationNode
        result = simResult.getOutput();

        if (this.input != null) {
            for (DataIns in : this.input) {
                if (in.getPassing() != null && in.getPassing()) {
                    this.output.add(new DataOutsAtomic(in.getName(), in.getType()));
                }
            }
        }

        /* Pass the output to the next node */
        for (Node node : children) {
            node.passResult(result);
            if (getLoopCounter() != -1) {
                node.setLoopCounter(loopCounter);
                node.setMaxLoopCounter(maxLoopCounter);
                node.setConcurrencyLimit(concurrencyLimit);
                node.setStartTime(startTime + simResult.getRTT());
            }
            node.call();
        }

        return true;
    }

    /**
     * Simulates the base function.
     *
     * @param functionToSimulate the base function which should be simulated.
     *
     * @return a QuadrupleResult containing the round trip time, cost, output and success of the simulated function
     *
     * @throws NoDatabaseEntryForIdException        if the given function is not in the database
     * @throws NotYetInvokedException               if the given function has not been invoked
     * @throws InvokationFailureException           if the invocation has failed
     * @throws LatestFinishingTimeException         if the latest finishing time has passed
     * @throws LatestStartingTimeException          if the latest starting time has passed
     * @throws MaxRunningTimeException              if the function runtime is larger than the specified max. running
     *                                              time
     * @throws SQLException                         if an error occurs when reading fields from a database entry
     * @throws RegionDetectionException             if detecting the region from the resource link fails
     * @throws MissingResourceLinkException         if no resource link is given
     * @throws MissingComputationalWorkException    when the field computationWork for the functionImplementation is not
     *                                              filled
     * @throws MissingSimulationParametersException if not all required fields are filled in in the database
     * @throws AlternativeStrategyException         if simulating the alternative strategy fails
     */
    private QuadrupleResult<Long, Double, Map<String, Object>, Boolean> simulateFunction(Function functionToSimulate)
            throws NoDatabaseEntryForIdException, NotYetInvokedException, InvokationFailureException, LatestFinishingTimeException,
            LatestStartingTimeException, MaxRunningTimeException, SQLException, RegionDetectionException, MissingResourceLinkException,
            MissingComputationalWorkException, MissingSimulationParametersException, AlternativeStrategyException {
        String resourceLink = functionToSimulate.getUrl();
        QuadrupleResult<Long, Double, Map<String, Object>, Boolean> result;

        // check if the function should be simulated with fault tolerance
        if (functionToSimulate.hasConstraintSet() || functionToSimulate.hasFTSet()) {
            // simulate with FT
            logger.info("Simulating function with fault tolerance...");
            result = simulateFunctionFT(functionToSimulate);

        } else {
            startTime = startTime == 0 ? getStartingTime() : startTime;
            result = getSimulationResult(resourceLink, functionToSimulate.getDeployment());
            Event event = null;
            if (result.isSuccess()) {
                event = Event.FUNCTION_END;
                logger.info("Simulating function {} took {}ms{}.", resourceLink, result.getRTT(), simInfo);
            } else {
                event = Event.FUNCTION_FAILED;
                logger.info("Simulating function {} failed{}.", resourceLink, simInfo);
            }
            MongoDBAccess.saveLog(event, resourceLink, functionToSimulate.getDeployment(), getName(), functionToSimulate.getType(), this.serviceOutput,
                    result.getRTT(), result.getCost(), result.isSuccess(), loopCounter, maxLoopCounter, startTime, Type.SIM);
        }

        return result;
    }

    /**
     * Simulates a function with FT.
     *
     * @param function to simulate
     *
     * @return a QuadrupleResult containing the round trip time, cost, output and success of the simulated function
     *
     * @throws LatestStartingTimeException          if the latest starting time has passed
     * @throws InvokationFailureException           if the invocation has failed
     * @throws LatestFinishingTimeException         if the latest finishing time has passed
     * @throws MaxRunningTimeException              if the function runtime is larger than the specified max. running
     *                                              time
     * @throws NoDatabaseEntryForIdException        if the given function is not in the database
     * @throws NotYetInvokedException               if the given function has not been invoked
     * @throws SQLException                         if an error occurs when reading fields from a database entry
     * @throws RegionDetectionException             if detecting the region from the resource link fails
     * @throws MissingResourceLinkException         if no resource link is given
     * @throws MissingComputationalWorkException    when the field computationWork for the functionImplementation is not
     *                                              filled
     * @throws AlternativeStrategyException         if simulating the alternative strategy fails
     * @throws MissingSimulationParametersException if not all required fields are filled in in the database
     */
    private QuadrupleResult<Long, Double, Map<String, Object>, Boolean> simulateFunctionFT(Function function)
            throws LatestStartingTimeException, InvokationFailureException, LatestFinishingTimeException, MaxRunningTimeException,
            NoDatabaseEntryForIdException, NotYetInvokedException, SQLException, RegionDetectionException, MissingResourceLinkException,
            MissingComputationalWorkException, AlternativeStrategyException, MissingSimulationParametersException {
        QuadrupleResult<Long, Double, Map<String, Object>, Boolean> quadrupleResult;

        if (function != null) {
            if (function.hasConstraintSet()) {
                Timestamp timeAtStart = new Timestamp(startTime == 0 ? getStartingTime() : startTime);
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
                // no constraints
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
     * @return a QuadrupleResult containing the round trip time, cost, output and success of the simulated function
     *
     * @throws NoDatabaseEntryForIdException        if the given function is not in the database
     * @throws NotYetInvokedException               if the given function has not been invoked
     * @throws SQLException                         if an error occurs when reading fields from a database entry
     * @throws RegionDetectionException             if detecting the region from the resource link fails
     * @throws MissingResourceLinkException         if no resource link is given
     * @throws MissingComputationalWorkException    when the field computationWork for the functionImplementation is not
     *                                              filled
     * @throws AlternativeStrategyException         if simulating the alternative strategy fails
     * @throws MissingSimulationParametersException if not all required fields are filled in in the database
     */
    private QuadrupleResult<Long, Double, Map<String, Object>, Boolean> simulateFT(Function function)
            throws NoDatabaseEntryForIdException, NotYetInvokedException, SQLException, RegionDetectionException,
            MissingResourceLinkException, MissingComputationalWorkException, AlternativeStrategyException, MissingSimulationParametersException {
        startTime = startTime == 0 ? getStartingTime() : startTime;
        String resourceLink = function.getUrl();
        QuadrupleResult<Long, Double, Map<String, Object>, Boolean> result = getSimulationResult(resourceLink, function.getDeployment());

        if (!result.isSuccess()) {
            logger.info("Simulating function {} failed{}.", resourceLink, simInfo);
            MongoDBAccess.saveLog(Event.FUNCTION_FAILED, resourceLink, function.getDeployment(), getName(), function.getType(), null, result.getRTT(),
                    result.getCost(), result.isSuccess(), loopCounter, maxLoopCounter, startTime, Type.SIM);
            if (function.hasFTSet()) {
                logger.info("##############  First invocation has failed, retrying " + function.getFTSettings().getRetries() +
                        " times.  ##############");
                for (int i = 0; i < function.getFTSettings().getRetries(); i++) {
                    // increment the starting time by the previous RTT
                    startTime += result.getRTT();
                    result = getSimulationResult(resourceLink, function.getDeployment());
                    if (result.isSuccess()) {
                        logger.info("Simulating function {} took {}ms{}.", resourceLink, result.getRTT(), simInfo);
                        MongoDBAccess.saveLog(Event.FUNCTION_END, resourceLink, function.getDeployment(), getName(), function.getType(), null, result.getRTT(),
                                result.getCost(), result.isSuccess(), loopCounter, maxLoopCounter, startTime, Type.SIM);
                        return result;
                    }
                    logger.info("Simulating function {} failed{}.", resourceLink, simInfo);
                    MongoDBAccess.saveLog(Event.FUNCTION_FAILED, resourceLink, function.getDeployment(), getName(), function.getType(), null, result.getRTT(),
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
        logger.info("Simulating function {} took {}ms{}.", resourceLink, result.getRTT(), simInfo);
        MongoDBAccess.saveLog(Event.FUNCTION_END, resourceLink, function.getDeployment(), getName(), function.getType(), null, result.getRTT(),
                result.getCost(), result.isSuccess(), loopCounter, maxLoopCounter, startTime, Type.SIM);
        return result;
    }

    /**
     * Simulates the alternativeStrategy of a given function.
     *
     * @param function to simulate the alternativeStrategy
     *
     * @return a QuadrupleResult containing the round trip time, cost, output and success of the simulated function
     *
     * @throws NoDatabaseEntryForIdException        if the given function is not in the database
     * @throws NotYetInvokedException               if the given function has not been invoked
     * @throws AlternativeStrategyException         if simulating the alternative strategy fails
     * @throws SQLException                         if an error occurs when reading fields from a database entry
     * @throws RegionDetectionException             if detecting the region from the resource link fails
     * @throws MissingResourceLinkException         if no resource link is given
     * @throws MissingComputationalWorkException    when the field computationWork for the functionImplementation is not
     *                                              filled
     * @throws MissingSimulationParametersException if not all required fields are filled in in the database
     */
    private QuadrupleResult<Long, Double, Map<String, Object>, Boolean> simulateAlternativeStrategy(Function function)
            throws NoDatabaseEntryForIdException, NotYetInvokedException, AlternativeStrategyException, SQLException,
            RegionDetectionException, MissingResourceLinkException, MissingComputationalWorkException, MissingSimulationParametersException {

        if (function.getFTSettings().getAltStrategy() != null) {
            int i = 0;
            for (List<Function> alternativePlan : function.getFTSettings().getAltStrategy()) {
                // create a map to store the intermediate results
                HashMap<String, QuadrupleResult<Long, Double, Map<String, Object>, Boolean>> tempResults = new HashMap<>();
                List<String> tempDeployments = new ArrayList<>();
                QuadrupleResult<Long, Double, Map<String, Object>, Boolean> result;
                startTime = startTime == 0 ? getStartingTime() : startTime;
                int j = 0;
                logger.info("##############  Trying Alternative Plan " + i + "  ##############");
                for (Function alternativeFunction : alternativePlan) {
                    logger.info("##############  Trying Alternative Function " + j + "  ##############");
                    result = getSimulationResult(alternativeFunction.getUrl(), alternativeFunction.getDeployment());
                    tempResults.put(alternativeFunction.getUrl(), result);
                    tempDeployments.add(alternativeFunction.getDeployment());
                    j++;
                }
                // go through all executed alternative functions to determine which of the successful ones was the fastest
                result = null;
                String url = null;
                String depl = null;
                j = 0;
                for (Map.Entry<String, QuadrupleResult<Long, Double, Map<String, Object>, Boolean>> set : tempResults.entrySet()) {
                    if (set.getValue().isSuccess()) {
                        // check if result is null or if the RTT in the result is greater than the current RTT
                        if (result == null || (result.getRTT() > set.getValue().getRTT())) {
                            result = set.getValue();
                            url = set.getKey();
                            depl = tempDeployments.get(j);
                        }
                    }
                    j++;
                }

                List<String> elements;
                String alternateInfo;
                String loopId = "";
                if (inLoop()) {
                    loopId = ", loopId=" + loopCounter;
                }
                j = 0;
                // check if at least one function simulated successfully
                if (result != null) {
                    // go through the executed functions to log that they were "canceled"
                    for (Map.Entry<String, QuadrupleResult<Long, Double, Map<String, Object>, Boolean>> set : tempResults.entrySet()) {
                        elements = extractValuesFromDeployment(tempDeployments.get(j));
                        alternateInfo = " for provider '" + elements.get(2) + "' in region '" + elements.get(1) + "', id=" + id + loopId;

                        if (!set.getKey().equals(url) && (set.getValue().getRTT() >= result.getRTT())) {
                            // they were "canceled" after the fastest function finished, therefore the RTT of the
                            // result is the RTT of the canceled function
                            logger.info("Canceled simulation of function {} after {}ms{}.", set.getKey(), result.getRTT(), alternateInfo);
                            MongoDBAccess.saveLog(Event.FUNCTION_CANCELED, set.getKey(), tempDeployments.get(j), getName(), function.getType(), null,
                                    result.getRTT(), result.getCost(), false, loopCounter, maxLoopCounter, startTime, Type.SIM);
                        } else if (!set.getValue().isSuccess()) {
                            // if a function was unsuccessful AND it ran shorter than the fastest successful one
                            logger.info("Simulating function {} failed{}.", set.getKey(), alternateInfo);
                            MongoDBAccess.saveLog(Event.FUNCTION_FAILED, set.getKey(), tempDeployments.get(j), getName(), function.getType(), null,
                                    set.getValue().getRTT(), set.getValue().getCost(), set.getValue().isSuccess(), loopCounter, maxLoopCounter, startTime, Type.SIM);
                        }
                        j++;
                    }
                    elements = extractValuesFromDeployment(depl);
                    alternateInfo = " for provider '" + elements.get(2) + "' in region '" + elements.get(1) + "', id=" + id + loopId;
                    // log the fastest successful function
                    logger.info("Simulating function {} took {}ms{}.", url, result.getRTT(), alternateInfo);
                    MongoDBAccess.saveLog(Event.FUNCTION_END, url, depl, getName(), function.getType(), null, result.getRTT(),
                            result.getCost(), result.isSuccess(), loopCounter, maxLoopCounter, startTime, Type.SIM);
                    return result;
                } else {
                    // no function was successful, log their failures
                    for (Map.Entry<String, QuadrupleResult<Long, Double, Map<String, Object>, Boolean>> set : tempResults.entrySet()) {
                        elements = extractValuesFromDeployment(tempDeployments.get(j));
                        alternateInfo = " for provider '" + elements.get(2) + "' in region '" + elements.get(1) + "', id=" + id + loopId;

                        logger.info("Simulating function {} failed{}.", set.getKey(), alternateInfo);
                        MongoDBAccess.saveLog(Event.FUNCTION_FAILED, set.getKey(), tempDeployments.get(j), getName(), function.getType(), null,
                                set.getValue().getRTT(), set.getValue().getCost(), set.getValue().isSuccess(), loopCounter, maxLoopCounter, startTime, Type.SIM);
                        j++;
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
     *
     * @throws SQLException             if an error occurs when reading fields from a database entry
     * @throws RegionDetectionException if detecting the region from the resource link fails
     */
    private boolean deploymentsAreTheSame(FunctionDeployment functionDeployment, int memorySize, Provider provider, String region)
            throws SQLException, RegionDetectionException {
        String functionId = functionDeployment.getKmsArn();
        int fdMemorySize = functionDeployment.getMemorySize();
        Provider fdProvider = Utils.detectProvider(functionId);
        String fdRegion = Utils.detectRegion(functionId);

        if (fdProvider != null && fdRegion != null && fdMemorySize != 0) {
            return fdMemorySize == memorySize && fdProvider == provider && fdRegion.equals(region);
        }
        return false;
    }

    /**
     * Checks if there are functionDeployments stored with the same functionImplementation as the given
     * functionDeployment.
     *
     * @param functionDeployment to get the parameters
     *
     * @return entries with the same functionImplementationId
     *
     * @throws SQLException if an error occurs when reading fields from a database entry
     */
    private List<FunctionDeployment> sameImplementationStored(FunctionDeployment functionDeployment) throws SQLException {
        return MetadataStore.getDeploymentsWithImplementationId(functionDeployment.getFunctionImplementationId());
    }

    /**
     * Calculates the RTT of a given function.
     *
     * @param entry            the entry from the database
     * @param success          if the simulation is success or not
     * @param deploymentString the deployment string of the function
     *
     * @return the RTT in ms and cost
     *
     * @throws SQLException                         if an error occurs when reading fields from a database entry
     * @throws RegionDetectionException             if detecting the region from the resource link fails
     * @throws MissingComputationalWorkException    when the field computationWork for the functionImplementation is not
     *                                              filled
     * @throws MissingSimulationParametersException if not all required fields are filled in in the database
     */
    private PairResult<Long, Double> calculateRoundTripTime(FunctionDeployment entry, Boolean success, String deploymentString) throws SQLException,
            RegionDetectionException, MissingComputationalWorkException, MissingSimulationParametersException {
        PairResult<Long, Double> result = null;
        List<String> elements;
        int memory = 0;
        String region = null;
        Provider provider = null;
        int concurrencyOverhead;
        at.enactmentengine.serverless.simulation.metadata.model.Provider providerEntry;

        if (deploymentString != null) {
            elements = extractValuesFromDeployment(deploymentString);
            memory = Integer.parseInt(elements.get(0));
            region = elements.get(1);
            provider = Provider.valueOf(elements.get(2));
            providerEntry = MetadataStore.getProviderEntry(provider);
        } else {
            providerEntry = MetadataStore.getProviderEntry(Utils.detectProvider(entry.getKmsArn()));
        }
        concurrencyOverhead = providerEntry.getConcurrencyOverheadMs();

        // if the deployment is null or deployment is already saved in the MD-DB,
        // simulate in the same region and with the same memory
        if (deploymentString == null || deploymentsAreTheSame(entry, memory, provider, region)) {
            // simply read from the values from the DB without calculating them again
            result = extractRttAndCost(success, concurrencyOverhead, entry);
        } else {
            List<FunctionDeployment> similarDeployments = sameImplementationStored(entry);
            // indicates if a similar deployment was found
            boolean similar = false;

            if (similarDeployments != null && !similarDeployments.isEmpty()) {
                Long sameRegionAndMemory = null;
                Long sameMemory = null;
                Region regionEntry = MetadataStore.getRegionEntry(region, provider);

                for (FunctionDeployment similarDeployment : similarDeployments) {
                    similar = true;

                    int givenRegionID = regionEntry.getId();
                    long similarRegionID = similarDeployment.getRegionId();
                    int similarMemorySize = similarDeployment.getMemorySize();

                    if (givenRegionID == similarRegionID && memory == similarMemorySize) {
                        sameRegionAndMemory = similarDeployment.getId();
                    } else if (memory == similarMemorySize) {
                        sameMemory = similarDeployment.getId();
                    }
                }

                FunctionDeployment similarResult;
                if (sameRegionAndMemory != null) {
                    similarResult = MetadataStore.getDeploymentById(sameRegionAndMemory);
                    result = extractRttAndCost(success, concurrencyOverhead, similarResult);
                } else if (sameMemory != null) {
                    // always prefer the given entry if they have the same memory size
                    if (memory == entry.getMemorySize()) {
                        sameMemory = entry.getId();
                    }
                    similarResult = MetadataStore.getDeploymentById(sameMemory);
                    SimulationModel model = new SimulationModel(similarResult, provider, region, memory, loopCounter);
                    result = model.simulateRoundTripTime(success);
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

        // simulate external services
        if(!serviceStrings.isEmpty()) {
            jFaaS.utils.PairResult<String, Long> simResult = null;

            if (region == null) {
                simResult = ServiceSimulationModel.calculateTotalRttForUsedServices(entry.getRegionId().intValue(), serviceStrings);
            } else {
                simResult = ServiceSimulationModel.calculateTotalRttForUsedServices(entry.getRegionId().intValue(), region, serviceStrings);
            }

            result.setRtt(result.getRtt() + simResult.getRTT());
            this.serviceOutput = simResult.getResult();
        }

        return result;
    }

    /**
     * Extracts the rtt and cost of the given entry.
     *
     * @param success             if the simulation is successful
     * @param concurrencyOverhead of the provider of the function
     * @param entry               to extract the values
     *
     * @return a PairResult containing the rtt and cost
     *
     * @throws SQLException if an error occurs when reading fields from a database entry
     */
    private PairResult<Long, Double> extractRttAndCost(Boolean success, int concurrencyOverhead, FunctionDeployment entry) throws SQLException {
        long rtt = entry.getAvgRTT().longValue();
        double cost = entry.getAvgCost();
        int averageLoopCounter = entry.getAvgLoopCounter();

        if (concurrencyOverhead != 0 && averageLoopCounter != 0) {
            rtt -= (long) concurrencyOverhead * averageLoopCounter;
        }
        if (loopCounter != -1 && concurrencyOverhead != 0) {
            rtt += (long) loopCounter * concurrencyOverhead;
        }

        rtt = SimulationModel.applyDistribution(rtt, success);
        SimulationParameters.workflowCost += cost;
        return new PairResult<>(rtt, cost);
    }

    /**
     * Retrieves the simValue from the yaml file if present in the properties of the data-outs of the function or
     * returns default values.
     *
     * @return the output of the function
     */
    private Map<String, Object> getFunctionOutput() {
        HashMap<String, Object> outputs = new HashMap<>();
        for (DataOutsAtomic out : new ArrayList<>(output)) {
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

        if (this.input != null) {
            for (DataIns in : new ArrayList<>(this.input)) {
                if (in.getPassing() != null && in.getPassing()) {
                    parseOutputValues(new DataOutsAtomic(in.getName(), in.getType()), null, outputs, true);
                }
            }
        }

        return outputs;
    }

    /**
     * Adapted from FunctionNode's method getValuesParsed to parse output values. The default values for the output are:
     * Number: 1, String: "", Collection: [], Boolean: False
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
                Number num;
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
     * Simulates whether the function returns as expected or yields an error. If the parameter IGNORE_FT in
     * {@link SimulationParameters} is true, it always returns true.
     *
     * @param entry the entry from the database
     *
     * @return true if function simulation is successful, false otherwise
     *
     * @throws SQLException if an error occurs when reading fields from a database entry
     */
    private Boolean simulateOutcome(FunctionDeployment entry) throws SQLException {
        if (SimulationParameters.IGNORE_FT) {
            return true;
        }
        double successRate = entry.getSuccessRate();
        // get a random double between 0 and 1
        Random random = new Random();
        double randomValue = random.nextDouble();

        // if the random value is smaller than the success rate, the invocation was successful
        return randomValue < successRate;
    }

    /**
     * Returns the RTT, output and success of the simulation of a function.
     *
     * @param resourceLink     the url of the function to simulate
     * @param deploymentString the deployment string for the function
     *
     * @return a QuadrupleResult containing the round trip time, cost, output and success of the simulated function
     *
     * @throws NoDatabaseEntryForIdException        if the given function is not in the database
     * @throws NotYetInvokedException               if the given function has not been invoked
     * @throws SQLException                         if an error occurs when reading fields from a database entry
     * @throws RegionDetectionException             if detecting the region from the resource link fails
     * @throws MissingComputationalWorkException    when the field computationWork for the functionImplementation is not
     *                                              filled
     * @throws MissingSimulationParametersException if not all required fields are filled in in the database
     */
    private QuadrupleResult<Long, Double, Map<String, Object>, Boolean> getSimulationResult(String resourceLink, String deploymentString)
            throws NoDatabaseEntryForIdException, NotYetInvokedException, SQLException, RegionDetectionException,
            MissingComputationalWorkException, MissingSimulationParametersException {
        FunctionDeployment fd = MetadataStore.getFunctionIdEntry(resourceLink);

        if (fd.getInvocations() == 0) {
            logger.info("Refreshing database to check for an invocation for '" + resourceLink + "'. This could take a moment.");
            ManualUpdate.main(null);
            fd = MetadataStore.getFunctionIdEntry(resourceLink);
            if (fd.getInvocations() == 0) {
                FunctionImplementation fi = MetadataStore.getImplementationById(fd.getFunctionImplementationId());
                if (fi.getComputationWork() == 0) {
                    throw new NotYetInvokedException("The function with id '" + resourceLink + "' has not been executed yet and " +
                            "no computation work is given for the function implementation. Either execute the function at least " +
                            "once or enter the computation work (in million instructions).");
                }
            }
        }

        Boolean success = simulateOutcome(fd);
        PairResult<Long, Double> result = calculateRoundTripTime(fd, success, deploymentString);
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
     *
     * @throws MissingResourceLinkException if no resource link is given
     * @throws SQLException                 if an error occurs when reading fields from a database entry
     */
    private long getStartingTime() throws MissingResourceLinkException, SQLException {
        long start;

        if (loopCounter == -1) {
            start = MongoDBAccess.getLastEndDateOverall();
        } else {
            String resourceLink = Utils.getResourceLink(properties, this);
            Provider provider = Utils.detectProvider(resourceLink);
            at.enactmentengine.serverless.simulation.metadata.model.Provider providerEntry = MetadataStore.getProviderEntry(provider);
            int maxConcurrency = providerEntry.getMaxConcurrency();
            if (loopCounter > maxConcurrency - 1 || (concurrencyLimit != -1 && loopCounter > concurrencyLimit - 1)) {
                start = -1;
                while (start == -1) {
                    start = SimulationParameters.getStartTime(amountParallelFunctions, loopCounter, null);
                    if (startTime != 0) {
                        start = startTime;
                    }
                    // wait to give other threads to opportunity to access the synchronized method
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                start = MongoDBAccess.getLastEndDateOutOfLoop();
            }
        }
        if (start == 0) {
            start = System.currentTimeMillis();
        }
        return start;
    }

    public long getAmountParallelFunctions() {
        return amountParallelFunctions;
    }

    public synchronized void setAmountParallelFunctions(long amountParallelFunctions) {
        this.amountParallelFunctions = amountParallelFunctions;
    }
}
