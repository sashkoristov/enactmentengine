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
 */
public class ParallelForEndNode extends Node {
    static final Logger logger = LoggerFactory.getLogger(ParallelForEndNode.class);
    private int waitCounter = 0;
    private List<DataOuts> output;
    private Map<String, Object> parallelResult = new HashMap<>();
    private int numberOfChildren;

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
        synchronized (this) {
            if (++waitCounter != numberOfChildren) {
                return false;
            }
        }

        Map<String, Object> outputValues = new HashMap<>();
        if (output != null) {
            for (DataOuts data : output) {
                String key = name + "/" + data.getName();
                if (parallelResult.containsKey(data.getSource())) {
                    outputValues.put(key, parallelResult.get(data.getSource()));
                } else if ("collection".equals(data.getType())) {
                    outputValues.put(key, parallelResult);
                }
            }
        }

        logger.info("Executing {} ParallelForEndNodeOld with output: {}", name, outputValues);

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
            if (output != null) {
                for (DataOuts data : output) {
                    if (input.containsKey(data.getSource())) {
                        handlePassResults(data, input);
                    }
                }
            }
        }
    }

    private void handlePassResults(DataOuts data, Map<String, Object> input) {
        if ("collection".equals(data.getType())) {
            if (parallelResult.containsKey(data.getSource())) {
                JsonArray resultArray = (JsonArray) parallelResult.get(data.getSource());
                resultArray.add(new Gson().toJsonTree(input.get(data.getSource())));
                parallelResult.put(data.getSource(), resultArray);
            } else {
                JsonArray resultArray = new JsonArray();
                resultArray.add(new Gson().toJsonTree(input.get(data.getSource())));
                parallelResult.put(data.getSource(), resultArray);
            }
        } else {
            parallelResult.put(data.getSource(), input.get(data.getSource()));
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
     * Sets the number of children. These number is needed for the synchronization.
     *
     * @param number of children
     */
    public void setNumberOfChildren(int number) {
        this.numberOfChildren = number;
    }

    /**
     * Stops the cloning mechanism at this node because it's the end of the
     * parallelFor branch that was cloned.
     */
    @Override
    public Node clone(Node endNode) throws CloneNotSupportedException {
        if (endNode == this) {
            return this;
        }

        return super.clone(endNode);
    }

    public Map<String, Object> getParallelResult() {
        return getResult();
    }

    public void setParallelResult(Map<String, Object> parallelResult) {
        this.parallelResult = parallelResult;
    }

    public List<DataOuts> getOutput() {
        return output;
    }

    public void setOutput(List<DataOuts> output) {
        this.output = output;
    }

}
