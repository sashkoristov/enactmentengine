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
     * The id of the iteration in parallelFor
     */
    private int id = 0;

    /**
     * The number of execution in a parallelFor loop.
     */
    protected int loopCounter = -1;

    /**
     * The end of a parallelFor loop.
     */
    protected int maxLoopCounter = -1;

    /**
     * The concurrency limit of a parallelFor loop.
     */
    protected int concurrencyLimit = -1;

    /**
     * The starting time for a function within a parallelFor used in simulation.
     */
    protected long startTime = 0;

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
        parents = new ArrayList<>();
        children = new ArrayList<>();
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

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getLoopCounter() {
        return loopCounter;
    }

    public void setLoopCounter(int loopCounter) {
        this.loopCounter = loopCounter;
    }

    public int getMaxLoopCounter() {return maxLoopCounter;}

    public void setMaxLoopCounter(int maxLoopCounter) {this.maxLoopCounter = maxLoopCounter;}

    public int getConcurrencyLimit() {
        return concurrencyLimit;
    }

    public void setConcurrencyLimit(int concurrencyLimit) {
        this.concurrencyLimit = concurrencyLimit;
    }

    public long getStartTime() {
        return startTime;
    }

    public synchronized void setStartTime(long startTime) {
        if (this instanceof ParallelForEndNode) {
            ((ParallelForEndNode) this).addAllFinishTimes(startTime);
        }
        if (this.startTime == 0) {
            this.startTime = startTime;
        } else if (startTime > this.startTime) {
            this.startTime = startTime;
        }
    }

    /**
     * Clone the whole node.
     *
     * @param endNode end node.
     *
     * @return cloned node.
     *
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
