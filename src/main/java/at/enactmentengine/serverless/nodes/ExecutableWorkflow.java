package at.enactmentengine.serverless.nodes;

import at.enactmentengine.serverless.exception.MissingInputDataException;
import com.dps.afcl.functions.objects.DataIns;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Class which handles the start of the execution of the workflow.
 *
 * @author markusmoosbrugger, jakobnoeckl
 * extended by @author stefanpedratscher
 */
public class ExecutableWorkflow {

    final static Logger logger = LoggerFactory.getLogger(ExecutableWorkflow.class);

    private Node startNode;
    private Node endNode;
    private String workflowName;
    private List<DataIns> definedInput;
    private ExecutorService exec;

    /**
     * Default constructor to create an executable workflow
     *
     * @param workflowName name of the workflow
     * @param workflow list pair of workflow elements
     * @param definedInput inputs
     */
    public ExecutableWorkflow(String workflowName, ListPair<Node, Node> workflow, List<DataIns> definedInput) {
        this.startNode = workflow.getStart();
        this.endNode = workflow.getEnd();
        this.workflowName = workflowName;
        this.definedInput = definedInput;
        this.exec = Executors.newSingleThreadExecutor();
    }

    /**
     * Starts the execution of the workflow.
     *
     * @param inputs The input values for the first workflow element.
     * @throws MissingInputDataException
     */
    public Map<String, Object> executeWorkflow(Map<String, Object> inputs) throws MissingInputDataException {

        final Map<String, Object> outVals = new HashMap<>();
        for (DataIns data : definedInput) {
            if (!inputs.containsKey(data.getSource())) {
                throw new MissingInputDataException(workflowName + " needs more input data: " + data.getSource());
            } else {
                outVals.put(workflowName + "/" + data.getName(), inputs.get(data.getSource()));
            }
        }

        // Start workflow execution
        logger.info("Starting execution of workflow: \"" + workflowName + "\"" + "  [at " + System.currentTimeMillis() + "ms]");
        startNode.passResult(outVals);
        Future<Boolean> future = exec.submit(startNode);
        try {
            if (future.get()) {
                logger.info("Workflow completed: " + endNode.getResult());
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

        exec.shutdown();

        return endNode.getResult();
    }

}
