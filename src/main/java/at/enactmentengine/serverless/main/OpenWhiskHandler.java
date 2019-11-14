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
 * 
 * based on @author markusmoosbrugger, jakobnoeckl
 * extended by @author stefanpedratscher
 *
 */
public class OpenWhiskHandler {

	@SuppressWarnings("unchecked")
	public static JsonObject main(JsonObject args) {
		final Logger logger = LoggerFactory.getLogger(App.class);
		YAMLParser yamlParser = new YAMLParser();
		final Properties props = System.getProperties();
		props.setProperty("jdk.internal.httpclient.disableHostnameVerification", Boolean.TRUE.toString());

		String filename = null;
		if (args != null && args.has("filename"))
			filename = args.getAsJsonPrimitive("filename").getAsString();
		Map<String, Object> jsonMap = new HashMap<>();
		if (args != null && args.has("params"))
			jsonMap = new Gson().fromJson(args.get("params").toString(), HashMap.class);

		JsonObject response = new JsonObject();
		String in;

		if (filename != null) {
            InputStream tmp = getFileFromCloudant(filename);
            in = "test";
        }
		else {
			response.addProperty("result", "No filename defined!");
			return response;
		}

		System.out.println(in + " ");

		logger.debug("Parsing file " + filename + "!");
		ExecutableWorkflow ex = yamlParser.parseExecutableWorkflow(in);
		if (ex != null) {
			Map<String, Object> input = new HashMap<String, Object>();

			input.put("some source", "5");// for ref gate
			input.put("some camera source", "0");
			input.put("some sensor source", "0");
			input.putAll(jsonMap);
			try {
				ex.executeWorkflow(input);
			} catch (MissingInputDataException e) {
				logger.error(e.getMessage(), e);
				response.addProperty("result", "Workflow stopped with errors! See Log for details!");
				return response;
			}
		}

		response.addProperty("result", "Workflow ran without errors! See logs for details!");
		return response;
	}

	private static InputStream getFileFromCloudant(String docId) {
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

		System.out.println(client);
		Database db = null;
		try {
			db = client.database("input_files", false);
		} catch (Exception e) {
			throw new RuntimeException("DB Not found", e);
		}
		System.out.println(db);

		InputStream is = db.getAttachment("yaml_files", docId);

		return is;
	}
}
