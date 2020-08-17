package at.enactmentengine.serverless.main;

import at.enactmentengine.serverless.nodes.ExecutableWorkflow;
import at.enactmentengine.serverless.parser.Language;
import at.enactmentengine.serverless.parser.YAMLParser;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * OpenWhiskHandler to allow the execution of the Enactment Engine as OpenWhisk
 * or IBM Cloud function.
 * <p>
 * based on @author markusmoosbrugger, jakobnoeckl
 * extended by @author stefanpedratscher
 */
public class OpenWhiskHandler {

    private static final Logger logger = LoggerFactory.getLogger(OpenWhiskHandler.class);
    private static final String RESULT_FIELD = "result";
    private static final String LANGUAGE_FIELD = "language";

    OpenWhiskHandler(){
    }

    public static JsonObject main(JsonObject args) {
        long startTime = System.currentTimeMillis();

        // Disable hostname verification (enable OpenWhisk connections)
        final Properties props = System.getProperties();
        props.setProperty("jdk.internal.httpclient.disableHostnameVerification", Boolean.TRUE.toString());

        // Create result object
        JsonObject response = new JsonObject();

        ExecutableWorkflow ex = null;

        // Set workflow language
        Language language = readLanguage(args);

        // Get input filename and possible additional parameters
        String filename = null;
        if (args != null && args.has("workflow")) {
            ex = new YAMLParser().parseExecutableWorkflowByStringContent(args.getAsJsonPrimitive("workflow").getAsString(), language, -1);
        }
        if (args != null && args.has("filename")) {
            filename = args.getAsJsonPrimitive("filename").getAsString();
            ex = new YAMLParser().parseExecutableWorkflow(filename, language, -1);

            // Check if filename is specified
            if (filename == null) {
                response.addProperty(RESULT_FIELD, "Error: No filename specified.");
                return response;
            }
        }
        HashMap<String, Object> jsonMap = new HashMap<>();
        if (args != null && args.has("params")) {
            jsonMap = new Gson().fromJson(args.get("params").toString(), HashMap.class);
        }

        // Parse and create executable workflow
        if (ex != null) {

            // Set the workflow input
            Map<String, Object> input = new HashMap<>();
            input.put("some source", "5");// for ref gate
            input.put("some camera source", "0");
            input.put("some sensor source", "0");

            // Add params from EE call as input
            input.putAll(jsonMap);

            // Execute workflow

            try {
                ex.executeWorkflow(input);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                response.addProperty(RESULT_FIELD, "Error: Could not run workflow. See logs for more details.");
                return response;
            }
        }

        long endTime = System.currentTimeMillis();
        response.addProperty(RESULT_FIELD, "Workflow ran without errors in " + (endTime - startTime) + "ms. Start: " + startTime + ", End: " + endTime);
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
