package at.enactmentengine.serverless.nodes;

import at.enactmentengine.serverless.Handler.AsyncHandler;
import at.enactmentengine.serverless.exception.MissingInputDataException;
import at.enactmentengine.serverless.exception.MissingResourceLinkException;
//import at.enactmentengine.serverless.main.LambdaHandler;
//import at.enactmentengine.serverless.main.Local;
import at.enactmentengine.serverless.object.Status;
import at.enactmentengine.serverless.object.Utils;
import at.uibk.dps.*;
import at.uibk.dps.afcl.functions.objects.DataIns;
import at.uibk.dps.afcl.functions.objects.DataOutsAtomic;
import at.uibk.dps.afcl.functions.objects.PropertyConstraint;
import at.uibk.dps.database.SQLLiteDatabase;
import at.uibk.dps.exception.InvokationFailureException;
import at.uibk.dps.exception.LatestFinishingTimeException;
import at.uibk.dps.exception.LatestStartingTimeException;
import at.uibk.dps.exception.MaxRunningTimeException;
import at.uibk.dps.function.AlternativeStrategy;
import at.uibk.dps.function.ConstraintSettings;
import at.uibk.dps.function.FaultToleranceSettings;
import at.uibk.dps.function.Function;
import at.uibk.dps.socketutils.ConstantsNetwork;
import at.uibk.dps.socketutils.UtilsSocket;
import at.uibk.dps.socketutils.entity.Invocation;
import at.uibk.dps.socketutils.logger.RequestLoggerInvocationWrite;
import at.uibk.dps.socketutils.logger.UtilsSocketLogger;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import jFaaS.Gateway;
// import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;
import java.sql.Timestamp;
import java.util.*;

/**
 * Class which handles the execution of a function.
 *
 * @author markusmoosbrugger, jakobnoeckl
 *
 *         adapted by @author stefanpedratscher
 */
public class FunctionNode extends Node {

	/**
	 * Logger for the a function node.
	 */
	private static final Logger logger = LoggerFactory.getLogger(FunctionNode.class);

	/**
	 * The number of executed functions.
	 */
	private static int counter = 0;

	/**
	 * The execution id of the workflow (needed to log the execution).
	 */
	private int executionId;

	/**
	 * The constraints for the function node.
	 */
	private List<PropertyConstraint> constraints;

	/**
	 * The properties of the function node.
	 */
	private List<PropertyConstraint> properties;

	/**
	 * Output of the function node.
	 */
	private List<DataOutsAtomic> output;

	/**
	 * Input to the function node.
	 */
	private List<DataIns> input;

	/**
	 * The invoker for the cloud functions.
	 */
	private static Gateway gateway = new Gateway(Utils.PATH_TO_CREDENTIALS);

	/**
	 * The result of the function node.
	 */
	private Map<String, Object> result;

	/**
	 * The protocol for the http requests.
	 */
	private static final String PROTOCOL = "https://";

	/**
	 * Constructor for a function node.
	 *
	 * @param name        of the base function.
	 * @param type        of the base function (fType).
	 * @param properties  of the base function.
	 * @param constraints of the base function.
	 * @param input       to the base function.
	 * @param output      of the base function.
	 * @param executionId for the logging of the execution.
	 */
	public FunctionNode(String name, String type, List<PropertyConstraint> properties,
						List<PropertyConstraint> constraints, List<DataIns> input, List<DataOutsAtomic> output, int executionId) {
		super(name, type);
		this.output = output;
		this.properties = properties;
		this.constraints = constraints;
		this.input = input;
		this.executionId = executionId;
		if (output == null) {
			this.output = new ArrayList<>();
		}
	}

