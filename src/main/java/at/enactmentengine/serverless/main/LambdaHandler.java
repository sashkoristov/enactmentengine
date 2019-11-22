package at.enactmentengine.serverless.main;

import at.enactmentengine.serverless.exception.MissingInputDataException;
import at.enactmentengine.serverless.nodes.ExecutableWorkflow;
import at.enactmentengine.serverless.parser.YAMLParser;
import com.amazonaws.SdkClientException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * LambdaHandler to allow the execution of the Enactment Engine as Lambda
 * function.
 * <p>
 * based on @author markusmoosbrugger, jakobnoeckl
 * extended by @author stefanpedratscher
 */

public class LambdaHandler implements RequestHandler<LambdaHandler.InputObject, String> {

    static final Logger logger = LoggerFactory.getLogger(App.class);

    public String handleRequest(InputObject inputObject, Context context) {
        long startTime = System.currentTimeMillis();

        ExecutableWorkflow ex = null;

        // Check if input is valid
        if (inputObject == null || /*inputObject.getBucketName() == null ||*/ inputObject.getFilename() == null) {
            if(inputObject == null || inputObject.getWorkflow() == null){
                return "{\"result\": \"Error: Could not run workflow. Input not valid.\"}";
            }

            // Parse and create executable workflow
            ex = new YAMLParser().parseExecutableWorkflowByStringContent(inputObject.getWorkflow());
        }else{

            // Parse and create executable workflow
            ex = new YAMLParser().parseExecutableWorkflow(inputObject.getFilename());
        }

        // Check if conversion to an executable workflow succeeded
        if (ex != null) {

            // Set the workflow input
            Map<String, Object> input = new HashMap<String, Object>();
            input.put("some source", "34477227772222299999");// for ref gate
            //input.put("some source", "10");// for anomaly
            input.put("some camera source", "0");
            input.put("some sensor source", "0");

            // Add params from EE call as input
            if (inputObject.getParams() != null) {
                inputObject.getParams().forEach((k, v) -> input.put(k, v));
            }

            // Execute workflow
            try {
                ex.executeWorkflow(input);
            } catch (MissingInputDataException e) {
                logger.error(e.getMessage(), e);
                return "{\"result\": \"Error: Could not run workflow. See logs for more details.\"}";
            }
        } else {
            return "{\"result\": \"Error: Could not convert to executable workflow.\"}";
        }

        long endTime = System.currentTimeMillis();

        return "{\"result\": \"Workflow ran without errors in " + (endTime - startTime) + "ms. Start: "+ startTime +", End: " + endTime + "\"}";
    }

    /**
     * Read a yaml workflow from S3
     *
     * @param inputObject of the request
     * @return an executable workflow
     * @throws Exception on failure
     */
    private static ExecutableWorkflow readFileFromS3(InputObject inputObject) throws Exception {
        S3Object fullObject = null;

        // Read the aws credentials
        String awsAccessKey = null;
        String awsSecretKey = null;
        try {
            Properties properties = new Properties();
            properties.load(LambdaHandler.class.getResourceAsStream("/credentials.properties"));
            awsAccessKey = properties.getProperty("aws_access_key");
            awsSecretKey = properties.getProperty("aws_secret_key");
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            // Authenticate
            BasicAWSCredentials awsCreds = new BasicAWSCredentials(awsAccessKey, awsSecretKey);
            AmazonS3 s3Client = AmazonS3ClientBuilder.standard().withRegion(Regions.US_EAST_2)
                    .withCredentials(new AWSStaticCredentialsProvider(awsCreds)).build();

            // Get the object from the bucket
            fullObject = s3Client
                    .getObject(new GetObjectRequest(inputObject.getBucketName(), inputObject.getFilename()));

            YAMLParser yamlParser = new YAMLParser();

            // Download file and save as yaml
            String pathname = "/tmp/workflow.yaml";
            FileUtils.copyInputStreamToFile(fullObject.getObjectContent(), new File(pathname));
            ExecutableWorkflow ex = yamlParser.parseExecutableWorkflow(pathname);

            s3Client.shutdown();

            return ex;

        } catch (SdkClientException e) {
            throw e;
        } finally {
            // To ensure that the network connection doesn't remain open, close any open input streams.
            if (fullObject != null) {
                fullObject.close();
            }
        }
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

        @Override
        public String toString() {
            return "InputObject{" + "filename='" + filename + '\'' + '}';
        }
    }
}