package at.enactmentengine.serverless.main;

import at.enactmentengine.serverless.nodes.ExecutableWorkflow;
import at.enactmentengine.serverless.parser.Language;
import at.enactmentengine.serverless.parser.YAMLParser;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Main class of enactment engine which specifies the input file and starts the
 * workflow on the machine on which it gets started.
 * <p>
 * based on @author markusmoosbrugger, jakobnoeckl
 * extended by @author stefanpedratscher
 */
class Executor {

    private static final Logger LOGGER = LoggerFactory.getLogger(Executor.class);
    private Map<String, Object> input;

    public Executor(){
        input = new HashMap<>();
    }

    Map<String, Object> executeWorkflow(String workflow, String workflowInput, int executionId) {
        try {
            return executeWorkflow(
                    workflow == null ? null : FileUtils.readFileToByteArray(new File(workflow)),
                    workflowInput == null ? null : FileUtils.readFileToByteArray(new File(workflowInput)),
                    executionId);
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
            return null;
        }
    }

    Map<String, Object> executeWorkflow(byte[] workflow, byte[] workflowInput, int executionId) {

        long start = System.currentTimeMillis();

        // Disable hostname verification (enable OpenWhisk connections)
        final Properties props = System.getProperties();
        props.setProperty("jdk.internal.httpclient.disableHostnameVerification", Boolean.TRUE.toString());

        // Get the input file as argument or default string
        if (workflow == null) {
            LOGGER.error("Please specify a workflow file");
        }

        // Create an executable workflow
        YAMLParser yamlParser = new YAMLParser();
        ExecutableWorkflow ex = yamlParser.parseExecutableWorkflow(workflow, Language.YAML, executionId);

        Map<String, Object> output = null;
        if (ex != null) {

            if(workflowInput != null){
                String decodedJsonInput = new String(workflowInput, StandardCharsets.UTF_8);
                input = new Gson().fromJson(decodedJsonInput, new TypeToken<HashMap<String, Object>>() {}.getType());
            }

            // --> REMOVE THESE TMP INPUT DATA IF NO LONGER NEEDED
            // Set some example workflow input
            input.put("some source", "34477227772222299999");// for ref gate
            JsonArray arr = new JsonArray();
            JsonArray arr2 = new JsonArray();
            JsonArray arr3 = new JsonArray();
            JsonArray arr4 = new JsonArray();
            int arr1Size = 2000;
            int arr2Size = 0;
            int arr3Size = 0;
            int arr4Size = 0;
            int total = arr1Size + arr2Size + arr3Size + arr4Size; // each
            for (int i = 0; i < arr1Size; i++) {
                arr.add(1);
            }
            for (int i = 0; i < arr2Size; i++) {
                arr2.add(1);
            }
            for (int i = 0; i < arr3Size; i++) {
                arr3.add(1);
            }
            for (int i = 0; i < arr4Size; i++) {
                arr4.add(1);
            }
            input.put("each", 1);
            input.put("total", total);
            input.put("array", arr);
            input.put("array2", arr2);
            input.put("array3", arr3);
            input.put("array4", arr4);
            // input.put("some source", "4");// for anomaly
            // input.put("some source", 50);// for parallel and basic files
            input.put("some camera source", "0");
            input.put("some sensor source", "0");
            // <-- REMOVE THESE TMP INPUT DATA IF NO LONGER NEEDED

            // Execute the workflow
            try {
                output = ex.executeWorkflow(input);
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }

            long end = System.currentTimeMillis();
            LOGGER.info("Execution took {}ms.", (end - start));
        }
        return output;
    }
}