	/**
	 * Checks the inputs, invokes function and passes results to children.
	 *
	 * @return boolean representing success of the node execution.
	 * @throws Exception on failure.
	 */
	@Override
	public Boolean call() throws Exception {

		/* The identifier for the current function */
		int id;
		synchronized (this) {
			id = counter++;
		}
		Map<String, Object> functionOutputs;

		//check if the its async handler
		if(isAsyncHandler())
		{
			functionOutputs= new HashMap<>();

			long start = System.currentTimeMillis();
			AsyncHandler asyncHandler = new AsyncHandler(isAsync(),this.input,this.parents,this.dataValues);
			asyncHandler.setAccounts(getAWSAccount(),getIBMAccount(),getGoogleAccount(),getAzureAccount());
			asyncHandler.run();
			long end = System.currentTimeMillis();

			ArrayList<String> returnString = new ArrayList<>();
			returnString.add("Finished functions:" + asyncHandler.getFinished());
			returnString.add("Running functions:" + asyncHandler.getRunning());
			returnString.add("Failed functions:" + asyncHandler.getFailed());

			String resultString = returnString.toString();
			logFunctionOutput(start, end, resultString, id);

			functionOutputs.put("handlerOutput", resultString);
			/* Pass the output to the next node */
			for (Node node : children) {
				node.passResult(functionOutputs);
				node.call();
			}
		}
		// else handle according to old system
		else {
			/* Read the resource link of the base function */
			String resourceLink = getResourceLink();
			logger.info("Executing function " + name + " at resource: " + resourceLink + " [" + System.currentTimeMillis()
					+ "ms], id=" + id);

			/* Actual values of the function input */
			Map<String, Object> actualFunctionInputs = new HashMap<>();

			/* Output values of the base function */
			functionOutputs = new HashMap<>();

			try {
				/* Check if an input is specified */
				if (input != null) {

					/* Iterate over all specified inputs */
					for (DataIns data : input) {

						/* Check if actual data contains the specified source */
						if (dataValues.containsKey(data.getSource())) {

							/* Check if the element should be passed to the output */
							if (data.getPassing() != null && data.getPassing()) {
								functionOutputs.put(name + "/" + data.getName(), dataValues.get(data.getSource()));
							} else {
								actualFunctionInputs.put(data.getName(), dataValues.get(data.getSource()));
							}
						} else {
							throw new MissingInputDataException(FunctionNode.class.getCanonicalName() + ": " + name
									+ " needs " + data.getSource() + " !");
						}
					}
				}
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
				return false;
			}

			/* Simulate Availability if specified TODO is this really needed? */
			if (Utils.SIMULATE_AVAILABILITY) {
				SQLLiteDatabase db = new SQLLiteDatabase("jdbc:sqlite:Database/FTDatabase.db");
				double simAvail = db.getSimulatedAvail(resourceLink);
				actualFunctionInputs = checkFunctionSimAvail(simAvail, actualFunctionInputs);
			}

			/* Log the function input */
			logFunctionInput(actualFunctionInputs, id);

			/* Parse function with optional constraints and properties */
			Function functionToInvoke = parseFTConstraints(resourceLink, actualFunctionInputs);

			/* Invoke function and measure duration */
			long start = System.currentTimeMillis();
			String resultString = invokeFunction(functionToInvoke, resourceLink, actualFunctionInputs);
			long end = System.currentTimeMillis();

			/* Log the function output */
			logFunctionOutput(start, end, resultString, id);

			/*
			 * Read the actual function outputs by their key and store them in
			 * functionOutputs
			 */
			// TODO check for success
			boolean success = getValuesParsed(resultString, functionOutputs);
			/* Pass the output to the next node */
			for (Node node : children) {
				node.passResult(functionOutputs);
				node.call();
			}
			/* Set the result of the function node */
			result = functionOutputs;

			/*
			 * Check if the execution identifier is specified (check if execution should be
			 * stored in the database)
			 */
			if (executionId != -1) {

				/* Create a function invocation object */
				Invocation functionInvocation = new Invocation(resourceLink, Utils.detectProvider(resourceLink).toString(),
						Utils.detectRegion(resourceLink),
						new Timestamp(start + TimeZone.getTimeZone("Europe/Rome").getOffset(start)),
						new Timestamp(end + TimeZone.getTimeZone("Europe/Rome").getOffset(start)), (end - start),
						checkResultSuccess(resultString).toString(), null, executionId);

				/* Store the invocation in the database */
				storeInDBFunctionInvocation(functionInvocation);
			}
		}

		return true;
	}

	/**
	 * Add availability value to the function input. TODO is this really needed?
	 *
	 * @param simAvail       the simulated availability from the database.
	 * @param functionInputs the actual function input.
	 * @return the new function input.
	 */
	private Map<String, Object> checkFunctionSimAvail(double simAvail, Map<String, Object> functionInputs) {

		/* Check if this functions avail should be simulated */
		if (simAvail != 1) {
			functionInputs.put("availability", simAvail);
		}
		return functionInputs;
	}

