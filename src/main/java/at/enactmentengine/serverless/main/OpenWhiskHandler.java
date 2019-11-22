package at.enactmentengine.serverless.main;

import at.enactmentengine.serverless.exception.MissingInputDataException;
import at.enactmentengine.serverless.nodes.ExecutableWorkflow;
import at.enactmentengine.serverless.parser.YAMLParser;
import com.cloudant.client.api.ClientBuilder;
import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.Database;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URL;
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

    private static final Logger logger = LoggerFactory.getLogger(App.class);

    public static JsonObject main(JsonObject args) {
        long startTime = System.currentTimeMillis();

        // Disable hostname verification (enable OpenWhisk connections)
        final Properties props = System.getProperties();
        props.setProperty("jdk.internal.httpclient.disableHostnameVerification", Boolean.TRUE.toString());

        // Create result object
        JsonObject response = new JsonObject();

        ExecutableWorkflow ex = null;

        // Get input filename and possible additional parameters
        String filename = null;
        if (args != null && args.has("workflow")) {
            ex = new YAMLParser().parseExecutableWorkflowByStringContent(args.getAsJsonPrimitive("workflow").getAsString());
        }
        if (args != null && args.has("filename")) {
            filename = args.getAsJsonPrimitive("filename").getAsString();
            ex = new YAMLParser().parseExecutableWorkflow(filename);

            // Check if filename is specified
            if (filename == null) {
                response.addProperty("result", "Error: No filename specified.");
                return response;
            }
        }
        HashMap jsonMap = new HashMap<>();
        if (args != null && args.has("params"))
            jsonMap = new Gson().fromJson(args.get("params").toString(), HashMap.class);

        // Parse and create executable workflow
        if (ex != null) {

            // Set the workflow input
            Map<String, Object> input = new HashMap<String, Object>();
            input.put("some source", "5");// for ref gate
            input.put("some camera source", "0");
            input.put("some sensor source", "0");

            // Add params from EE call as input
            input.putAll(jsonMap);

            // Execute workflow
            try {
                ex.executeWorkflow(input);
            } catch (MissingInputDataException e) {
                logger.error(e.getMessage(), e);
                response.addProperty("result", "Error: Could not run workflow. See logs for more details.");
                return response;
            }
        }

        long endTime = System.currentTimeMillis();
        response.addProperty("result", "Workflow ran without errors in " + (endTime - startTime) + "ms. Start: "+ startTime +", End: " + endTime);
        return response;
    }

    /**
     * Get a file from cloudant
     *
     * @param docId id of the document
     * @return InputStream of the file
     */
    private static InputStream getFileFromCloudant(String docId) {

        // Authenticate
        CloudantClient client = null;
        try {
            Properties properties = new Properties();
            properties.load(LambdaHandler.class.getResourceAsStream("/credentials.properties"));
            String apikey = properties.getProperty("ibm_api_key");
            client = ClientBuilder
                    .url(new URL(
                            "https://256ea85e-21ba-4e92-aafa-1a1fb6ae2498-bluemix.cloudantnosqldb.appdomain.cloud/"))
                    .iamApiKey(apikey).build();
        } catch (Exception e) {
            throw new RuntimeException("Client not created", e);
        }

        // Database access
        Database db = null;
        try {
            db = client.database("input_files", false);
        } catch (Exception e) {
            throw new RuntimeException("DB Not found", e);
        }
        InputStream is = db.getAttachment("yaml_files", docId);

        return is;
    }
}
