package at.enactmentengine.serverless.nodes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.enactmentengine.serverless.exception.MissingInputDataException;
import at.enactmentengine.serverless.model.Data;

/**
 * Control node which manages the tasks at the start of a parallel loop.
 * 
 * @author markusmoosbrugger, jakobnoeckl
 *
 */
public class ParallelStartNodeOld extends Node {
	private List<Data> definedInput;
	final static Logger logger = LoggerFactory.getLogger(ParallelStartNodeOld.class);
	private static int MAX_NUMBER_THREADS = 50;

	public ParallelStartNodeOld(String name, String type, List<Data> definedInput) {
		super(name, type);
		this.definedInput = definedInput;
	}

	/**
	 * Checks the dataValues and creates a thread pool for the execution of the
	 * children.
	 */
	@Override
	public Boolean call() throws Exception {
		final Map<String, Object> outVals = new HashMap<>();
		for (Data data : definedInput) {
			if (!dataValues.containsKey(data.getSource())) {
				throw new MissingInputDataException(ParallelForStartNodeOld.class.getCanonicalName() + ": " + name
						+ " needs " + data.getSource() + "!");
			} else {
				outVals.put(name + "/" + data.getName(), dataValues.get(data.getSource()));
			}
		}

		logger.info("Executing " + name + " ParallelStartNodeOld");
		ExecutorService exec = Executors
				.newFixedThreadPool(children.size() > MAX_NUMBER_THREADS ? MAX_NUMBER_THREADS : children.size());
		List<Future<Boolean>> futures = new ArrayList<>();
		for (int i = 0; i < children.size(); i++) {
			Node node = children.get(i);
			node.passResult(outVals);
			futures.add(exec.submit(node));

		}
		for (Future<Boolean> future : futures) {
			future.get();
		}
		exec.shutdown();
		return true;
	}

	/**
	 * Saves the passed result as dataValues.
	 */
	@Override
	public void passResult(Map<String, Object> input) {
		synchronized (this) {
			if (dataValues == null) {
				dataValues = new HashMap<String, Object>();
			}
			for (Data data : definedInput) {
				if (input.containsKey(data.getSource())) {
					dataValues.put(data.getSource(), input.get(data.getSource()));
				}
			}
		}
	}

	@Override
	public Map<String, Object> getResult() {
		return null;
	}

	/**
	 * Clones this node and its children.
	 */
	public Node clone(Node endnode) throws CloneNotSupportedException {
		Node node = (Node) super.clone();
		node.children = new ArrayList<>();

		for (int i = 0; i < children.size(); i++) {
			Node currNode = (Node) children.get(i).clone(endnode);
			node.children.add(currNode);
			if (i == 0) {
				endnode = findParallelEndNode(currNode, 0);
			}
		}

		return node;
	}

	/**
	 * Finds the matching parallel end node recursively.
	 */
	private Node findParallelEndNode(Node currentNode, int depth) {
		for (Node child : currentNode.getChildren()) {
			if (child instanceof ParallelEndNodeOld) {
				if (depth == 0) {
					return child;
				} else {
					return findParallelEndNode(child, depth - 1);
				}

			} else if (child instanceof ParallelStartNodeOld) {
				return findParallelEndNode(child, depth + 1);
			} else {
				return findParallelEndNode(child, depth);
			}
		}
		return null;
	}
}
