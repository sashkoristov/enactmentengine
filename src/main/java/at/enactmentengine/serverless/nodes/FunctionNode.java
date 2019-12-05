package at.enactmentengine.serverless.nodes;

import at.enactmentengine.serverless.exception.MissingInputDataException;
import at.enactmentengine.serverless.exception.MissingResourceLinkException;
import at.enactmentengine.serverless.main.LambdaHandler;
import com.dps.afcl.functions.objects.DataIns;
import com.dps.afcl.functions.objects.DataOutsAtomic;
import com.dps.afcl.functions.objects.PropertyConstraint;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dps.invoker.*;
import org.apache.commons.lang.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Class which handles the execution of a function.
 *
 * @author markusmoosbrugger, jakobnoeckl
 */
public class FunctionNode extends Node {

    final static Logger logger = LoggerFactory.getLogger(FunctionNode.class);

    private List<PropertyConstraint> properties;
    private List<DataOutsAtomic> output;
    private List<DataIns> input;
    private FaaSInvoker faasInvoker = new DockerInvoker();
    private Map<String, Object> result;

    public FunctionNode(String name, String type, List<PropertyConstraint> properties, List<DataIns> input, List<DataOutsAtomic> output) {
        super(name, type);
        this.output = output;
        if (output == null) {
            this.output = new ArrayList<>();
        }
        this.properties = properties;
        this.input = input;
    }

    /**
     * Checks the inputs, invokes function and passes results to children.
     */
    @Override
    public Boolean call() throws Exception {
        Map<String, Object> outVals = new HashMap<>();
        String resourceLink = setFaaSInvoker();
        logger.info("Executing function " + name + " at resource: " + resourceLink + " [" + System.currentTimeMillis() + "ms]");

        // Check if all input data is sent by last node and create an input map
        Map<String, Object> functionInputs = new HashMap<>();

        try {
            if (input != null) {
                for (DataIns data : input) {
                    if (!dataValues.containsKey(data.getSource())) {
                        throw new MissingInputDataException(
                                FunctionNode.class.getCanonicalName() + ": " + name + " needs " + data.getSource() + "!");
                    } else {
                        // if (data.getPass()!=null && data.getPass().equals("true"))
                        if (data.getPassing() != null && data.getPassing())
                            outVals.put(name + "/" + data.getName(), dataValues.get(data.getSource()));
                        else
                            functionInputs.put(data.getName(), dataValues.get(data.getSource()));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (functionInputs.size() > 20) {
            logger.info("Input for function is large" + " [" + System.currentTimeMillis() + "ms]");
        } else {
            logger.info("Input for function " + name + " : " + functionInputs + " [" + System.currentTimeMillis() + "ms]");
        }
        String resultString = (String) faasInvoker.invokeFunction(resourceLink, functionInputs);
        if (resultString.length() > 100) {
            logger.info("Result of function is large" + "[" + System.currentTimeMillis() + "ms]");
        } else {
            logger.info("Result from function " + name + " : " + resultString + " [" + System.currentTimeMillis() + "ms]");
        }
        getValuesParsed(resultString, outVals);
        for (Node node : children) {
            node.passResult(outVals);
            node.call();
        }
        result = outVals;
        return true;
    }

    /**
     * Parses the result string into a map. Supported types for the result elements
     * are number, string and collection.
     *
     * @param resultString The result string from the FaaS function.
     * @param out          The output map of this function.
     * @return
     */
    private void getValuesParsed(String resultString, Map<String, Object> out) {
        if (resultString == null || resultString.equals("null"))
            return;
        try {
            JsonObject jso = new Gson().fromJson(resultString, JsonObject.class);

            for (DataOutsAtomic data : output) {
                if (out.containsKey(name + "/" + data.getName())) {
                    continue;
                }
                if (data.getType().equals("number")) {
                    Object number = (int) jso.get(data.getName()).getAsInt();
                    out.put(name + "/" + data.getName(), number);
                } else if (data.getType().equals("string")) {
                    out.put(name + "/" + data.getName(), jso.get(data.getName()).getAsString());
                } else if (data.getType().equals("collection")) {
                    // array stays array to later decide which type
                    out.put(name + "/" + data.getName(), jso.get(data.getName()).getAsJsonArray());
                } else if (data.getType().equals("object")) {
                    out.put(name + "/" + data.getName(), jso);
                } else {
                    logger.info("Error while trying to parse key in function " + name);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            logger.info("Error while trying to parse key in function " + name);
        }
    }

    /**
     * Sets the FaaSInvoker depending on the resource link. Currently AWS Lambda,
     * OpenWhisk, IBM Cloud Functions and Docker are supported.
     *
     * @throws MissingResourceLinkException
     */
    private String setFaaSInvoker() throws MissingResourceLinkException {
        String resourceLink = null;
        // if no properies are set the dummy invoker is used.
        if (properties == null) {
            this.faasInvoker = new DummyInvoker();
            return null;
        }
        for (PropertyConstraint p : properties) {
            if (p.getName().equals("resource")) {
                resourceLink = p.getValue();
                break;
            }
        }
        if (resourceLink == null)
            throw new MissingResourceLinkException("No resource link on function node " + this.toString());

        resourceLink = resourceLink.substring(resourceLink.indexOf(":") + 1);

        // depending on the resource link the different FaaS Invoker are selected
        if (resourceLink.contains("eu-gb") || resourceLink.contains("amazonaws")) {
            this.faasInvoker = new HTTPInvoker();
        } else if (resourceLink.contains("arn:")) {
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

            this.faasInvoker = new LambdaInvoker(awsAccessKey, awsSecretKey);
        } else if (resourceLink.contains("138.232.66.185:31001")) {
            String openWhiskKey = null;
            try {
                Properties properties = new Properties();
                properties.load(LambdaHandler.class.getResourceAsStream("/credentials.properties"));
                openWhiskKey = properties.getProperty("ibm_api_key");
            } catch (Exception e) {
                e.printStackTrace();
            }
            this.faasInvoker = new OpenWhiskInvoker(openWhiskKey);
        } else if (resourceLink.contains("tcp")) {
            this.faasInvoker = new DockerInvoker();
        } else {
            throw new NotImplementedException("No FaaS Invoker which matches this link.");
        }
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
                e.printStackTrace();
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

}
