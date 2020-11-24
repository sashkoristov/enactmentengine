package at.enactmentengine.serverless.main;

import at.enactmentengine.serverless.exception.MissingInputDataException;
import at.enactmentengine.serverless.nodes.ExecutableWorkflow;
import at.enactmentengine.serverless.parser.Language;
import at.enactmentengine.serverless.parser.YAMLParser;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * LambdaHandler to allow the execution of the Enactment Engine as Lambda
 * function.
 * <p>
 * based on @author markusmoosbrugger, jakobnoeckl
 * extended by @author stefanpedratscher
 */

public class LambdaHandler implements RequestHandler<LambdaHandler.InputObject, String> {

    /**
     * Logger for lambda handler.
     */
    private static final Logger logger = LoggerFactory.getLogger(LambdaHandler.class);

    /**
     * Starting point of the lambda handler function.
     *
     * @param inputObject request for the enactment-engine.
     * @param context of the lambda function.
     * @return json result.
     */
    @Override
    public String handleRequest(InputObject inputObject, Context context) {

        /* Start measuring time for workflow execution */
        long start = System.currentTimeMillis();

        int executionId = -1;
        Map<String, Object> executionResult = null;
        Language language = readLanguage(inputObject);

        if(inputObject != null) {
            if(inputObject.getWorkflow() != null){

                /* Parse the workflow */
                ExecutableWorkflow ex = new YAMLParser().parseExecutableWorkflowByStringContent(inputObject.getWorkflow(), language, executionId);
                try {
                    /* Execute the workflow */
                    executionResult = ex.executeWorkflow(inputObject.getInput());
                } catch (MissingInputDataException | ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                return "{\"result\": \"Error: Could not run workflow. Request not valid.\"}";
            }
        } else {
            return "{\"result\": \"Error: Could not run workflow. Request not specified.\"}";
        }

        /* Stop measuring time for workflow execution */
        long end = System.currentTimeMillis();

        return "{\"logs\": \"Workflow ran without errors in " + (end - start) + "ms. Start: " + start + ", End: " + end + "\"," +
                "\"result\": \"" + executionResult + "\"}";
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

        /**
         * Log the execution.
         */
        private boolean logResult;

        /**
         * Filename of the workflow
         */
        private String filename;

        /**
         * Potential bucket where the file is stored
         */
        private String bucketName;

        /**
         * Additional parameters
         */
        private Map<String, String> params;

        /**
         * Workflow as JSON
         */
        private String workflow;

        /**
         * Workflow input as JSON
         */
        private Map<String, Object> input;

        /**
         * Workflow language (JSON, YAML)
         */
        private String language;

        /**
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

        public Map<String, Object> getInput() { return input; }

        public void setInput(Map<String, Object> input) { this.input = input; }

        public boolean isLogResult() { return logResult; }

        public void setLogResult(boolean logResult) { this.logResult = logResult; }

        @Override
        public String toString() {
            return "InputObject{" + "filename='" + filename + '\'' + '}';
        }
    }
}