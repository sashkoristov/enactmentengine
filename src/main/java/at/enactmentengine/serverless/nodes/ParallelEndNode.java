package at.enactmentengine.serverless.nodes;

import at.uibk.dps.afcl.functions.objects.DataOuts;
import at.uibk.dps.afcl.functions.objects.PropertyConstraint;
import com.google.gson.JsonArray;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.Map.Entry;

/**
 * Control node which manages the tasks at the end of a parallel loop.
 *
 * @author markusmoosbrugger, jakobnoeckl
 */
public class ParallelEndNode extends Node {
    final static Logger logger = LoggerFactory.getLogger(ParallelEndNode.class);
    private int waitcounter = 0;
    private List<DataOuts> output;
    private Map<String, Object> parallelResult = new HashMap<>();
    private Node currentCopy;

    public ParallelEndNode(String name, String type, List<DataOuts> output) {
        super(name, type);
        this.output = output;
    }

    /**
     * Counts the number of invocations and resumes with passing the results to the
     * children if all parents have finished.
     */
    @Override
    public Boolean call() throws Exception {
        synchronized (this) {
            if (++waitcounter != parents.size()) {
                return false;
            }
        }

        Map<String, Object> outputValues = new HashMap<>();
        if (output != null) {
            for (DataOuts data : output) {
                String key = name + "/" + data.getName();
                if (parallelResult.containsKey(data.getSource())) {
                    outputValues.put(key, parallelResult.get(data.getSource()));
                    continue;
                }
                for (Entry<String, Object> inputElement : parallelResult.entrySet()) {
                    if (data.getSource() != null && data.getSource().contains(inputElement.getKey())) {
                        if ("collection".equals(data.getType())) {
                            // Default behaviour for collections is to pass the results
                            // from the executed branches as key-value pairs
                            Object valueToPass = parallelResult;
                            if (data.getConstraints() != null) {
                                valueToPass = fulfillCollectionOutputConstraints(data.getConstraints(), parallelResult);
                            }
                            outputValues.put(key, valueToPass);
                            break;
                        }
                        outputValues.put(key, inputElement.getValue());
                    }
                }
            }
            logger.info("Executing " + name + " ParallelEndNodeOld with output: " + outputValues.toString());

        }

        for (Node node : children) {
            node.passResult(outputValues);
            node.call();

        }
        return true;
    }

    /**
     * Retrieves the results from the different parents and set them as result.
     */
    @Override
    public void passResult(Map<String, Object> input) {
        synchronized (this) {
            if (output == null) {
                return;
            }
            for (DataOuts data : output) {
                if (input.containsKey(data.getSource())) {
                    parallelResult.put(data.getSource(), input.get(data.getSource()));
                }
                for (Entry<String, Object> inputElement : input.entrySet()) {
                    if (data.getSource() != null && data.getSource().contains(inputElement.getKey())) {
                        parallelResult.put(inputElement.getKey(), input.get(inputElement.getKey()));
                    }
                }
            }

        }

    }

    /**
     * Returns the result.
     */
    @Override
    public Map<String, Object> getResult() {
        return parallelResult;
    }

    /**
     * Clones this node and its children. Cloning is needed for ParallelFor
     * branches.
     */
    @Override
    public Node clone(Node endNode) throws CloneNotSupportedException {
        if (endNode == currentCopy) {
            return endNode;
        }

        Node node = (Node) super.clone();
        node.children = new ArrayList<>();
        for (Node childrenNode : children) {
            node.children.add(childrenNode.clone(endNode));
        }
        currentCopy = node;
        return node;

    }

    /**
     * Fulfills the given collection output constraints for the given data elements.
     *
     * @param constraints the constraints to consider
     * @param data        the data elements
     * @return the resulting data elements
     */
    protected Object fulfillCollectionOutputConstraints(List<PropertyConstraint> constraints,
                                                        Map<String, Object> data) {
        Object result = data;
        for (PropertyConstraint constraint : constraints) {
            if (constraint.getName().equals("aggregation")) {
                if (constraint.getValue().equals("+")) {
                    // Combine the results from the executed branches into one collection
                    JsonArray arr = new JsonArray(data.values().size());
                    for (Object value : data.values()) {
                        arr.addAll((JsonArray) value);
                    }
                    result = arr;
                } else if (constraint.getValue().equals(",")) {
                    // Same as default behaviour - pass the results from the executed branches as key-value pairs
                    result = data;
                } else {
                    throw new NotImplementedException("Aggregation type " + constraint.getValue() + " not implemented.");
                }
            } else {
                throw new NotImplementedException("Constraint " + constraint.getName() + " not implemented.");
            }
        }
        return result;
    }

}
