package at.enactmentengine.serverless.model;

import java.util.List;

import at.enactmentengine.serverless.nodes.ListPair;
import at.enactmentengine.serverless.nodes.Node;
import at.enactmentengine.serverless.nodes.ParallelEndNodeOld;
import at.enactmentengine.serverless.nodes.ParallelStartNodeOld;

/**
 * Model class for a parallel workflow element.
 * 
 * @author markusmoosbrugger, jakobnoeckl
 *
 */
public class WorkflowElementParallel extends WorkflowElement {

	private String name;
	private List<Data> dataIns;
	private List<WorkflowElementSection> parallelBody;
	private List<Data> dataOuts;

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

	public List<WorkflowElementSection> getParallelBody() {
		return parallelBody;
	}

	public void setParallelBody(List<WorkflowElementSection> parallelBody) {
		this.parallelBody = parallelBody;
	}

	public List<Data> getDataOuts() {
		return dataOuts;
	}

	public void setDataOuts(List<Data> dataOuts) {
		this.dataOuts = dataOuts;
	}

	/**
	 * Creates a ListPair which includes a ParallelStartNodeOld and a ParallelEndNodeOld.
	 * This nodes are used for management purposes. The nodes between start and end
	 * are linked together.
	 */
	@Override
	public ListPair<Node, Node> toNodeList() {
		ParallelStartNodeOld start = new ParallelStartNodeOld(name, "test", dataIns);
		ParallelEndNodeOld end = new ParallelEndNodeOld(name, "test", dataOuts);

		for (int i = 0; i < parallelBody.size(); i++) {
			ListPair<Node, Node> currentListPair = parallelBody.get(i).toNodeList();
			currentListPair.getEnd().addChild(end);
			currentListPair.getStart().addParent(start);
			start.addChild(currentListPair.getStart());
			end.addParent(currentListPair.getEnd());
		}

		return new ListPair<Node, Node>(start, end);
	}
}
