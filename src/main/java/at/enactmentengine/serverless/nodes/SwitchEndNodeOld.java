package at.enactmentengine.serverless.nodes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.enactmentengine.serverless.model.Data;

/**
 * Control node which manages the tasks at the end of a switch element.
 * 
 * @author markusmoosbrugger, jakobnoeckl
 *
 */
public class SwitchEndNodeOld extends Node {
	private List<Data> dataOuts;
	private Map<String, Object> switchResult = new HashMap<>();
	final static Logger logger = LoggerFactory.getLogger(SwitchEndNodeOld.class);

	public SwitchEndNodeOld(String name, List<Data> dataOuts) {
		super(name, "");
		this.dataOuts = dataOuts;
	}

	/**
	 * Passes the results to the children if one parent has finished. No
	 * synchronization needed because always just one switch case can be executed.
	 */
	@Override
	public Boolean call() throws Exception {

		logger.info("Executing " + name + " SwitchEndNodeOld");

		Map<String, Object> outputValues = new HashMap<>();

		for (Data data : dataOuts) {
			for (Entry<String, Object> inputElement : switchResult.entrySet()) {
				outputValues.put(name + "/" + data.getName(), inputElement.getValue());
			}

		}
		if (outputValues.size() == 0) {
			for (Data data : dataOuts) {
				if (data.getSource().contains("NULL")) {
					outputValues.put(name + "/" + data.getName(), "NULL");
				}

			}
		}
		for (Node node : children) {
			node.passResult(outputValues);
			node.call();
		}
		return true;
	}

	/**
	 * Sets the passed result for the switch element.
	 */
	@Override
	public void passResult(Map<String, Object> input) {
		synchronized (this) {
			for (Data data : dataOuts) {
				for (Entry<String, Object> inputElement : input.entrySet()) {
					if (data.getSource().contains(inputElement.getKey())) {
						switchResult.put(inputElement.getKey(), input.get(inputElement.getKey()));
					}
				}
			}
		}

	}

	/**
	 * Returns the result.
	 */
	@Override
	public Map<String, Object> getResult() {
		return switchResult;
	}

}
