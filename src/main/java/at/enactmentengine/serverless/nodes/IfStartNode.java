package at.enactmentengine.serverless.nodes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.enactmentengine.serverless.exception.MissingInputDataException;
import at.enactmentengine.serverless.model.Condition;
import at.enactmentengine.serverless.model.ConditionElement;
import at.enactmentengine.serverless.model.Data;

/**
 * Control node which manages the tasks at the start of a if element.
 * 
 * @author markusmoosbrugger, jakobnoeckl
 *
 */
public class IfStartNode extends Node {
	private Condition condition;
	private List<Data> dataIns;
	final static Logger logger = LoggerFactory.getLogger(IfStartNode.class);

	public IfStartNode(String name, List<Data> dataIns, Condition condition) {
		super(name, "");
		this.condition = condition;
		this.dataIns = dataIns;

	}

	/**
	 * Checks the dataValues and evaluates the condition. Depending on the
	 * evaluation either the if or else branch is executed.
	 */
	@Override
	public Boolean call() throws Exception {
		final Map<String, Object> ifInputValues = new HashMap<>();
		for (Data data : dataIns) {
			if (!dataValues.containsKey(data.getSource())) {
				throw new MissingInputDataException(
						IfStartNode.class.getCanonicalName() + ": " + name + " needs " + data.getSource() + "!");
			} else {
				ifInputValues.put(name + "/" + data.getName(), dataValues.get(data.getSource()));
			}
		}
		boolean evaluate = false;

		for (ConditionElement conditionElement : condition.getConditions()) {
			evaluate = evaluate(conditionElement, ifInputValues);
			// if combined with is "or" and one condition element is true the whole
			// condition evaluates to true
			if (condition.getCombinedWith().equals("or") && evaluate) {
				break;
			}
			// if combined with is "and" and one condition element is false the whole
			// condition evaluates to false
			if (condition.getCombinedWith().equals("and") && !evaluate) {
				break;
			}
		}

		Node node = null;
		if (evaluate) {
			node = children.get(0);
			logger.info("Executing " + name + " IfStartNode in if branch.");
		} else {
			node = children.get(1);
			logger.info("Executing " + name + " IfStartNode in else branch.");
		}

		node.passResult(ifInputValues);
		node.call();

		return true;
	}

	/**
	 * Evaluates a single condition element.
	 * 
	 * @param conditionElement The condition element.
	 * @param ifInputValues    The input values for the condition.
	 * @return true when the condition element is evaluated to true, otherwise
	 *         false.
	 * @throws MissingInputDataException
	 */
	private boolean evaluate(ConditionElement conditionElement, Map<String, Object> ifInputValues)
			throws MissingInputDataException {
		int data1 = parseCondition(conditionElement.getData1(), ifInputValues);
		int data2 = parseCondition(conditionElement.getData2(), ifInputValues);
		switch (conditionElement.getOperator()) {
		case "==":
			return data1 == data2;
		case "<":
			return data1 < data2;
		case "<=":
			return data1 <= data2;
		case ">":
			return data1 > data2;
		case ">=":
			return data1 >= data2;
		case "!=":
			return data1 != data2;
		default:
			logger.info("No condition match for operator " + conditionElement.getOperator());
		}
		return false;
	}

	/**
	 * Sets the passed result as dataValues.
	 */
	@Override
	public void passResult(Map<String, Object> input) {
		synchronized (this) {
			if (dataValues == null)
				dataValues = new HashMap<>();
			for (Data data : dataIns) {
				if (input.containsKey(data.getSource())) {
					dataValues.put(data.getSource(), input.get(data.getSource()));
				}
			}
		}
	}

	/**
	 * Tries to parse the given string as an integer. It this is not possible it has
	 * to be a variable name and the value for that variable has to be in the if
	 * input values.
	 * 
	 * @param string        The string which is parsed to an integer.
	 * @param ifInputValues A map that contains the needed input values.
	 * @return The parsed value as integer.
	 * @throws MissingInputDataException
	 */
	private int parseCondition(String string, Map<String, Object> ifInputValues) throws MissingInputDataException {
		String conditionName = null;
		int conditionData = 0;
		try {
			conditionData = Integer.valueOf(string);
		} catch (NumberFormatException e) {
			conditionName = string;
		}
		try {
			if (conditionName != null) {
				conditionData = ((Integer) ifInputValues.get(conditionName)).intValue();
			}
		} catch (Exception e) {
			throw new MissingInputDataException(
					IfStartNode.class.getCanonicalName() + ": " + name + " needs " + conditionName + "!");
		}

		return conditionData;

	}

	@Override
	public Map<String, Object> getResult() {
		return null;
	}

}
