package at.enactmentengine.serverless.nodes;

import at.enactmentengine.serverless.exception.MissingInputDataException;
import at.enactmentengine.serverless.exception.MissingResourceLinkException;
import at.enactmentengine.serverless.main.LambdaHandler;
import at.uibk.dps.AWSAccount;
import at.uibk.dps.FaultToleranceEngine;
import at.uibk.dps.IBMAccount;
import at.uibk.dps.SocketUtils;
import at.uibk.dps.afcl.functions.objects.DataIns;
import at.uibk.dps.afcl.functions.objects.DataOutsAtomic;
import at.uibk.dps.afcl.functions.objects.PropertyConstraint;
import at.uibk.dps.communication.InvocationLogManagerRequest;
import at.uibk.dps.communication.InvocationLogManagerRequestFactory;
import at.uibk.dps.communication.entity.Invocation;
import at.uibk.dps.database.SQLLiteDatabase;
import at.uibk.dps.exception.InvokationFailureException;
import at.uibk.dps.exception.LatestFinishingTimeException;
import at.uibk.dps.exception.LatestStartingTimeException;
import at.uibk.dps.exception.MaxRunningTimeException;
import at.uibk.dps.function.AlternativeStrategy;
import at.uibk.dps.function.ConstraintSettings;
import at.uibk.dps.function.FaultToleranceSettings;
import at.uibk.dps.function.Function;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import jFaaS.Gateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Socket;
import java.sql.Timestamp;
import java.util.*;

/**
 * Class which handles the execution of a function.
 *
 * @author markusmoosbrugger, jakobnoeckl
 *
 * adapted by @author stefanpedratscher
 */
public class FunctionNode extends Node {

    private static final Logger logger = LoggerFactory.getLogger(FunctionNode.class);

    private static int counter = 0;
    private int executionId;

    private List<PropertyConstraint> constraints;
    private List<PropertyConstraint> properties;
    private List<DataOutsAtomic> output;
    private List<DataIns> input;
    private static Gateway gateway = new Gateway("credentials.properties");
    private Map<String, Object> result;

    private static final String PROTOCOL = "https://";

    public FunctionNode(String name, String type, List<PropertyConstraint> properties,
                        List<PropertyConstraint> constraints, List<DataIns> input, List<DataOutsAtomic> output, int executionId) {
        super(name, type);
        this.output = output;
        if (output == null) {
            this.output = new ArrayList<>();
        }
        this.properties = properties;
        this.constraints = constraints;
        this.input = input;
        this.executionId = executionId;
    }

