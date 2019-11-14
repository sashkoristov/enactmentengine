package at.enactmentengine.serverless.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import at.enactmentengine.serverless.nodes.ListPair;
import at.enactmentengine.serverless.nodes.Node;
import at.enactmentengine.serverless.nodes.SwitchEndNode;
import at.enactmentengine.serverless.nodes.SwitchStartNode;

/**
 * Model class for a switch workflow element.
 * 
 * @author markusmoosbrugger, jakobnoeckl
 *
 */
public class WorkflowElementSwitch extends WorkflowElement {

	private String name;
	private List<Data> dataIns;
	private Data dataEval;
	private List<Case> cases;
	private List<Data> dataOuts;
	@JsonProperty("default")
	private List<Object> workflowElementDefault;
	private List<WorkflowElement> workflowElementDefaultParsed;

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

	public Data getDataEval() {
		return dataEval;
	}

	public void setDataEval(Data dataEval) {
		this.dataEval = dataEval;
	}

	public List<Case> getCases() {
		return cases;
	}

	public void setCases(List<Case> cases) {
		this.cases = cases;
	}

	public List<Data> getDataOuts() {
		return dataOuts;
	}

	public void setDataOuts(List<Data> dataOuts) {
		this.dataOuts = dataOuts;
	}

	/**
	 * Creates a ListPair which includes a SwitchStartNode and a SwitchEndNode. This
	 * nodes are used for management purposes. For each switch case and for the
	 * default case an own branch is created and the elements inside are linked
	 * together. Every case is put between start- and endnode of the switch element.
	 */
	@Override
	public ListPair<Node, Node> toNodeList() {
		SwitchStartNode start = new SwitchStartNode(name, dataIns, dataEval, cases);
		SwitchEndNode end = new SwitchEndNode(name, dataOuts);
		// switch cases
		for (Case switchCase : cases) {
			ListPair<Node, Node> switchPair = new ListPair<Node, Node>();
			ListPair<Node, Node> startNode = switchCase.getParsedFunctions().get(0).toNodeList();
			switchPair.setStart(startNode.getStart());
			Node currentEnd = startNode.getEnd();
			for (int i = 1; i < switchCase.getParsedFunctions().size(); i++) {
				ListPair<Node, Node> currentListPair = switchCase.getParsedFunctions().get(i).toNodeList();
				currentEnd.addChild(currentListPair.getStart());
				currentListPair.getStart().addParent(currentEnd);
				currentEnd = currentListPair.getEnd();

			}
			switchPair.setEnd(currentEnd);
			start.addChild(switchPair.getStart());
			currentEnd.addChild(end);
			end.addParent(switchPair.getEnd());

		}
		// default case
		if (workflowElementDefaultParsed != null) {
			ListPair<Node, Node> switchPair = new ListPair<Node, Node>();
			ListPair<Node, Node> startNode = workflowElementDefaultParsed.get(0).toNodeList();
			switchPair.setStart(startNode.getStart());
			Node currentEnd = startNode.getEnd();
			for (int i = 1; i < workflowElementDefaultParsed.size(); i++) {
				ListPair<Node, Node> currentListPair = workflowElementDefaultParsed.get(i).toNodeList();
				currentEnd.addChild(currentListPair.getStart());
				currentListPair.getStart().addParent(currentEnd);
				currentEnd = currentListPair.getEnd();

			}
			switchPair.setEnd(currentEnd);
			start.addChild(switchPair.getStart());
			currentEnd.addChild(end);
			end.addParent(switchPair.getEnd());
		}

		return new ListPair<Node, Node>(start, end);
	}

	public List<Object> getWorkflowElementDefault() {
		return workflowElementDefault;
	}

	public void setWorkflowElementDefault(List<Object> workflowElementDefault) {
		this.workflowElementDefault = workflowElementDefault;
	}

	public List<WorkflowElement> getWorkflowElementDefaultParsed() {
		return workflowElementDefaultParsed;
	}

	public void setWorkflowElementDefaultParsed(List<WorkflowElement> list) {
		this.workflowElementDefaultParsed = list;
	}

}