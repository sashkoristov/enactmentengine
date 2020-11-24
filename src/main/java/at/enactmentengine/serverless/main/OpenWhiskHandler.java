package at.enactmentengine.serverless.main;

import at.enactmentengine.serverless.exception.MissingInputDataException;
import at.enactmentengine.serverless.nodes.ExecutableWorkflow;
import at.enactmentengine.serverless.parser.Language;
import at.enactmentengine.serverless.parser.YAMLParser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

/**
 * OpenWhiskHandler to allow the execution of the Enactment Engine as OpenWhisk
 * or IBM Cloud function.
 * <p>
 * based on @author markusmoosbrugger, jakobnoeckl
 * extended by @author stefanpedratscher
 */
public class OpenWhiskHandler {

    /**
     * Logger for the openWhisk handler.
     */
    private static final Logger logger = LoggerFactory.getLogger(OpenWhiskHandler.class);

    /**
     * Key in json output where the output should be placed.
     */
    private static final String RESULT_FIELD = "result";

    /**
     * The language of the workflow (json or yaml).
     */
    private static final String LANGUAGE_FIELD = "language";

    /**
     * Default empty constructor.
     */
    OpenWhiskHandler(){
    }

    /**
     * Starting point of the OpenWhisk action.
     *
     * @param args input of the openWhisk action (enactment-engine).
     * @return the result of the workflow execution.
     */
    public static JsonObject main(JsonObject args) {

        /* Start measuring time for workflow execution */
        long start = System.currentTimeMillis();

        /* Disable hostname verification (enable OpenWhisk connections) */
        final Properties props = System.getProperties();
        props.setProperty("jdk.internal.httpclient.disableHostnameVerification", Boolean.TRUE.toString());

        int executionId = -1;
        JsonObject response = new JsonObject();
        Language language = readLanguage(args);

        if(args != null) {
            if(args.has("workflow")){

                /* Parse the workflow */
                ExecutableWorkflow ex = new YAMLParser().parseExecutableWorkflowByStringContent(args.getAsJsonPrimitive("workflow").getAsString(), language, executionId);
                try {
                    /* Execute the workflow */
                    ObjectMapper objectMapper = new ObjectMapper();
                    Map<String, Object> executionResult = ex.executeWorkflow(objectMapper.readValue(args.getAsJsonPrimitive("input").getAsString(), Map.class));
                    response.addProperty(RESULT_FIELD, String.valueOf(executionResult));
                } catch (MissingInputDataException | ExecutionException | InterruptedException | IOException e) {
                    e.printStackTrace();
                }
            } else {
                response.addProperty(RESULT_FIELD, "Error: Could not run workflow. Request not valid.");
            }
        } else {
            response.addProperty(RESULT_FIELD, "Error: Could not run workflow. Request not specified.");
        }

        /* Stop measuring time for workflow execution */
        long end = System.currentTimeMillis();

        response.addProperty(RESULT_FIELD, "Workflow ran without errors in " + (end - start) + "ms. Start: " + start + ", End: " + end  +
                ". Result: " + response);
        return response;
    }

    /**
     * Read the language of the input object.
     *
     * @param args to read from.
     * @return yaml, json or undefined.
     */
    private static Language readLanguage(JsonObject args) {
        if (args != null && args.has(LANGUAGE_FIELD)) {
            if ("yaml".equals(args.getAsJsonPrimitive(LANGUAGE_FIELD).getAsString())) {
                return Language.YAML;
            } else if ("json".equals(args.getAsJsonPrimitive(LANGUAGE_FIELD).getAsString())) {
                return Language.JSON;
            }
        }
        return Language.NOT_SET;
    }
}
