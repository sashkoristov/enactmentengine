package at.enactmentengine.serverless.nodes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Abstract class which defines basic properties and functions for all nodes.
 * 
 * @author markusmoosbrugger, jakobnoeckl
 *
 */
public abstract class Node implements Callable<Boolean>, Cloneable {
	protected List<Node> parents = new ArrayList<>();
	protected List<Node> children = new ArrayList<>();
	protected String name;
	protected String type;
	protected Map<String, Object> dataValues;

	public Node(String name, String type) {
		super();
		this.name = name;
		this.type = type;
	}

	public abstract void passResult(Map<String, Object> map);

	public void addChild(Node node) {
		children.add(node);
	}

	public void addParent(Node node) {
		parents.add(node);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<Node> getChildren() {
		return children;
	}

	public abstract Map<String, Object> getResult();

	public Node clone(Node endNode) throws CloneNotSupportedException {
		Node node = (Node) super.clone();
		node.children = new ArrayList<>();
		for (Node childrenNode : children) {
			node.children.add((Node) childrenNode.clone(endNode));

		}
		return node;
	}
}
