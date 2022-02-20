package at.enactmentengine.serverless.object;

import at.enactmentengine.serverless.exception.MissingResourceLinkException;
import at.enactmentengine.serverless.exception.RegionDetectionException;
import at.enactmentengine.serverless.nodes.Node;
import at.uibk.dps.afcl.functions.objects.DataOutsAtomic;
import at.uibk.dps.afcl.functions.objects.PropertyConstraint;
import at.uibk.dps.function.AlternativeStrategy;
import at.uibk.dps.function.ConstraintSettings;
import at.uibk.dps.function.FaultToleranceSettings;
import at.uibk.dps.function.Function;

/* remove not available dependency */
//import at.uibk.dps.socketutils.ConstantsNetwork;
//import at.uibk.dps.socketutils.UtilsSocket;
//import at.uibk.dps.socketutils.entity.Invocation;
//import at.uibk.dps.socketutils.logger.RequestLoggerInvocationWrite;
//import at.uibk.dps.socketutils.logger.UtilsSocketLogger;

import at.uibk.dps.util.Provider;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.Socket;
import java.sql.Timestamp;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


/**
 * Utility class for the enactment-engine.
 *
 * @author stefanpedratscher, extended by @author mikahautz
 */
public class Utils {

    /**
     * The protocol for the resource links.
     */
    private static final String PROTOCOL = "https://";
    /**
     * Path to the credentials properties file
     */
    public static String PATH_TO_CREDENTIALS = "credentials.properties";
    /**
     * Determine if the availability should be simulated.
     */
    public static boolean SIMULATE_AVAILABILITY = false;

    /**
     * Detect the provider with given function url.
     *
     * @param resourceLink the resource of the function.
     *
     * @return the detected provider.
     */
    public static Provider detectProvider(String resourceLink) {

        /* Check for providers */
        if (resourceLink.contains(".functions.cloud.ibm.com/") || resourceLink.contains(".functions.appdomain.cloud/")) {
            return Provider.IBM;
        } else if (resourceLink.contains("arn:aws:lambda:")) {
            return Provider.AWS;
        } else if (resourceLink.contains("cloudfunctions.net")) {
            return Provider.GOOGLE;
        } else if (resourceLink.contains("azure")) {
            return Provider.AZURE;
        } else if (resourceLink.contains("fc.aliyuncs")) {
            return Provider.ALIBABA;
        }

        // Inform Scheduler Provider Detection Failed
        return Provider.FAIL;
    }

    /**
     * Detect the provider with given function url.
     *
     * @param resourceLink the resource of the function.
     *
     * @return detected region.
     *
     * @throws RegionDetectionException on region detection failure.
     */
    public static String detectRegion(String resourceLink) throws RegionDetectionException {
        String regionName;
        switch (Utils.detectProvider(resourceLink)) {
            case IBM:
                if (resourceLink.contains("functions.cloud.ibm")) {
                    return resourceLink.split(PROTOCOL)[1].split("\\.functions\\.cloud\\.ibm")[0];
                } else if (resourceLink.contains("functions.appdomain.cloud")) {
                    return resourceLink.split(PROTOCOL)[1].split("\\.functions\\.appdomain\\.cloud")[0];
                }
                return resourceLink.split(PROTOCOL)[1].split("\\.functions\\.cloud\\.ibm")[0];
            case AWS:
                return resourceLink.split("lambda:")[1].split(":")[0];
            case GOOGLE:
                return resourceLink.split(PROTOCOL)[1].split("\\.cloudfunctions\\.net")[0];
            case AZURE:
                throw new RegionDetectionException("Azure currently not supported.");
            case ALIBABA:
                throw new RegionDetectionException("Alibaba currently not supported.");
            default:
                throw new RegionDetectionException("Failed to detect Region and Provider.");
        }
    }

