package at.enactmentengine.serverless.nodes;

import at.uibk.dps.afcl.functions.objects.DataOuts;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Control node which manages the tasks at the end of a parallel for loop.
 *
 * @author markusmoosbrugger, jakobnoeckl
 * adapted by @author stefanpedratscher
 */
public class ParallelForEndNode extends Node {

    /**
     * Logger for parallel-for-end node.
     */
    static final Logger logger = LoggerFactory.getLogger(ParallelForEndNode.class);

    /**
     * Keeps track of the number of finished parents.
     */
    private int finishedParents = 0;

    /**
     * Output of the parallel-for-end node defined in the workflow file.
     */
    private List<DataOuts> output;

    /**
     * The result of the parallel-for node.
     */
    private Map<String, Object> parallelForResult = new HashMap<>();

    /**
     * The number of parents (number of functions in the parallelFor).
     */
    private int numberOfParents;

    /**
     * Default constructor for a parallel-for-end node
     *
     * @param name of the parallel-for node.
     * @param type of the parallel-for node.
     * @param output defined output of the parallel-for node.
     */
    public ParallelForEndNode(String name, String type, List<DataOuts> output) {
        super(name, type);
        this.output = output;
    }

    /**
     * Counts the number of invocations and resumes with passing the results to the
     * children if all parents have finished.
     */
    @Override
    public Boolean call() throws Exception {

        /* Check if all functions in the parallel-for are finished */
        synchronized (this) {
            if (++finishedParents != numberOfParents) {
                return false;
            }
        }

        /* Prepare the output of the node */
        Map<String, Object> outputValues = new HashMap<>();

        /* Check if there is an output specified in the workflow file */
        if (output != null) {
            for (DataOuts data : output) {

                /* Define the output key */
                // TODO should we remove name?
                String key = name + "/" + data.getName();

                /* Check if the result contains the specified source */
                if (parallelForResult.containsKey(data.getSource())) {
                    outputValues.put(key, parallelForResult.get(data.getSource()));
                } else if ("collection".equals(data.getType())) {
                    outputValues.put(key, parallelForResult);
                }
            }
        }

        logger.info("Executing {} ParallelForEndNodeOld with output: {}", name, outputValues);

        /* Pass results to every child */
        for (Node node : children) {
            node.passResult(outputValues);
            node.call();
        }

        return true;
    }

    /**
     * Retrieves the results from the different parents and set them as result.
     *
     * @param input which should be passed.
     */
    @Override
    public void passResult(Map<String, Object> input) {
        synchronized (this) {

            /* Check if an output is specified */
            if (output != null) {

                /* Iterate over output and handle the results */
                for (DataOuts data : output) {
                    if (input.containsKey(data.getSource())) {
                        handlePassResults(data, input);
                    }
                }
            }
        }
    }

    /**
     * Handle the passing of the results.
     *
     * @param dataOuts output specified in the workflow file.
     * @param input which should be passed.
     */
    private void handlePassResults(DataOuts dataOuts, Map<String, Object> input) {

        /* Check for collection type */
        if ("collection".equals(dataOuts.getType())) {

            JsonArray resultArray;

            /* Use existing or create a new array */
            if (parallelForResult.containsKey(dataOuts.getSource())) {
                resultArray = (JsonArray) parallelForResult.get(dataOuts.getSource());
            } else {
                resultArray = new JsonArray();
            }

            /* Add the new output to the result collection */
            resultArray.add(new Gson().toJsonTree(input.get(dataOuts.getSource())));
            parallelForResult.put(dataOuts.getSource(), resultArray);
        } else {
            parallelForResult.put(dataOuts.getSource(), input.get(dataOuts.getSource()));
        }
    }



    /**
     * /Returns the result.
     *
     * @return result.
     */
    @Override
    public Map<String, Object> getResult() {
        return parallelForResult;
    }

    /**
     * Sets the number of children. These number is needed for the synchronization.
     *
     * @param number of children
     */
    public void setNumberOfParents(int number) {
        this.numberOfParents = number;
    }

    /**
     * Stops the cloning mechanism at this node because it's the end of the
     * parallelFor branch that was cloned.
     *
     * @param endNode end node.
     *
     * @return cloned node.
     * @throws CloneNotSupportedException on failure.
     */
    @Override
    public Node clone(Node endNode) throws CloneNotSupportedException {
        if (endNode == this) {
            return this;
        }
        return super.clone(endNode);
    }

    /** Getter and Setter */

    public Map<String, Object> getParallelForResult() {
        return getResult();
    }

    public void setParallelForResult(Map<String, Object> parallelForResult) {
        this.parallelForResult = parallelForResult;
    }

    public List<DataOuts> getOutput() {
        return output;
    }

    public void setOutput(List<DataOuts> output) {
        this.output = output;
    }

}
