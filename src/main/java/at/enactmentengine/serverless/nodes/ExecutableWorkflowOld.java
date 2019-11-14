package at.enactmentengine.serverless.nodes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.enactmentengine.serverless.exception.MissingInputDataException;
import at.enactmentengine.serverless.model.Data;

/**
 * Class which handles the start of the execution of the workflow.
 * 
 * @author markusmoosbrugger, jakobnoeckl
 *
 */
public class ExecutableWorkflowOld {

	final static Logger logger = LoggerFactory.getLogger(ExecutableWorkflowOld.class);
	private Node startNode;
	private Node endNode;
	private String workflowName;
	private List<Data> definedInput;
	private ExecutorService exec;

	public ExecutableWorkflowOld(String workflowName, ListPair<Node, Node> workflow, List<Data> definedInput) {
		startNode = workflow.getStart();
		endNode = workflow.getEnd();
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
		for (Data data : definedInput) {
			if (!inputs.containsKey(data.getSource())) {
				throw new MissingInputDataException(workflowName + " needs more input data: " + data.getSource());
			} else {
				outVals.put(workflowName + "/" + data.getName(), inputs.get(data.getSource()));
			}
		}
		logger.info("Starting Execution of workflow " + workflowName + "!" + " ["+System.currentTimeMillis()+"ms]");
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
