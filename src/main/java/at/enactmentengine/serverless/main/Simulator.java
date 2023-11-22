package at.enactmentengine.serverless.main;

import at.enactmentengine.serverless.simulation.SimulationParameters;
import at.enactmentengine.serverless.nodes.ExecutableWorkflow;
import at.enactmentengine.serverless.parser.Language;
import at.enactmentengine.serverless.parser.YAMLParser;
import at.uibk.dps.databases.MongoDBAccess;
import at.uibk.dps.util.Event;
import at.uibk.dps.util.Type;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Main class for the simulation part of the enactment engine.
 *
 * based on {@link Executor}, modified by @author mikahautz
 */
public class Simulator {

    /**
     * Logger for executor.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(Simulator.class);

    /**
     * Input of the workflow.
     */
    private Map<String, Object> workflowInput;

    /**
     * Default constructor for Simulator.
     */
    public Simulator() { }

    /**
     * workflowResult
     *
     * @param workflow      path to workflow yaml file which should be executed.
     * @param workflowInput path to input json file which should be used as workflow input.
     * @param executionId   the unique identifier for each execution.
     * @param start         the start time
     *
     * @return the result of the workflow.
     */
    Map<String, Object> simulateWorkflow(String workflow, String workflowInput, int executionId, long start) {
        Map<String, Object> workflowResult = null;

        try {
            /* Convert file content to byte[] and execute the workflow */
            workflowResult = simulateWorkflow(
                    workflow == null ? null : FileUtils.readFileToByteArray(new File(workflow)),
                    workflowInput == null ? null : FileUtils.readFileToByteArray(new File(workflowInput)),
                    executionId, start);
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }

        return workflowResult;
    }

    /**
     * Execute the given workflow.
     *
     * @param workflow      byte[] of the workflow yaml file which should be executed.
     * @param workflowInput byte[] of the input json file which should be used as workflow input.
     * @param executionId   the unique identifier for each execution.
     * @param start         the start time
     *
     * @return the result of the workflow.
     */
    Map<String, Object> simulateWorkflow(byte[] workflow, byte[] workflowInput, int executionId, long start) {

        /* Disable hostname verification (enable OpenWhisk connections) */
        final Properties props = System.getProperties();
        props.setProperty("jdk.internal.httpclient.disableHostnameVerification", Boolean.TRUE.toString());

        /* Get the workflowInput file as argument or default string */
        if (workflow == null) {
            LOGGER.error("Please specify a workflow file");
            return null;
        }

        /* Create an executable workflow */
        ExecutableWorkflow ex = new YAMLParser().parseExecutableWorkflow(workflow, Language.YAML, executionId, true);

        /* Create variable to store workflow output */
        Map<String, Object> workflowOutput = null;

        /* Check if conversion to executable workflow was successful */
        if (ex != null) {

            /* Check of there is a workflow input */
            if (workflowInput != null) {

                /* Decode json workflow input */
                String decodedJsonInput = new String(workflowInput, StandardCharsets.UTF_8);
                this.workflowInput = new Gson().fromJson(decodedJsonInput, new TypeToken<HashMap<String, Object>>() {
                }.getType());
            }

            /* Execute the workflow */
            try {
                workflowOutput = ex.simulateWorkflow(this.workflowInput);
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
                MongoDBAccess.saveLog(Event.WORKFLOW_FAILED, null, null, null, null, null, System.currentTimeMillis() - start,
                        SimulationParameters.workflowCost, false, -1, -1, start, Type.SIM);
                return null;
            }

            long simWorkflowDuration = MongoDBAccess.getLastEndDateOverall() - start;
            boolean success = ex.getEndNode().getResult() != null;
            Event event = success ? Event.WORKFLOW_END : Event.WORKFLOW_FAILED;

            LOGGER.info("Simulation of workflow takes {}ms with a cost of {}.", simWorkflowDuration, SimulationParameters.workflowCost);
            MongoDBAccess.saveLog(event, null, null, null, null, null, simWorkflowDuration, SimulationParameters.workflowCost, success, -1, -1, start, Type.SIM);
        }

        return workflowOutput;
    }

}
