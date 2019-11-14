package at.enactmentengine.serverless.model;

import java.util.ArrayList;
import java.util.List;

import at.enactmentengine.serverless.nodes.ExecutableWorkflow;
import at.enactmentengine.serverless.nodes.ListPair;
import at.enactmentengine.serverless.nodes.Node;

/**
 * Model class for a non executable workflow.
 * 
 * @author markusmoosbrugger, jakobnoeckl
 *
 */
public class Workflow {
	private String workflow;
	private String name;
	private List<Data> dataIns;
	private List<Object> workflowBody;
	private List<Data> dataOuts;
	private List<WorkflowElement> workflowBodyParsed;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<Data> getDataIns() {
		return dataIns;
	}

	public void setDataIns(List<Data> dataIns) {
		this.dataIns = dataIns;
	}

	public List<Data> getDataOuts() {
		return dataOuts;
	}

	public void setDataOuts(List<Data> dataOuts) {
		this.dataOuts = dataOuts;
	}

	public String getWorkflow() {
		return workflow;
	}

	public List<Object> getWorkflowBody() {
		return workflowBody;
	}

	public void setWorkflowBody(List<Object> workflowBody) {
		this.workflowBody = workflowBody;
	}

	public void setWorkflow(String workflow) {
		this.workflow = workflow;
	}

	public List<WorkflowElement> getWorkflowBodyParsed() {
		if (workflowBodyParsed == null) {
			workflowBodyParsed = new ArrayList<WorkflowElement>();
		}
		return workflowBodyParsed;
	}

	public void setWorkflowBodyParsed(List<WorkflowElement> workflowBodyParsed) {
		this.workflowBodyParsed = workflowBodyParsed;
	}

	/**
	 * Converts the non executable workflow to an executable workflow. Goes through
	 * the whole workflow and converts every node to a list pair. These list pairs
	 * are then linked together in the correct order.
	 * 
	 * @return an executable workflow.
	 */
	public ExecutableWorkflow toExecutableWorkflow() {
		ListPair<Node, Node> workflowPair = new ListPair<Node, Node>();
		ListPair<Node, Node> startNode = workflowBodyParsed.get(0).toNodeList();
		workflowPair.setStart(startNode.getStart());
		Node currentEnd = startNode.getEnd();

		for (int i = 1; i < workflowBodyParsed.size(); i++) {
			ListPair<Node, Node> current = workflowBodyParsed.get(i).toNodeList();
			currentEnd.addChild(current.getStart());
			current.getStart().addParent(currentEnd);
			currentEnd = current.getEnd();
		}
		workflowPair.setEnd(currentEnd);

		return new ExecutableWorkflow(name, workflowPair, dataIns);
	};
}
