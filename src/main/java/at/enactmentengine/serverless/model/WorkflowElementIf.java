package at.enactmentengine.serverless.model;

import java.util.List;

import at.enactmentengine.serverless.nodes.IfEndNodeOld;
import com.fasterxml.jackson.annotation.JsonProperty;

import at.enactmentengine.serverless.nodes.IfStartNodeOld;
import at.enactmentengine.serverless.nodes.ListPair;
import at.enactmentengine.serverless.nodes.Node;

/**
 * Model class for an if workflow element.
 * 
 * @author markusmoosbrugger, jakobnoeckl
 *
 */
public class WorkflowElementIf extends WorkflowElement {

	private String name;
	private List<Data> dataIns;
	private Condition condition;
	private List<FunctionWrapper> then;
	@JsonProperty("else")
	private List<FunctionWrapper> elseProperty;
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

	public Condition getCondition() {
		return condition;
	}

	public void setCondition(Condition condition) {
		this.condition = condition;
	}

	public List<FunctionWrapper> getThen() {
		return then;
	}

	public void setThen(List<FunctionWrapper> then) {
		this.then = then;
	}

	public List<Data> getDataOuts() {
		return dataOuts;
	}

	public void setDataOuts(List<Data> dataOuts) {
		this.dataOuts = dataOuts;
	}

	/**
	 * Creates a ListPair which includes an IfStartNodeOld and an IfEndNodeOld. This nodes
	 * are used for management purposes. For the "then" and "else" branch the
	 * elements are linked together and both branches are put between start- and
	 * endnode of the if element.
	 */

	@Override
	public ListPair<Node, Node> toNodeList() {
		IfStartNodeOld start = new IfStartNodeOld(name, dataIns, condition);
		IfEndNodeOld end = new IfEndNodeOld(name, dataOuts);

		ListPair<Node, Node> thenPair = new ListPair<Node, Node>();
		ListPair<Node, Node> startNode = then.get(0).getFunction().toNodeList();
		thenPair.setStart(startNode.getStart());
		Node currentEnd = startNode.getEnd();
		for (int i = 1; i < then.size(); i++) {
			ListPair<Node, Node> current = then.get(i).getFunction().toNodeList();
			currentEnd.addChild(current.getStart());
			current.getStart().addParent(currentEnd);
			currentEnd = current.getEnd();
		}
		thenPair.setEnd(currentEnd);
		start.addChild(thenPair.getStart());
		currentEnd.addChild(end);
		end.addParent(thenPair.getEnd());

		ListPair<Node, Node> elsePair = new ListPair<Node, Node>();
		startNode = elseProperty.get(0).getFunction().toNodeList();
		elsePair.setStart(startNode.getStart());
		currentEnd = startNode.getEnd();
		for (int i = 1; i < elseProperty.size(); i++) {
			ListPair<Node, Node> current = elseProperty.get(i).getFunction().toNodeList();
			currentEnd.addChild(current.getStart());
			current.getStart().addParent(currentEnd);
			currentEnd = current.getEnd();
		}
		elsePair.setEnd(currentEnd);
		start.addChild(elsePair.getStart());
		currentEnd.addChild(end);
		end.addParent(elsePair.getEnd());

		return new ListPair<Node, Node>(start, end);
	}

}
