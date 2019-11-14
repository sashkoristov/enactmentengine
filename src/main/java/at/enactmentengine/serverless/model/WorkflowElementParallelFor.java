package at.enactmentengine.serverless.model;

import java.util.List;

import at.enactmentengine.serverless.nodes.ListPair;
import at.enactmentengine.serverless.nodes.Node;
import at.enactmentengine.serverless.nodes.ParallelForEndNode;
import at.enactmentengine.serverless.nodes.ParallelForStartNode;

/**
 * Model class for a parallel for workflow element.
 * 
 * @author markusmoosbrugger, jakobnoeckl
 *
 */
public class WorkflowElementParallelFor extends WorkflowElement {

	private String name;
	private List<Data> dataIns;
	private LoopCounter loopCounter;
	private List<Object> loopBody;
	private List<WorkflowElement> loopBodyParsed;
	private List<Data> dataOuts;

	public String getName() {
		return name;
	}

	public List<WorkflowElement> getLoopBodyParsed() {
		return loopBodyParsed;
	}

	public void setLoopBodyParsed(List<WorkflowElement> loopBodyParsed) {
		this.loopBodyParsed = loopBodyParsed;

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

	public LoopCounter getLoopCounter() {
		return loopCounter;
	}

	public void setLoopCounter(LoopCounter loopCounter) {
		this.loopCounter = loopCounter;
	}

	public List<Object> getLoopBody() {
		return loopBody;
	}

	public void setLoopBody(List<Object> loopBody) {
		this.loopBody = loopBody;
	}

	public List<Data> getDataOuts() {
		return dataOuts;
	}

	public void setDataOuts(List<Data> dataOuts) {
		this.dataOuts = dataOuts;
	}

	/**
	 * Creates a ListPair which includes a ParallelForStartNode and a
	 * ParallelForEndNode. This nodes are used for management purposes. The nodes
	 * between start and end are linked together. Because the number of for parallel
	 * branches is not known at this time it creates one branch which is copied
	 * later during execution.
	 */
	public ListPair<Node, Node> toNodeList() {
		ParallelForStartNode parallelForStartNode = new ParallelForStartNode(name, "type", dataIns, loopCounter);
		ParallelForEndNode parallelForEndNode = new ParallelForEndNode(name, "", dataOuts);

		ListPair<Node, Node> firstPair = loopBodyParsed.get(0).toNodeList();

		Node currentEnd = firstPair.getEnd();
		parallelForStartNode.addChild(firstPair.getStart());
		for (int j = 1; j < loopBodyParsed.size(); j++) {
			ListPair<Node, Node> current = loopBodyParsed.get(j).toNodeList();
			currentEnd.addChild(current.getStart());
			current.getStart().addChild(currentEnd);
			currentEnd = current.getEnd();
		}
		currentEnd.addChild(parallelForEndNode);
		parallelForEndNode.addParent(currentEnd);

		return new ListPair<Node, Node>(parallelForStartNode, parallelForEndNode);
	}

}
