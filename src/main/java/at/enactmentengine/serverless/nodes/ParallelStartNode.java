package at.enactmentengine.serverless.nodes;

import at.enactmentengine.serverless.exception.MissingInputDataException;
import at.uibk.dps.afcl.functions.objects.DataIns;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Control node which manages the tasks at the start of a parallel loop.
 *
 * @author markusmoosbrugger, jakobnoeckl
 * adapted by @author stefanpedratscher
 */
public class ParallelStartNode extends Node {

    /**
     * Logger for the parallel-start node.
     */
    static final Logger logger = LoggerFactory.getLogger(ParallelStartNode.class);
    /**
     * The maximum number of threads running in parallel
     */
    private static final int MAX_NUMBER_THREADS = 1000;
    /**
     * The input defined within the workflow file.
     */
    private List<DataIns> definedInput;

    /**
     * Default constructor for the parallel-start node.
     *
     * @param name of the parallel-start node.
     * @param type of the parallel-start node.
     * @param definedInput input defined in the workflow file.
     */
    public ParallelStartNode(String name, String type, List<DataIns> definedInput) {
        super(name, type);
        this.definedInput = definedInput;
    }

    /**
     * Checks the dataValues and creates a thread pool for the execution of the
     * children.
     */
    @Override
    public Boolean call() throws Exception {

        final Map<String, Object> outValues = new HashMap<>();

        /* Check if there is an input defined */
        if (definedInput != null) {

            /* Iterate over the possible inputs and look for defined ones */
            for (DataIns data : definedInput) {
                if (!dataValues.containsKey(data.getSource())) {
                    throw new MissingInputDataException(ParallelStartNode.class.getCanonicalName() + ": " + name
                            + " needs " + data.getSource() + "!");
                } else {
                    outValues.put(name + "/" + data.getName(), dataValues.get(data.getSource()));
                }
            }
        }

        logger.info("Executing {} ParallelStartNodeOld", name);

        /* Create an executor service to manage parallel running threads */
        ExecutorService exec = Executors
                .newFixedThreadPool(children.size() > MAX_NUMBER_THREADS ? MAX_NUMBER_THREADS : children.size());

        /* Pass data to all children and execute them */
        List<Future<Boolean>> futures = new ArrayList<>();
        for (Node node : children) {
            node.passResult(outValues);
            if (getLoopCounter() != -1) {
                node.setLoopCounter(loopCounter);
                node.setMaxLoopCounter(maxLoopCounter);
            }
            futures.add(exec.submit(node));
        }

        /* Wait for all children to finish */
        for (Future<Boolean> future : futures) {
            future.get();
        }
        exec.shutdown();

        return true;
    }

    /**
     *  Saves the passed result as dataValues.
     *
     * @param input to be passed
     */
    @Override
    public void passResult(Map<String, Object> input) {
        synchronized (this) {

            /* Check if the map containing the actual values is already defined */
            if (dataValues == null) {
                dataValues = new HashMap<>();
            }

            /* Check if there is an input defined */
            if (definedInput != null) {

                /* Iterate over the defined input and look for a match with the actual value */
                for (DataIns data : definedInput) {
                    if (input.containsKey(data.getSource())) {
                        dataValues.put(data.getSource(), input.get(data.getSource()));
                    }
                }
            }
        }
    }

    /**
     * Return the result of the parallel-start node.
     *
     * @return null because a start node does not return anything.
     */
    @Override
    public Map<String, Object> getResult() {
        return null;
    }

    /**
     * Clones this node and its children.
     *
     * @param endNode end node.
     *
     * @return cloned node.
     * @throws CloneNotSupportedException on failure.
     */
    @Override
    public Node clone(Node endNode) throws CloneNotSupportedException {
        Node node = (Node) super.clone();

        /* Find children nodes */
        node.children = new ArrayList<>();
        for (int i = 0; i < children.size(); i++) {
            Node currNode = children.get(i).clone(endNode);
            node.children.add(currNode);
            if (i == 0) {
                endNode = findParallelEndNode(currNode, 0);
            }
        }

        return node;
    }

    /**
     * Finds the matching parallel-end node recursively.
     *
     * @param currentNode the start node to search for.
     * @param depth of the parallel nesting
     *
     * @return found parallel-end node.
     */
    private Node findParallelEndNode(Node currentNode, int depth) {

        /* Iterate over all children */
        for (Node child : currentNode.getChildren()) {

            /* Check if end node is found */
            if (child instanceof ParallelEndNode) {

                /* Check if dept is correct */
                if (depth == 0) {
                    return child;
                } else {
                    return findParallelEndNode(child, depth - 1);
                }
            } else if (child instanceof ParallelStartNode) {

                /* Another nested parallel node detected */
                return findParallelEndNode(child, depth + 1);
            } else {

                /* Another (compound or base) function detected */
                return findParallelEndNode(child, depth);
            }
        }
        return null;
    }
}
