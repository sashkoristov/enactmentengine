package at.enactmentengine.serverless.nodes;

import com.dps.afcl.functions.objects.DataOuts;
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
 */
public class IfEndNode extends Node {

    private List<DataOuts> dataOuts;
    private Map<String, Object> ifResult;
    final static Logger logger = LoggerFactory.getLogger(IfEndNode.class);

    public IfEndNode(String name, List<DataOuts> dataOuts) {
        super(name, "");
        this.dataOuts = dataOuts;
    }

    /**
     * Passes the results to the children if one parent has finished. No
     * synchronization needed because always just one parent (if or else branch) is
     * executed.
     */
    @Override
    public Boolean call() throws Exception {
        Map<String, Object> outputValues = new HashMap<>();
        if (dataOuts != null) {
            for (DataOuts data : dataOuts) {
                for (Entry<String, Object> inputElement : this.ifResult.entrySet()) {
                    outputValues.put(name + "/" + data.getName(), inputElement.getValue());
                }
            }
        }
        logger.info("Executing " + name + " IfEndNodeOld with output:" + outputValues.toString());
        for (Node node : children) {
            node.passResult(outputValues);
            node.call();
        }
        return true;

    }

    /**
     * Sets the result for the if element.
     */
    @Override
    public void passResult(Map<String, Object> input) {
        synchronized (this) {
            if (ifResult == null) {
                ifResult = new HashMap<String, Object>();
            }
            if (dataOuts != null) {
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

    @Override
    public Map<String, Object> getResult() {
        // TODO Auto-generated method stub
        return null;
    }

}