    /**
     * Checks the inputs, invokes function and passes results to children.
     */
    @Override
    public Boolean call() throws Exception {

        int id;
        synchronized (this) {
            id = counter++;
        }

        Map<String, Object> outVals = new HashMap<>();
        String resourceLink = getResourceLink();
        logger.info("Executing function {} at resource: {} [{}ms], id={}", name, resourceLink, System.currentTimeMillis(), id);

        // Check if all input data is sent by last node and create an input map
        Map<String, Object> functionInputs = new HashMap<>();
        try {
            if (input != null) {
                for (DataIns data : input) {
                    if (!dataValues.containsKey(data.getSource())) {
                        throw new MissingInputDataException(FunctionNode.class.getCanonicalName() + ": " + name
                                + " needs " + data.getSource() + "!");
                    } else {
                        if (data.getPassing() != null && data.getPassing()) {
                            outVals.put(name + "/" + data.getName(), dataValues.get(data.getSource()));
                        } else {
                            functionInputs.put(data.getName(), dataValues.get(data.getSource()));
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

        //Simulate Availability
        SQLLiteDatabase db = new SQLLiteDatabase("jdbc:sqlite:Database/FTDatabase.db");
        double simAvail = db.getSimulatedAvail(resourceLink);
        functionInputs = checkFunctionSimAvail(simAvail, functionInputs);


        logFunctionInput(functionInputs, id);

        Function functionToInvoke = parseNodeFunction(resourceLink, functionInputs);

        long start = System.currentTimeMillis();
        String resultString = invokeFunction(functionToInvoke, resourceLink, functionInputs);
        long end = System.currentTimeMillis();

        logFunctionOutput(start, end, resultString, id);

        getValuesParsed(resultString, outVals);
        for (Node node : children) {
            node.passResult(outVals);
            node.call();
        }
        result = outVals;

        String[] providerRegion = getProviderAndRegion(resourceLink);

        String status = checkResultSuccess(resultString);

        Invocation functionInvocation = new Invocation(
                resourceLink,
                providerRegion[0],
                providerRegion[1],
                new Timestamp(start + TimeZone.getTimeZone("Europe/Rome").getOffset(start)),
                new Timestamp(end + TimeZone.getTimeZone("Europe/Rome").getOffset(start)),
                (end - start),
                status,
                null,
                executionId
        );
        logFunctionInvocation(functionInvocation);

        return true;
    }

    private Map<String, Object> checkFunctionSimAvail(double simAvail, Map<String, Object> functionInputs) {
        if (simAvail != 1) { //if this functions avail should be simulated
            functionInputs.put("availability", simAvail);
        }
        return functionInputs;
    }

    private String checkResultSuccess(String resultString) {
        return resultString.contains("error:") ? "ERROR" : "OK";
    }

    private Function parseNodeFunction(String resourceLink, Map<String, Object> functionInputs) {
        Function functionToInvoke = null;
        try {
            functionToInvoke = parseThisNodesFunction(resourceLink, functionInputs);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return functionToInvoke;
    }

    private void logFunctionOutput(long start, long end, String resultString, int id) {
        if (resultString.length() > 100000) {
            logger.info("Function took: {} ms. Result: too large [{}ms], id={}", (end - start), System.currentTimeMillis(), id);
        } else {
            logger.info("Function took: {} ms. Result: {} : {} [{}ms], id={}", (end - start), name, resultString, System.currentTimeMillis(), id);
        }
    }

    private String invokeFunction(Function functionToInvoke, String resourceLink, Map<String, Object> functionInputs) throws IOException, MaxRunningTimeException, LatestFinishingTimeException, LatestStartingTimeException, InvokationFailureException {
        String resultString = null;
        if (functionToInvoke != null && (functionToInvoke.hasConstraintSet() || functionToInvoke.hasFTSet())) { // Invoke with Fault Tolerance Module
            FaultToleranceEngine ftEngine = new FaultToleranceEngine(getAWSAccount(), getIBMAccount());
            try {
                logger.info("Invoking function with fault tolerance...");
                resultString = ftEngine.InvokeFunctionFT(functionToInvoke);
            } catch (Exception e) {
                result = null;
                throw e;
            }
        } else {
            resultString = gateway.invokeFunction(resourceLink, functionInputs).toString();
        }
        return resultString;
    }

    private void logFunctionInput(Map<String, Object> functionInputs, int id) {
        if (functionInputs.size() > 20) {
            logger.info("Input for function is large [{}ms], id={}", System.currentTimeMillis(), id);
        } else {
            logger.info("Input for function {} : {} [{}ms], id={}", name, functionInputs, System.currentTimeMillis(), id);
        }
    }

    private void logFunctionInvocation(Invocation functionInvocation){

        logger.info("Connecting to logger service...");
        try (Socket loggerService = new Socket("logger-service", 9005)) {

            InvocationLogManagerRequest invocationLogManagerRequest = InvocationLogManagerRequestFactory.getInsertFunctionInvocationRequest(functionInvocation, executionId);
            logger.info("Sending request to logger-service...");
            SocketUtils.sendJsonObject(loggerService, invocationLogManagerRequest);

            logger.info("Closing connection to logger service...");
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }

    }

    private String[] getProviderAndRegion(String resourceLink){
        String[] res = new String[2];
        res[0] = "NOT_FOUND";
        res[1] = "NOT_FOUND";
        if(resourceLink.contains("functions.cloud.ibm")){
            res[0] = "IBM";
            res[1] = resourceLink.split(PROTOCOL)[1].split("\\.functions\\.cloud\\.ibm")[0];
        } else if(resourceLink.contains("functions.appdomain.cloud")){
            res[0] = "IBM";
            res[1] = resourceLink.split(PROTOCOL)[1].split("\\.functions\\.appdomain\\.cloud")[0];
        }else if(resourceLink.contains("arn")){
            res[0] = "AWS";
            res[1] = resourceLink.split("lambda:")[1].split(":")[0];
        } else if(resourceLink.contains("cloudfunctions.net")){
            res[0] = "GOOGLE";
            res[1] = resourceLink.split(PROTOCOL)[1].split("\\.cloudfunctions\\.net")[0];
        } else if(resourceLink.contains("azure")){
            res[0] = "AZURE";
        } else if(resourceLink.contains("fc.aliyuncs")){
            res[0] = "ALIBABA";
        }
        return res;
    }

    /**
     * Parses the result string into a map. Supported types for the result
     * elements are number, string and collection.
     *
     * @param resultString The result string from the FaaS function.
     * @param out          The output map of this function.
     * @return
     */
    private boolean getValuesParsed(String resultString, Map<String, Object> out) {
        if (resultString == null || "null".equals(resultString)){
            return false;
        }
        try {
            for (DataOutsAtomic data : output) {

                JsonObject jso = getReturnAsJson(resultString, data);

                if (out.containsKey(name + "/" + data.getName())) {
                    continue;
                }
                switch (data.getType()) {
                    case "number":
                        Object number = jso.get(data.getName()).getAsInt();
                        out.put(name + "/" + data.getName(), number);
                        break;
                    case "string":
                        out.put(name + "/" + data.getName(), jso.get(data.getName()).getAsString());
                        break;
                    case "collection":
                        // array stays array to later decide which type
                        out.put(name + "/" + data.getName(), jso.get(data.getName()).getAsJsonArray());
                        break;
                    case "object":
                        out.put(name + "/" + data.getName(), jso);
                        break;
                    default:
                        logger.info("Error while trying to parse key in function {}", name);
                        break;
                }
            }
            return true;

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            logger.error("Error while trying to parse key in function {}", name);
            return false;
        }
    }

    /**
     * Get the result as json object.
     *
     * @return json result
     */
    JsonObject getReturnAsJson(String resultString, DataOutsAtomic data){
        JsonObject jso;
        try {
            jso = new Gson().fromJson(resultString, JsonObject.class);
        } catch (com.google.gson.JsonSyntaxException e) {
            // If there is no JSON object as return value create one
            jso = new JsonObject();
            jso.addProperty(data.getName(), resultString);
        }
        return jso;
    }


    /**
     * Sets the FaaSInvoker depending on the resource link. Currently AWS
     * Lambda, OpenWhisk, IBM Cloud Functions and Docker are supported.
     *
     * @throws MissingResourceLinkException
     */
    private String getResourceLink() throws MissingResourceLinkException {
        String resourceLink = null;

        if (properties == null) {
            throw new MissingResourceLinkException("No properties specified " + this.toString());
        }
        for (PropertyConstraint p : properties) {
            if (p.getName().equals("resource")) {
                resourceLink = p.getValue();
                break;
            }
        }
        if (resourceLink == null) {
            throw new MissingResourceLinkException("No resource link on function node " + this.toString());
        }

        resourceLink = resourceLink.substring(resourceLink.indexOf(":") + 1);
        return resourceLink;
    }


    /**
     * Sets the dataValues and passes the result to all children.
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
     * Returns the result.
     */
    @Override
    public Map<String, Object> getResult() {
        return result;
    }

    /**
     * parses function Settings.
     * returns FunctionInvocation Object with correct Constraint and FT Settings
     */
    private Function parseThisNodesFunction(String resourceLink, Map<String, Object> functionInputs) {
        List<PropertyConstraint> constraintList = this.constraints;
        // Split Constraints and FT stuff

        List<PropertyConstraint> cList = new LinkedList<>();
        List<PropertyConstraint> ftList = new LinkedList<>();

        // Constraints and Properties are optional, so check if some of them are set
        if (constraintList == null) {
            return null;
        }
        for (PropertyConstraint constraint : constraintList) {
            if (constraint.getName().startsWith("FT-")) {
                ftList.add(constraint);
            } else if (constraint.getName().startsWith("C-")) {
                cList.add(constraint);
            }
        }

        // FT Parsing
        FaultToleranceSettings ftSettings = getFaultToleranceSettings(ftList, functionInputs);

        // ConstraintParsing
        ConstraintSettings cSettings = getConstraintSettings(cList);

        return getFinalFunction(ftSettings, cSettings, resourceLink, functionInputs);
    }

    private FaultToleranceSettings getFaultToleranceSettings(List<PropertyConstraint> ftList, Map<String, Object> functionInputs) {
        FaultToleranceSettings ftSettings = new FaultToleranceSettings(0);
        List<List<Function>> altStrat = new LinkedList<>();
        for (PropertyConstraint ftConstraint : ftList) {
            if (ftConstraint.getName().compareTo("FT-Retries") == 0) {
                ftSettings.setRetries(Integer.valueOf(ftConstraint.getValue()));
            } else if (ftConstraint.getName().startsWith("FT-AltPlan-")) {
                List<Function> altPlan = new LinkedList<>();
                String workingString = ftConstraint.getValue().substring(ftConstraint.getValue().indexOf(";") + 1);
                while (workingString.contains(";")) {
                    String funcString = workingString.substring(0, workingString.indexOf(";"));
                    Function tmpFunc = new Function(funcString, this.type, functionInputs);
                    workingString = workingString.substring(workingString.indexOf(";") + 1);
                    altPlan.add(tmpFunc);
                }
                altStrat.add(altPlan);
            }
        }
        if (altStrat.isEmpty()) {
            AlternativeStrategy altStrategy = new AlternativeStrategy(altStrat);
            ftSettings.setAltStrategy(altStrategy);
        }
        return ftSettings;
    }

    private ConstraintSettings getConstraintSettings(List<PropertyConstraint> cList) {
        ConstraintSettings cSettings = new ConstraintSettings(null, null, 0);
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

    private Function getFinalFunction(FaultToleranceSettings ftSettings, ConstraintSettings cSettings, String resourceLink, Map<String, Object> functionInputs) {
        Function finalFunc = null;
        if (!ftSettings.isEmpty() && !cSettings.isEmpty()) { // Both
            finalFunc = new Function(resourceLink, this.type, functionInputs, ftSettings, cSettings);
        } else if (!ftSettings.isEmpty() && cSettings.isEmpty()) { // Only FT
            finalFunc = new Function(resourceLink, this.type, functionInputs, ftSettings);
        } else if (ftSettings.isEmpty() && !cSettings.isEmpty()) { // only constraints
            finalFunc = new Function(resourceLink, this.type, functionInputs, cSettings);
        } else { // No Constraints or FT set
            finalFunc = new Function(resourceLink, this.type, functionInputs);
        }
        return finalFunc;
    }

    private AWSAccount getAWSAccount() {
        String awsAccessKey = null;
        String awsSecretKey = null;
        try {
            Properties propertiesFile = new Properties();
            propertiesFile.load(LambdaHandler.class.getResourceAsStream("/credentials.properties"));
            awsAccessKey = propertiesFile.getProperty("aws_access_key");
            awsSecretKey = propertiesFile.getProperty("aws_secret_key");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return new AWSAccount(awsAccessKey, awsSecretKey);
    }

    private IBMAccount getIBMAccount() {
        String ibmKey = null;
        try {
            Properties propertiesFile = new Properties();
            propertiesFile.load(LambdaHandler.class.getResourceAsStream("/credentials.properties"));
            ibmKey = propertiesFile.getProperty("ibm_api_key");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return new IBMAccount(ibmKey);
    }
}
