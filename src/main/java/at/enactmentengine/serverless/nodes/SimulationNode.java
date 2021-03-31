package at.enactmentengine.serverless.nodes;

import at.enactmentengine.serverless.object.Utils;
import at.uibk.dps.afcl.functions.objects.DataIns;
import at.uibk.dps.afcl.functions.objects.DataOutsAtomic;
import at.uibk.dps.afcl.functions.objects.PropertyConstraint;
import at.uibk.dps.function.Function;
import at.uibk.dps.socketutils.entity.Invocation;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        // TODO read result from metadata DB
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
        logger.info("Simulating function " + name + " at resource: " + resourceLink + " [" + System.currentTimeMillis()
                + "ms], id=" + id);

        // TODO as in FunctionNode, log function inputs by getting the values from the mdDB?
        // logFunctionInput(actualFunctionInputs, id);

        /* Parse function with optional constraints and properties */
        Function functionToInvoke = Utils.parseFTConstraints(resourceLink, null, constraints, type);

        //TODO check if function is stored in metadataDB
        // if not exists then log error and return

        /* Simulate function and measure duration */
        long start = System.currentTimeMillis();
        String resultString = simulateFunction(functionToInvoke, resourceLink);
        long end = System.currentTimeMillis();

        // TODO as in FunctionNode, log function output by getting the values from the mdDB?
        // logFunctionOutput(start, end, resultString, id);

        // TODO get output from mdDB
        Map<String, Object> functionOutputs = new HashMap<>();
        functionOutputs.put("Key1Test", "Value1Test");
        JsonObject jsonResult = Utils.generateJson(resultString, new DataOutsAtomic("testKey", "string"));

        /* Pass the output to the next node */
        for (Node node : children) {
            node.passResult(functionOutputs);
            node.call();
        }

        /* Set the result of the function node */
        result = functionOutputs;

        /*
         * Check if the execution identifier is specified (check if execution should be
         * stored in the database)
         */
        if (executionId != -1) {

            /* Create a function invocation object */
            Invocation functionInvocation = new Invocation(resourceLink, Utils.detectProvider(resourceLink).toString(),
                    Utils.detectRegion(resourceLink),
                    new Timestamp(start + TimeZone.getTimeZone("Europe/Rome").getOffset(start)),
                    new Timestamp(end + TimeZone.getTimeZone("Europe/Rome").getOffset(start)), (end - start),
                    Utils.checkResultSuccess(resultString).toString(), null, executionId);

            /* Store the invocation in the database */
            Utils.storeInDBFunctionInvocation(logger, functionInvocation, executionId);
        }

        return true;
    }

    /**
     * Simulates the base function.
     *
     * @param functionToSimulate the base function which should be invoked.
     * @param resourceLink       the resource of the base function.
     *
     * @return the stringified json result of the base function invocation.
     */
    private String simulateFunction(Function functionToSimulate, String resourceLink) {
        // TODO simulate here
        String resultString;
        resultString = "{\"key2Test\": \"value2Test\"}";
        return resultString;
    }

}
