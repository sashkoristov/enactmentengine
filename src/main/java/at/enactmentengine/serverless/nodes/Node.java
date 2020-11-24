package at.enactmentengine.serverless.nodes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Abstract class which defines basic properties and functions for all nodes.
 *
 * @author markusmoosbrugger, jakobnoeckl
 * adapted by @author stefanpedratscher
 */
public abstract class Node implements Callable<Boolean>, Cloneable {

    /**
     * All parent nodes of the current node.
     */
    protected List<Node> parents;

    /**
     * All child nodes of the current node.
     */
    protected List<Node> children;

    /**
     * The name of the node.
     */
    protected String name;

    /**
     * The type of the node
     */
    protected String type;

    /**
     * The input values of the node.
     */
    protected Map<String, Object> dataValues;

    /**
     * Default constructor for a node.
     *
     * @param name of the node.
     * @param type of the node.
     */
    public Node(String name, String type) {
        super();
        this.name = name;
        this.type = type;
        this.parents = new ArrayList<>();
        this.children = new ArrayList<>();
    }

    /**
     * Pass results to the next node(s).
     *
     * @param map data which should be passed.
     */
    public abstract void passResult(Map<String, Object> map);

    /**
     * Add another child to the children's node list.
     *
     * @param node child which should be added.
     */
    public void addChild(Node node) {
        children.add(node);
    }

    /**
     * Add another parent to the parents's node list.
     *
     * @param node parent which should be added.
     */
    public void addParent(Node node) {
        parents.add(node);
    }

    /** Getter and Setter */

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

    public Map<String, Object> getDataValues() {
        return dataValues;
    }

    public void setDataValues(Map<String, Object> dataValues) {
        this.dataValues = dataValues;
    }

    /**
     * Clone the whole node.
     *
     * @param endNode end node.
     * @return cloned node.
     * @throws CloneNotSupportedException on failure.
     */
    public Node clone(Node endNode) throws CloneNotSupportedException {
        Node node = (Node) super.clone();
        node.children = new ArrayList<>();
        for (Node childrenNode : children) {
            node.children.add(childrenNode.clone(endNode));

        }
        return node;
    }
}
