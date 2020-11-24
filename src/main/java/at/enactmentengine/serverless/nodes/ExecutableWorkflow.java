package at.enactmentengine.serverless.nodes;

import at.enactmentengine.serverless.exception.MissingInputDataException;
import at.uibk.dps.afcl.functions.objects.DataIns;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
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

    /**
     * Logger for executable workflow.
     */
    private static final Logger logger = LoggerFactory.getLogger(ExecutableWorkflow.class);

    /**
     * Start node of the executable workflow.
     */
    private Node startNode;

    /**
     * End node of the executable workflow.
     */
    private Node endNode;

    /**
     * The name of the workflow.
     */
    private String workflowName;

    /**
     * The expected workflow input (written in the .yaml file).
     */
    private List<DataIns> definedInput;

    /**
     * Executor service to run the workflow.
     */
    private ExecutorService executorService;

    /**
     * Default constructor to create an executable workflow.
     *
     * @param workflowName name of the workflow.
     * @param workflow     node list pair of workflow elements.
     * @param definedInput expected workflow inputs.
     */
    public ExecutableWorkflow(String workflowName, ListPair<Node, Node> workflow, List<DataIns> definedInput) {
        this.startNode = workflow.getStart();
        this.endNode = workflow.getEnd();
        this.workflowName = workflowName;
        this.definedInput = definedInput;
        this.executorService = Executors.newSingleThreadExecutor();
    }

    /**
     * Starts the execution of the workflow.
     *
     * @param input values for the first workflow element (actual values).
     *
     * @return result of the workflow.
     *
     * @throws MissingInputDataException on missing input data.
     * @throws ExecutionException on execution failure.
     * @throws InterruptedException on interruption.
     */
    public Map<String, Object> executeWorkflow(Map<String, Object> input) throws MissingInputDataException, ExecutionException, InterruptedException {

        /* Create a variable to handle the present input */
        final Map<String, Object> presentInput = new HashMap<>();

        /* Iterate over all expected inputs */
        if( definedInput!= null ) {
            for (DataIns data : definedInput) {

                /* Check if the actual input contains the expected input */
                if (input != null && input.containsKey(data.getSource())) {

                    /* Add the actual input to the list of actually present inputs */
                    presentInput.put(workflowName + "/" + data.getName(), input.get(data.getSource()));
                } else {
                    /* The expected input is not present */
                    throw new MissingInputDataException(workflowName + " needs more input data: " + data.getSource());
                }
            }
        }

        /* Start workflow execution */
        logger.info("Starting execution of workflow: \"{}\" [at {}ms]", workflowName, System.currentTimeMillis());

        /* Pass the present inputs to the start node */
        startNode.passResult(presentInput);

        /* Run the start node */
        Future<Boolean> future = executorService.submit(startNode);

        try {

            /* Wait if needed for the node */
            if (Boolean.TRUE.equals(future.get())) {

                /* Check if the result is valid */
                if (endNode.getResult() != null) {
                    logger.info("Workflow completed: {}", endNode.getResult());
                } else {
                    logger.error("Workflow Failed! End result is Null");
                }
            }
        } catch (InterruptedException | ExecutionException e) {

            /* Cancel task and shut down executor on failure */
            future.cancel(true);
            executorService.shutdownNow();
            throw e;
        }

        /* Terminate executor */
        executorService.shutdown();

        /* Return result of the last node in the workflow (workflow result) */
        return endNode.getResult();
    }

    /**
     * Getter ans Setter
     */

    public Node getStartNode() {
        return startNode;
    }

    public void setStartNode(Node startNode) {
        this.startNode = startNode;
    }

    public Node getEndNode() {
        return endNode;
    }

    public void setEndNode(Node endNode) {
        this.endNode = endNode;
    }

    public String getWorkflowName() {
        return workflowName;
    }

    public void setWorkflowName(String workflowName) {
        this.workflowName = workflowName;
    }

    public List<DataIns> getDefinedInput() {
        return definedInput;
    }

    public void setDefinedInput(List<DataIns> definedInput) {
        this.definedInput = definedInput;
    }
}
