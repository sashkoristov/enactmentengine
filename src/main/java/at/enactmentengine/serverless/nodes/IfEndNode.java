package at.enactmentengine.serverless.nodes;

import at.uibk.dps.afcl.functions.objects.DataOuts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Control node which manages the tasks at the end of a if element.
 *
 * @author markusmoosbrugger, jakobnoeckl
 * adapted by @author stefanpedratscher
 */
public class IfEndNode extends Node {

    /**
     * Logger for the an if-end node.
     */
    static final Logger logger = LoggerFactory.getLogger(IfEndNode.class);

    /**
     * The output of the if node defined in the workflow file.
     */
    private List<DataOuts> dataOuts;

    /**
     * The actual output of the if node.
     */
    private Map<String, Object> ifResult;

    /**
     * Constructor for an if-end control node.
     *
     * @param name of the if node.
     * @param dataOuts of the if node.
     */
    public IfEndNode(String name, List<DataOuts> dataOuts) {
        super(name, null);
        this.dataOuts = dataOuts;
    }

    /**
     * Passes the results to the children if one parent has finished. No
     * synchronization needed because always just one parent (if or else branch) is
     * executed.
     *
     * @return boolean representing success of the node execution.
     * @throws Exception on failure.
     */
    @Override
    public Boolean call() throws Exception {

        /* Define the output values of the if-end construct */
        Map<String, Object> outputValues = new HashMap<>();

        /* Check if any data output is specified in the workflow file */
        if (dataOuts != null) {

            /* Iterate over all data outputs specified in the workflow file */
            for (DataOuts data : dataOuts) {

                /* Find the corresponding actual output of the if node */
                for (Entry<String, Object> inputElement : this.ifResult.entrySet()) {
                    outputValues.put(name + "/" + data.getName(), inputElement.getValue());
                }
            }
        }

        logger.info("Executing {} IfEndNode with output: {}", name, outputValues);

        /* Pass the output to all child nodes */
        for (Node node : children) {
            node.passResult(outputValues);
            node.call();
        }

        return true;
    }

    /**
     * Sets the result for the if element.
     *
     * @param input to the child functions.
     */
    @Override
    public void passResult(Map<String, Object> input) {
        synchronized (this) {
            /* Check if there is an input specified */
            if (ifResult == null) {
                ifResult = new HashMap<>();
            }

            /* Check if there is an output specified in the workflow file */
            if (dataOuts != null) {

                /* Iterate over all outputs and search the according input */
                for (DataOuts data : dataOuts) {
                    for (Entry<String, Object> inputElement : input.entrySet()) {
                        if (data.getSource().contains(inputElement.getKey())) {
                            this.ifResult.put(inputElement.getKey(), input.get(inputElement.getKey()));
                        }
                    }
                }
            }
        }
    }

    /**
     * Get the result of the if-end construct.
     *
     * @return the if-end result.
     */
    @Override
    public Map<String, Object> getResult() {
        return ifResult;
    }
}