    /**
     * Get the resource link of the base function.
     *
     * @return the resource link of the base function.
     *
     * @throws MissingResourceLinkException on missing resource link.
     */
    public static String getResourceLink(List<PropertyConstraint> properties, Node node) throws MissingResourceLinkException {

        /* Check if there are properties specified */
        if (properties == null) {
            throw new MissingResourceLinkException("No properties specified " + node.toString());
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
            throw new MissingResourceLinkException("No resource link on function node " + node.toString());
        }

        if (!resourceLink.startsWith("http") && !resourceLink.startsWith("arn")) {

            /* Remove the programming language of the resource link */
            resourceLink = resourceLink.substring(resourceLink.indexOf(":") + 1);
        }

        return resourceLink;
    }

    /**
     * Parse the fault tolerance constraints.
     *
     * @param resourceLink   the resource link of the base function.
     * @param functionInputs inputs to the base function.
     *
     * @return function object with correctly set ft values.
     */
    public static Function parseFTConstraints(String resourceLink, Map<String, Object> functionInputs, List<PropertyConstraint> constraints,
                                              String type, String name, int loopCounter) {

        /* Keeps track of all constraint settings */
        List<PropertyConstraint> cList = new LinkedList<>();

        /* Keeps track of all fault tolerance settings */
        List<PropertyConstraint> ftList = new LinkedList<>();

        /* Check if there are constraints set */
        if (constraints == null) {
            return null;
        }

        /* Iterate over constraints and look for according settings */
        for (PropertyConstraint constraint : constraints) {
            if (constraint.getName().startsWith("FT-")) {
                ftList.add(constraint);
            } else if (constraint.getName().startsWith("C-")) {
                cList.add(constraint);
            }
        }

        /* Parse fault tolerance settings */
        FaultToleranceSettings ftSettings = getFaultToleranceSettings(ftList, functionInputs, type, name, loopCounter);

        /* Parse constraint settings */
        ConstraintSettings cSettings = getConstraintSettings(cList);

        return new Function(resourceLink, name, type, loopCounter, functionInputs, ftSettings.isEmpty() ? null : ftSettings,
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
    private static FaultToleranceSettings getFaultToleranceSettings(List<PropertyConstraint> ftList, Map<String, Object> functionInputs,
                                                                    String type, String name, int loopCounter) {

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
                    String deploymentString = null;
                    // id and deployment are divided by two #
                    if (funcString.contains("##")) {
                        deploymentString = funcString.substring(funcString.indexOf("##") + 2);
                        funcString = funcString.substring(0, funcString.indexOf("##"));
                    }
                    Function tmpFunc = new Function(funcString, name, type, loopCounter, functionInputs);
                    tmpFunc.setDeployment(deploymentString);
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
    private static ConstraintSettings getConstraintSettings(List<PropertyConstraint> cList) {

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
     * Check for error object in the json result of the cloud function.
     *
     * @param resultString json result of the base function.
     *
     * @return status og the execution.
     */
    public static Status checkResultSuccess(String resultString) {
        return (resultString.contains("error:") || resultString.contains("\"error\":")) ? Status.ERROR : Status.SUCCESS;
    }

    /* code snippet not working because of removed dependency 'com.github.ApolloCEC:socketUtils:-SNAPSHOT' */
//    /**
//     * Send a request to store the function invocation in the logging database.
//     *
//     * @param functionInvocation to store in the database.
//     */
//    public static void storeInDBFunctionInvocation(Logger logger, Invocation functionInvocation, int executionId) {
//
//        logger.info("Connecting to logger service...");
//        try (Socket loggerService = new Socket(ConstantsNetwork.LOGGER_SERVICE_HOST,
//                ConstantsNetwork.LOGGER_SERVICE_PORT)) {
//
//            RequestLoggerInvocationWrite invocationLogManagerRequest = UtilsSocketLogger
//                    .generateRequestInvocationWrite(functionInvocation, executionId);
//            logger.info("Sending request to logger-service...");
//            UtilsSocket.sendJsonObject(loggerService.getOutputStream(), invocationLogManagerRequest);
//
//            logger.info("Closing connection to logger service...");
//        } catch (IOException e) {
//            logger.error(e.getMessage(), e);
//        }
//    }

    /**
     * Convert the stringified json to a json object representing the function output.
     *
     * @param resultString stringified json.
     * @param data         outputs of the base function.
     *
     * @return json object representing the base function output.
     */
    public static JsonObject generateJson(String resultString, DataOutsAtomic data) {
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
}
