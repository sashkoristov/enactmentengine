package at.enactmentengine.serverless.nodes;

import at.uibk.dps.afcl.functions.objects.DataOuts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Control node which manages the tasks at the end of a switch element.
 *
 * @author markusmoosbrugger, jakobnoeckl
 * adapted by @author stefanpedratscher
 */
public class SwitchEndNode extends Node {

    /**
     * Logger for a switch-end node.
     */
    static final Logger logger = LoggerFactory.getLogger(SwitchEndNode.class);

    /**
     * Output defined in the workflow file.
     */
    private List<DataOuts> dataOuts;

    /**
     * Actual result of the switch construct.
     */
    private Map<String, Object> switchResult = new HashMap<>();


    /**
     * Default constructor of a switch-end node.
     *
     * @param name of the switch-end node.
     * @param dataOuts output defined in the workflow file.
     */
    public SwitchEndNode(String name, List<DataOuts> dataOuts) {
        super(name, "");
        this.dataOuts = dataOuts;
    }

    /**
     * Passes the results to the children if one parent has finished. No
     * synchronization needed because always just one switch case can be executed.
     */
    @Override
    public Boolean call() throws Exception {

        logger.info("Executing {} SwitchEndNodeOld", name);
        Map<String, Object> outputValues = new HashMap<>();

        /* Check if there is an output defined */
        if(dataOuts != null){

            /* Iterate over the possible outputs and look for defined ones */
            for (DataOuts data : dataOuts) {
                for (Entry<String, Object> inputElement : switchResult.entrySet()) {
                    outputValues.put(name + "/" + data.getName(), inputElement.getValue());
                }
            }
        }

        if (outputValues.size() == 0 && dataOuts != null) {
            for (DataOuts data : dataOuts) {
                if (data.getSource().contains("NULL")) {
                    outputValues.put(name + "/" + data.getName(), "NULL");
                }

            }
        }
        for (Node node : children) {
            node.passResult(outputValues);
            node.call();
        }
        return true;
    }

    /**
     * Sets the passed result for the switch element.
     */
    @Override
    public void passResult(Map<String, Object> input) {
        synchronized (this) {
            if(dataOuts != null){
                for (DataOuts data : dataOuts) {
                    for (Entry<String, Object> inputElement : input.entrySet()) {
                        if (data.getSource().contains(inputElement.getKey())) {
                            switchResult.put(inputElement.getKey(), input.get(inputElement.getKey()));
                        }
                    }
                }
            }

        }

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

        /* Clone the node */
        SwitchEndNode node = (SwitchEndNode) super.clone();
        node.switchResult = new HashMap<>();

        return node;
    }

    /**
     * Returns the result.
     */
    @Override
    public Map<String, Object> getResult() {
        return switchResult;
    }

}
