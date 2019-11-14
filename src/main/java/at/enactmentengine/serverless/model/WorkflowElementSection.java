package at.enactmentengine.serverless.model;

import java.util.List;

import at.enactmentengine.serverless.nodes.ListPair;
import at.enactmentengine.serverless.nodes.Node;

/**
 * Model class for section.
 * 
 * @author markusmoosbrugger, jakobnoeckl
 *
 */
public class WorkflowElementSection extends WorkflowElement {
	private List<Object> section;
	private List<WorkflowElement> sectionParsed;

	public List<Object> getSection() {
		return section;
	}

	public void setSection(List<Object> section) {
		this.section = section;
	}

	/**
	 * Creates a ListPair which includes the first element of the section and the
	 * last element of the section. The nodes between these elements are also linked
	 * together.
	 *
	 */

	public ListPair<Node, Node> toNodeList() {
		ListPair<Node, Node> sectionPair = new ListPair<Node, Node>();
		ListPair<Node, Node> startNode = sectionParsed.get(0).toNodeList();
		sectionPair.setStart(startNode.getStart());
		Node currentEnd = startNode.getEnd();
		for (int i = 1; i < sectionParsed.size(); i++) {
			ListPair<Node, Node> current = sectionParsed.get(i).toNodeList();
			currentEnd.addChild(current.getStart());
			current.getStart().addParent(currentEnd);
			currentEnd = current.getEnd();
		}
		sectionPair.setEnd(currentEnd);
		return sectionPair;
	}

	public void setSectionParsed(List<WorkflowElement> sectionParsed) {
		this.sectionParsed = sectionParsed;

	}

	@Override
	public String getName() {
		return null;
	}

}
