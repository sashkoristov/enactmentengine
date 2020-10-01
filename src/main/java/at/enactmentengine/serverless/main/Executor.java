package at.enactmentengine.serverless.main;

import at.enactmentengine.serverless.nodes.ExecutableWorkflow;
import at.enactmentengine.serverless.parser.Language;
import at.enactmentengine.serverless.parser.YAMLParser;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Main class of enactment engine which specifies the workflowInput file and starts the
 * workflow on the machine on which it gets started.
 * <p>
 * based on @author markusmoosbrugger, jakobnoeckl
 * extended by @author stefanpedratscher
 */
class Executor {

    /**
     * Logger for executor.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(Executor.class);

    /**
     * Input of the workflow.
     */
    private Map<String, Object> workflowInput;

    /**
     * Default constructor for executor.
     */
    public Executor() {
        workflowInput = new HashMap<>();
    }

    /**
     * workflowResult
     *
     * @param workflow      path to workflow yaml file which should be executed.
     * @param workflowInput path to input json file which should be used as workflow input.
     * @param executionId   the unique identifier for each execution.
     * @return the result of the workflow.
     */
    Map<String, Object> executeWorkflow(String workflow, String workflowInput, int executionId) {
        Map<String, Object> workflowResult = null;

        try {
            /* Convert file content to byte[] and execute the workflow */
            workflowResult = executeWorkflow(
                    workflow == null ? null : FileUtils.readFileToByteArray(new File(workflow)),
                    workflowInput == null ? null : FileUtils.readFileToByteArray(new File(workflowInput)),
                    executionId);
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
     * @return the result of the workflow.
     */
    Map<String, Object> executeWorkflow(byte[] workflow, byte[] workflowInput, int executionId) {

        /* Measure start time of the workflow execution */
        long start = System.currentTimeMillis();

        /* Disable hostname verification (enable OpenWhisk connections) */
        final Properties props = System.getProperties();
        props.setProperty("jdk.internal.httpclient.disableHostnameVerification", Boolean.TRUE.toString());

        /* Get the workflowInput file as argument or default string */
        if (workflow == null) {
            LOGGER.error("Please specify a workflow file");
            return null;
        }

        /* Create an executable workflow */
        ExecutableWorkflow ex = new YAMLParser().parseExecutableWorkflow(workflow, Language.YAML, executionId);

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
                workflowOutput = ex.executeWorkflow(this.workflowInput);
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
                return null;
            }

            /* Measure end time of the workflow execution */
            long end = System.currentTimeMillis();
            LOGGER.info("Execution took {}ms.", (end - start));
        }

        return workflowOutput;
    }
}