	/**
	 * Check for error object in the json result of the cloud function.
	 *
	 * @param resultString json result of the base function.
	 * @return status og the execution.
	 */
	private Status checkResultSuccess(String resultString) {
		return resultString.contains("error:") ? Status.ERROR : Status.SUCCESS;
	}

	/**
	 * Log the function output.
	 *
	 * @param start        time of the base function.
	 * @param end          time of the base function.
	 * @param resultString json result of the base function.
	 * @param id           unique identifier of the base function.
	 */
	private void logFunctionOutput(long start, long end, String resultString, int id) {
		if (resultString.length() > 100000) {
			logger.info("Function took: " + (end - start) + " ms. Result: too large [" + System.currentTimeMillis()
					+ "ms], id=" + id + "");
		} else {
			logger.info("Function took: " + (end - start) + " ms. Result: " + name + " : " + resultString + " ["
					+ System.currentTimeMillis() + "ms], id=" + id + "");
		}
	}

	/**
	 * Invoke the base function.
	 *
	 * @param functionToInvoke the base function which should be invoked.
	 * @param resourceLink     the resource of the base function.
	 * @param functionInputs   the input to the base function.
	 *
	 * @return the stringified json result of the base function invocation.
	 *
	 * @throws MaxRunningTimeException      on maximum runtime exceeded.
	 * @throws LatestFinishingTimeException on latest finish time exceeded.
	 * @throws LatestStartingTimeException  on latest start time exceeded.
	 * @throws InvokationFailureException   on failed invocation.
	 * @throws IOException                  on input output exception.
	 */
	private String invokeFunction(Function functionToInvoke, String resourceLink, Map<String, Object> functionInputs)
			throws MaxRunningTimeException, LatestFinishingTimeException, LatestStartingTimeException,
			InvokationFailureException, IOException, InterruptedException {
		String resultString;

		/* Check if function should be invoked with fault tolerance settings */
		if (!isAsync() && functionToInvoke != null && (functionToInvoke.hasConstraintSet() || functionToInvoke.hasFTSet())) {

			/* Invoke the function with fault tolerance */
			FaultToleranceEngine ftEngine = null;

			if (getGoogleAccount() != null && getAzureAccount() != null && getIBMAccount() != null && getAWSAccount() != null) {
				ftEngine = new FaultToleranceEngine(getGoogleAccount(), getAzureAccount(), getAWSAccount(), getIBMAccount());

			} else if (getGoogleAccount() != null && getAzureAccount() != null && getIBMAccount() != null) {
				ftEngine = new FaultToleranceEngine(getGoogleAccount(), getAzureAccount(), getIBMAccount());
			} else if (getGoogleAccount() != null && getAzureAccount() != null && getAWSAccount() != null) {
				ftEngine = new FaultToleranceEngine(getGoogleAccount(), getAzureAccount(), getAWSAccount());
			} else if (getAzureAccount() != null && getGoogleAccount() != null) {
				ftEngine = new FaultToleranceEngine(getGoogleAccount(), getAzureAccount());
			} else if (getIBMAccount() != null && getAWSAccount() != null) {
				ftEngine = new FaultToleranceEngine(getAWSAccount(), getIBMAccount());

			}

			try {
				logger.info("Invoking function with fault tolerance...");
				resultString = ftEngine.InvokeFunctionFT(functionToInvoke);
			} catch (Exception e) {
				result = null;
				throw e;
			}
		} else {
			/* Invoke the function without fault tolerance */
			/* check if async invocation or not */
			if (isAsync()) {
				System.out.println("async invocation function node");
				long s = System.currentTimeMillis();
				resultString = gateway.invokeAsyncFunciton(resourceLink, functionInputs).toString();
				long e = System.currentTimeMillis();
				System.out.println("time: " + (e - s));

			} else {
				System.out.println("sync invocation function node");
				long s = System.currentTimeMillis();
				resultString = gateway.invokeFunction(resourceLink, functionInputs).toString();
				long e = System.currentTimeMillis();
				System.out.println("time: " + (e - s));
			}
		}
		return resultString;

	}

	private boolean isAsyncHandler()
	{
		//FIXME ? do we want a specific name check to
		return this.type.equals("build-in:asyncHandler");
	}

