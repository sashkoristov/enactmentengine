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
 * adapted by @author stefanpedratscher
 */
public class ParallelEndNode extends Node {

    /**
     * Logger for the parallel-end node class.
     */
    static final Logger logger = LoggerFactory.getLogger(ParallelEndNode.class);

    /**
     * Keeps track of the number of finished parents.
     */
    private int finishedParents = 0;

    /**
     * The output of the parallel specified in the workflow file.
     */
    private List<DataOuts> output;

    /**
     * The result of the parallel construct.
     */
    private Map<String, Object> parallelResult = new HashMap<>();

    /**
     * Clone of the current node.
     */
    private Node currentCopy;

    /**
     * Default constructor for a parallel-end node.
     *
     * @param name   of the parallel-end node.
     * @param type   of the parallel-end node.
     * @param output of the parallel-end node.
     */
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

        /* Check if all functions in the parallel node are finished */
        synchronized (this) {
            if (++finishedParents != parents.size()) {
                return false;
            }
        }

        /* Get the output of the executed parents */
        Map<String, Object> outputValues = new HashMap<>();

        /* Check if there is an output specified */
        if (output != null) {
            for (DataOuts data : output) {

                /* Define the output key */
                // TODO should we remove name?
                String key = name + "/" + data.getName();

                /* Check if the result contains the specified source */
                if (parallelResult.containsKey(data.getSource())) {
                    outputValues.put(key, parallelResult.get(data.getSource()));
                    continue;
                }

                /* Check for a collection result */
                outputValues.putAll(checkCollection(data, key));
            }
            logger.info("Executing {} ParallelEndNodeOld with output: {}", name, outputValues);

        }

        /* Pass the results to all children */
        for (Node node : children) {
            node.passResult(outputValues);
            node.call();
        }

        return true;
    }

    /**
     * Check for a collection output of the parallel section.
     * <p>
     * TODO can this be merged with the other function?
     *
     * @param dataOuts output specified in the workflow file.
     * @param key      on which the data should be added.
     * @return the output values of the optional collection.
     */
    private Map<String, Object> checkCollection(DataOuts dataOuts, String key) {

        Map<String, Object> outputValues = new HashMap<>();

        /* Iterate over all results of the parallel node */
        for (Entry<String, Object> inputElement : parallelResult.entrySet()) {

            /* Check if the defined source in the workflow file contains the actual result of the parallel node */
            if (dataOuts.getSource() != null && dataOuts.getSource().contains(inputElement.getKey())) {

                /* Check if the specified output is of type collection */
                if ("collection".equals(dataOuts.getType())) {

                    /* Default behaviour for collections is to pass the results from the executed branches as key-value pairs */
                    Object valueToPass = parallelResult;

                    /* Check if there are contraints (e.g. aggregation) defined */
                    if (dataOuts.getConstraints() != null) {
                        valueToPass = fulfillCollectionOutputConstraints(dataOuts.getConstraints(), parallelResult);
                    }

                    outputValues.put(key, valueToPass);
                    break;
                }
                outputValues.put(key, inputElement.getValue());
            }
        }
        return outputValues;
    }

    /**
     * Retrieves the results from the different parents and set them as result.
     *
     * @param input which should be added o the results.
     */
    @Override
    public void passResult(Map<String, Object> input) {
        synchronized (this) {

            /* Check if there is an output specified */
            if (output == null) {
                return;
            }

            /* Iterate over the output */
            for (DataOuts data : output) {

                /* Check if the input is specified in the output of the workflow file */
                if (input.containsKey(data.getSource())) {
                    parallelResult.put(data.getSource(), input.get(data.getSource()));
                }

                /* Add input data to the results if they are specified in the output */
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
     *
     * @return the result of the parallel node.
     */
    @Override
    public Map<String, Object> getResult() {
        return parallelResult;
    }

    /**
     * Clones this node and its children. Cloning is needed for ParallelFor
     * branches.
     *
     * @param endNode end node.
     * @return cloned node.
     * @throws CloneNotSupportedException on failure.
     */
    @Override
    public Node clone(Node endNode) throws CloneNotSupportedException {

        /* Check if already a clone */
        if (endNode == currentCopy) {
            return endNode;
        }

        /* Clone the node */
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

        /* Iterate over all constraints */
        for (PropertyConstraint constraint : constraints) {

            /* Check for aggregation constraint */
            if ("aggregation".equals(constraint.getName())) {
                if ("+".equals(constraint.getValue())) {

                    /* Combine the results from the executed branches into one collection */
                    JsonArray arr = new JsonArray(data.values().size());
                    for (Object value : data.values()) {
                        arr.addAll((JsonArray) value);
                    }
                    result = arr;
                } else if (",".equals(constraint.getValue())) {
                    /* Same as default behaviour - pass the results from the executed branches as key-value pairs */
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
