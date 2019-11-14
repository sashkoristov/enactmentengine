package at.enactmentengine.serverless.main;

import at.enactmentengine.serverless.exception.MissingInputDataException;
import at.enactmentengine.serverless.nodes.ExecutableWorkflow;
import at.enactmentengine.serverless.parser.YAMLParser;
import com.amazonaws.AmazonServiceException;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * LambdaHandler to allow the execution of the Enactment Engine as Lambda
 * function.
 * 
 * based on @author markusmoosbrugger, jakobnoeckl
 * extended by @author stefanpedratscher
 *
 */
@SuppressWarnings("unused")
public class LambdaHandler implements RequestHandler<LambdaHandler.InputObject, String> {

	public String handleRequest(InputObject inputObject, Context context) {
		long startTime = System.currentTimeMillis();
		System.out.println("got \"" + inputObject + "\" from call" + " ["+System.currentTimeMillis()+"ms]");
		final Logger logger = LoggerFactory.getLogger(App.class);
		if (inputObject == null || /*inputObject.getBucketname() == null ||*/ inputObject.getFilename() == null) {
			return "{\"result\": \"Workflow stopped with errors! Input missing!\"}";
		}

		ExecutableWorkflow ex;
		try {
			InputStream in = App.class.getResourceAsStream(inputObject.getFilename());
			YAMLParser yamlParser = new YAMLParser();
			ex = yamlParser.parseExecutableWorkflowOld(in);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return "{\"result\": \"Workflow stopped with errors! See Logs!\"}";
		}

		if (ex != null) {
			Map<String, Object> input = new HashMap<String, Object>();
			input.put("some source", "34477227772222299999");// for ref gate
			input.put("some source", "10");// for anomaly
			input.put("some camera source", "0");
			input.put("some sensor source", "0");

			if (inputObject.getParams() != null) {
				inputObject.getParams().forEach((k, v) -> input.put(k, v));
			}

			try {
				ex.executeWorkflow(input);
			} catch (MissingInputDataException e) {
				logger.error(e.getMessage(), e);
				return "{\"result\": \"Workflow stopped with errors! See Logs!\"}";
			}
		} else {
			return "{\"result\": \"Workflow was not created!\"}";
		}
		long endTime = System.currentTimeMillis();
		return "{\"result\": \"Workflow ran without errors in "+(endTime-startTime)+"ms!\"}" + startTime;
	}

	private static ExecutableWorkflow readFileFromS3(InputObject inputObject) throws Exception {
		S3Object fullObject = null, objectPortion = null, headerOverrideObject = null;

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


		BasicAWSCredentials awsCreds = new BasicAWSCredentials(awsAccessKey, awsSecretKey);

		ExecutableWorkflow ex = null;
		try {
			AmazonS3 s3Client = AmazonS3ClientBuilder.standard().withRegion(Regions.US_EAST_2)
					.withCredentials(new AWSStaticCredentialsProvider(awsCreds)).build();

			// Get an object and print its contents.
			System.out.println("Downloading an object");
			fullObject = s3Client
					.getObject(new GetObjectRequest(inputObject.getBucketname(), inputObject.getFilename()));
			System.out.println("Content-Type: " + fullObject.getObjectMetadata().getContentType());
			System.out.println("Content: ");

			YAMLParser yamlParser = new YAMLParser();
			ex = yamlParser.parseExecutableWorkflowOld(fullObject.getObjectContent());
			s3Client.shutdown();

		} catch (AmazonServiceException e) {
			throw e;
		} catch (SdkClientException e) {
			throw e;
		} finally {
			// To ensure that the network connection doesn't remain open, close any open
			// input streams.
			if (fullObject != null) {
				fullObject.close();
			}
			if (objectPortion != null) {
				objectPortion.close();
			}
			if (headerOverrideObject != null) {
				headerOverrideObject.close();
			}
		}

		return ex;
	}

	public static class InputObject {
		private String filename;
		private String bucketname;
		private Map<String, String> params;

		public String getFilename() {
			return filename;
		}

		public void setFilename(String filename) {
			this.filename = filename;
		}

		public String getBucketname() {
			return bucketname;
		}

		public void setBucketname(String bucketname) {
			this.bucketname = bucketname;
		}

		public Map<String, String> getParams() {
			return params;
		}

		public void setParams(Map<String, String> params) {
			this.params = params;
		}

		@Override
		public String toString() {
			return "InputObject{" + "filename='" + filename + '\'' + '}';
		}
	}
}