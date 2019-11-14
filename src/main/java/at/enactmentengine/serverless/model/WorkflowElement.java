package at.enactmentengine.serverless.model;

import at.enactmentengine.serverless.nodes.ListPair;
import at.enactmentengine.serverless.nodes.Node;

/**
 * Abstract class for a workflow element.
 * 
 * @author markusmoosbrugger, jakobnoeckl
 *
 */

public abstract class WorkflowElement {

	public abstract ListPair<Node, Node> toNodeList();
	public abstract String getName();
}
