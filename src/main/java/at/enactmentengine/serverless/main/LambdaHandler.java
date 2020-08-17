package at.enactmentengine.serverless.main;

import at.enactmentengine.serverless.nodes.ExecutableWorkflow;
import at.enactmentengine.serverless.parser.Language;
import at.enactmentengine.serverless.parser.YAMLParser;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * LambdaHandler to allow the execution of the Enactment Engine as Lambda
 * function.
 * <p>
 * based on @author markusmoosbrugger, jakobnoeckl
 * extended by @author stefanpedratscher
 */

public class LambdaHandler implements RequestHandler<LambdaHandler.InputObject, String> {

    private static final Logger logger = LoggerFactory.getLogger(LambdaHandler.class);

    @Override
    public String handleRequest(InputObject inputObject, Context context) {
        long startTime = System.currentTimeMillis();

        ExecutableWorkflow ex;

        // Set workflow language
        Language language = readLanguage(inputObject);

        // Check if input is valid
        if (inputObject == null || /*inputObject.getBucketName() == null ||*/ inputObject.getFilename() == null) {
            if (inputObject == null || inputObject.getWorkflow() == null) {
                return "{\"result\": \"Error: Could not run workflow. Input not valid.\"}";
            }

            // Parse and create executable workflow
            ex = new YAMLParser().parseExecutableWorkflowByStringContent(inputObject.getWorkflow(), language, -1);
        } else {

            // Parse and create executable workflow
            ex = new YAMLParser().parseExecutableWorkflow(inputObject.getFilename(), language, -1);
        }

        // Check if conversion to an executable workflow succeeded
        if (ex != null) {

            // Set the workflow input
            Map<String, Object> input = new HashMap<>();
            input.put("some source", "34477227772222299999");// for ref gate
            //input.put("some source", "10");// for anomaly
            input.put("some camera source", "0");
            input.put("some sensor source", "0");

            // Add params from EE call as input
            if (inputObject.getParams() != null) {
                inputObject.getParams().forEach(input::put);
            }

            // Execute workflow
            try {
                ex.executeWorkflow(input);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                return "{\"result\": \"Error: Could not run workflow. See logs for more details.\"}";
            }
        } else {
            return "{\"result\": \"Error: Could not convert to executable workflow.\"}";
        }

        long endTime = System.currentTimeMillis();

        return "{\"result\": \"Workflow ran without errors in " + (endTime - startTime) + "ms. Start: " + startTime + ", End: " + endTime + "\"}";
    }

    /**
     * Read the language of the input object.
     *
     * @param inputObject to read from.
     * @return yaml, json or undefined.
     */
    private Language readLanguage(InputObject inputObject) {
        if (inputObject != null && inputObject.getLanguage() != null) {
            if ("yaml".equals(inputObject.getLanguage())) {
                return Language.YAML;
            } else if ("json".equals(inputObject.getLanguage())) {
                return Language.JSON;
            }
        }
        return Language.NOT_SET;
    }

    /**
     * InputObject represents the input
     * of the EE in AWS Lambda
     */
    public static class InputObject {

        // Filename of the workflow
        private String filename;

        // Potential bucket where the file is stored
        private String bucketName;

        // Additional parameters
        private Map<String, String> params;

        // Workflow as JSON
        private String workflow;

        // Workflow language (JSON, YAML)
        private String language;

        /*
         * Getter and Setter
         */

        public String getFilename() {
            return filename;
        }

        public void setFilename(String filename) {
            this.filename = filename;
        }

        public String getBucketName() {
            return bucketName;
        }

        public void setBucketName(String bucketName) {
            this.bucketName = bucketName;
        }

        public Map<String, String> getParams() {
            return params;
        }

        public void setParams(Map<String, String> params) {
            this.params = params;
        }

        public String getWorkflow() {
            return workflow;
        }

        public void setWorkflow(String workflow) {
            this.workflow = workflow;
        }

        public String getLanguage() {
            return language;
        }

        public void setLanguage(String language) {
            this.language = language;
        }

        @Override
        public String toString() {
            return "InputObject{" + "filename='" + filename + '\'' + '}';
        }
    }
}