	private boolean isAsync()
	{
		for (PropertyConstraint propertyConstraint: this.properties) {
			if(propertyConstraint.getName().equals("invoke-type")
					&& propertyConstraint.getValue().equals("ASYNC"))
				return true;
		}
		return false;
	}

	/**
	 * Log the function input.
	 *
	 * @param functionInputs the actual function input.
	 * @param id             unique identifier of the base function.
	 */
	private void logFunctionInput(Map<String, Object> functionInputs, int id) {
		if (functionInputs.size() > 20) {
			logger.info("Input for function is large [{}ms], id={}", System.currentTimeMillis(), id);
		} else {
			logger.info("Input for function " + name + " : " + functionInputs + " [" + System.currentTimeMillis()
					+ "ms], id=" + id + "");
		}
	}

	/**
	 * Send a request to store the function invocation in the logging database.
	 *
	 * @param functionInvocation to store in the database.
	 */
	private void storeInDBFunctionInvocation(Invocation functionInvocation) {

		logger.info("Connecting to logger service...");
		try (Socket loggerService = new Socket(ConstantsNetwork.LOGGER_SERVICE_HOST,
				ConstantsNetwork.LOGGER_SERVICE_PORT)) {

			RequestLoggerInvocationWrite invocationLogManagerRequest = UtilsSocketLogger
					.generateRequestInvocationWrite(functionInvocation, executionId);
			logger.info("Sending request to logger-service...");
			UtilsSocket.sendJsonObject(loggerService.getOutputStream(), invocationLogManagerRequest);

			logger.info("Closing connection to logger service...");
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
	}

	/**
	 * Parses the json result into a map as key-value pair.
	 *
	 * @param result          The stringified json result from the base function.
	 * @param functionOutputs The output values / map of the base function. a
	 * @return success or failure of the value parsing.
	 */
	private boolean getValuesParsed(String result, Map<String, Object> functionOutputs) {

		/* Check if there is a function result and a specified output */
		if (result == null || "null".equals(result)) {
			return output == null || output.isEmpty();
		}

		try {
			/* Iterate over all specified outputs in the yaml file */
			for (DataOutsAtomic data : output) {

				/* Convert the json result to a json object */
				JsonObject jsonResult = generateJson(result, data);

				/* Check if the function output already contains the specified value */
				if (functionOutputs.containsKey(name + "/" + data.getName())) {
					continue;
				}

				// TODO why not do this?
				// functionOutputs.put(name + "/" + data.getName(),
				// jsonResult.get(data.getName()));

				/* Parse according data type */
				switch (data.getType()) {
					case "number":
						Object number = jsonResult.get(data.getName()).getAsDouble();
						functionOutputs.put(name + "/" + data.getName(), number);
						break;
					case "string":
						functionOutputs.put(name + "/" + data.getName(), jsonResult.get(data.getName()).getAsString());
						break;
					case "collection":
						// array stays array to later decide which type
						functionOutputs.put(name + "/" + data.getName(), jsonResult.get(data.getName()).getAsJsonArray());
						break;
					case "object":
						functionOutputs.put(name + "/" + data.getName(), jsonResult);
						break;
					case "bool":
						functionOutputs.put(name + "/" + data.getName(), jsonResult.get(data.getName()).getAsBoolean());
						break;
					default:
						logger.error("Error while trying to parse key in function {}. Type: {}", name, data.getType());
						break;
				}
			}
			return true;

		} catch (Exception e) {
			logger.error("Error while trying to parse key in function {}", name);
			return false;
		}
	}

	/**
	 * Convert the stringified json to a json object representing the function
	 * output.
	 *
	 * @param resultString stringified json.
	 * @param data         outputs of the base function.
	 *
	 * @return json object representing the base function output.
	 */
	JsonObject generateJson(String resultString, DataOutsAtomic data) {
		JsonObject jso;
		try {

			/* Parse the json string to a json object */
			jso = new Gson().fromJson(resultString, JsonObject.class);
		} catch (com.google.gson.JsonSyntaxException e) {

			/* If there is no JSON object as return value, create one */
			jso = new JsonObject();
			jso.addProperty(data.getName(), resultString);
		}
		return jso;
	}

	/**
	 * Get the resource link of the base function.
	 *
	 * @return the resource link of the base function.
	 *
	 * @throws MissingResourceLinkException on missing resource link.
	 */
	private String getResourceLink() throws MissingResourceLinkException {

		/* Check if there are properties specified */
		if (properties == null) {
			throw new MissingResourceLinkException("No properties specified " + this.toString());
		}

		String resourceLink = null;

		/* Iterate over properties and search for the resource */
		for (PropertyConstraint p : properties) {
			if ("resource".equals(p.getName())) {
				resourceLink = p.getValue();
				break;
			}
		}

		/* Check if the resource link was not specified */
		if (resourceLink == null) {
			throw new MissingResourceLinkException("No resource link on function node " + this.toString());
		}

		if (!resourceLink.startsWith("http") && !resourceLink.startsWith("arn")) {

			/* Remove the programming language of the resource link */
			resourceLink = resourceLink.substring(resourceLink.indexOf(":") + 1);
		}

		return resourceLink;
	}

	/**
	 * Sets the dataValues and passes the result to all children.
	 *
	 * @param input to the child functions.
	 */
	@Override
	public void passResult(Map<String, Object> input) {
		synchronized (this) {
			try {
				this.dataValues = input;
				for (Node node : children) {
					node.passResult(input);
				}
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}

	}

	/**
	 * Get the result of a function node.
	 *
	 * @return result of the base function.
	 */
	@Override
	public Map<String, Object> getResult() {
		return result;
	}

	/**
	 * Parse the fault tolerance constraints.
	 *
	 * @param resourceLink   the resource link of the base function.
	 * @param functionInputs inputs to the base function.
	 *
	 * @return function object with correctly set ft values.
	 */
	public Function parseFTConstraints(String resourceLink, Map<String, Object> functionInputs) {

		/* Keeps track of all constraint settings */
		List<PropertyConstraint> cList = new LinkedList<>();

		/* Keeps track of all fault tolerance settings */
		List<PropertyConstraint> ftList = new LinkedList<>();

		/* Check if there are constraints set */
		if (this.constraints == null) {
			return null;
		}

		/* Iterate over constraints and look for according settings */
		for (PropertyConstraint constraint : this.constraints) {
			if (constraint.getName().startsWith("FT-")) {
				ftList.add(constraint);
			} else if (constraint.getName().startsWith("C-")) {
				cList.add(constraint);
			}
		}

		/* Parse fault tolerance settings */
		FaultToleranceSettings ftSettings = getFaultToleranceSettings(ftList, functionInputs);

		/* Parse constraint settings */
		ConstraintSettings cSettings = getConstraintSettings(cList);

		return new Function(resourceLink, this.type, functionInputs, ftSettings.isEmpty() ? null : ftSettings,
				cSettings.isEmpty() ? null : cSettings);
	}

	/**
	 * Look for fault tolerance settings.
	 *
	 * @param ftList         all fault tolerance settings.
	 * @param functionInputs the input of the base function.
	 *
	 * @return fault tolerance settings.
	 */
	private FaultToleranceSettings getFaultToleranceSettings(List<PropertyConstraint> ftList,
															 Map<String, Object> functionInputs) {

		/* Set the default fault tolerance settings to zero retries */
		FaultToleranceSettings ftSettings = new FaultToleranceSettings(0);

		/* Create a lis for the alternative strategy */
		List<List<Function>> alternativeStrategy = new LinkedList<>();

		/* Iterate over all fault tolerance constraints and check for supported ones */
		for (PropertyConstraint ftConstraint : ftList) {
			if (ftConstraint.getName().compareTo("FT-Retries") == 0) {

				/*
				 * Set the given number of retries a base function should be repeated if a
				 * failure happens
				 */
				ftSettings.setRetries(Integer.valueOf(ftConstraint.getValue()));
			} else if (ftConstraint.getName().startsWith("FT-AltPlan-")) {

				/* Pack all alternative function into an alternative plan */
				List<Function> alternativePlan = new LinkedList<>();
				String possibleResources = ftConstraint.getValue().substring(ftConstraint.getValue().indexOf(";") + 1);
				while (possibleResources.contains(";")) {
					String funcString = possibleResources.substring(0, possibleResources.indexOf(";"));
					Function tmpFunc = new Function(funcString, this.type, functionInputs);
					possibleResources = possibleResources.substring(possibleResources.indexOf(";") + 1);

					alternativePlan.add(tmpFunc);
				}
				alternativeStrategy.add(alternativePlan);
			}
		}
		ftSettings.setAltStrategy(new AlternativeStrategy(alternativeStrategy));
		return ftSettings;
	}

	/**
	 * Look for constraint settings.
	 *
	 * @param cList all constraint settings.
	 *
	 * @return constraint settings.
	 */
	private ConstraintSettings getConstraintSettings(List<PropertyConstraint> cList) {

		/* Set the default constraint settings */
		ConstraintSettings cSettings = new ConstraintSettings(null, null, 0);

		/* Iterate over all constraint settings and check for supported ones */
		for (PropertyConstraint cConstraint : cList) {
			if (cConstraint.getName().compareTo("C-latestStartingTime") == 0) {
				cSettings.setLatestStartingTime(Timestamp.valueOf(cConstraint.getValue()));
			} else if (cConstraint.getName().compareTo("C-latestFinishingTime") == 0) {
				cSettings.setLatestFinishingTime(Timestamp.valueOf(cConstraint.getValue()));
			} else if (cConstraint.getName().compareTo("C-maxRunningTime") == 0) {
				cSettings.setMaxRunningTime(Integer.valueOf(cConstraint.getValue()));
			}
		}
		return cSettings;
	}

	/**
	 * Read the AWS credentials. TODO do we need this?
	 *
	 * @return aws account object.
	 */
	private AWSAccount getAWSAccount() {
		String awsAccessKey = null;
		String awsSecretKey = null;
		String awsSessionToken = null;
		try {
			Properties propertiesFile = new Properties();
//			propertiesFile.load(LambdaHandler.class.getResourceAsStream(Utils.PATH_TO_CREDENTIALS));




			propertiesFile.load(new FileInputStream(Utils.PATH_TO_CREDENTIALS));

			//FileUtils.readFileToByteArray(new File(workflow))

			awsAccessKey = propertiesFile.getProperty("aws_access_key");
			awsSecretKey = propertiesFile.getProperty("aws_secret_key");
			if(propertiesFile.containsKey("aws_session_token")){
				awsSessionToken = propertiesFile.getProperty("aws_session_token");
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return new AWSAccount(awsAccessKey, awsSecretKey, awsSessionToken);
	}

	/**
	 * Read the IBM credentials. TODO do we need this?
	 *
	 * @return ibm account object.
	 */
	private IBMAccount getIBMAccount() {
		String ibmKey = null;
		try {
			Properties propertiesFile = new Properties();
//			propertiesFile.load(Local.class.getResourceAsStream(Utils.PATH_TO_CREDENTIALS));
			propertiesFile.load(new FileInputStream(Utils.PATH_TO_CREDENTIALS));

			ibmKey = propertiesFile.getProperty("ibm_api_key");
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return new IBMAccount(ibmKey);
	}

	/**
	 * Read the Azure credentials. TODO do we need this?
	 *
	 * @return azure account object.
	 */
	private AzureAccount getAzureAccount() {
		String azure_key = null;
		try {
			Properties propertiesFile = new Properties();
//			propertiesFile.load(Local.class.getResourceAsStream(Utils.PATH_TO_CREDENTIALS));
			propertiesFile.load(new FileInputStream(Utils.PATH_TO_CREDENTIALS));

			azure_key = propertiesFile.getProperty("azure_key");
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return new AzureAccount(azure_key);
	}


	/**
	 * Read the Google credentials. TODO do we need this?
	 *
	 * @return google account object.
	 */
	private GoogleFunctionAccount getGoogleAccount() {
		String google_key = null;
		try {
			Properties propertiesFile = new Properties();
//			propertiesFile.load(Local.class.getResourceAsStream(Utils.PATH_TO_CREDENTIALS));
			propertiesFile.load(new FileInputStream(Utils.PATH_TO_CREDENTIALS));

			google_key = propertiesFile.getProperty("google_sa_key");
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return new GoogleFunctionAccount(google_key);
	}

	public List<PropertyConstraint> getProperties() {
		return properties;
	}

	public List<PropertyConstraint> getConstraints() {
		return constraints;
	}

	public List<DataIns> getInput() {
		return input;
	}

	public List<DataOutsAtomic> getOutput() {
		return output;
	}
}
