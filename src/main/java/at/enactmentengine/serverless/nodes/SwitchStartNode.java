package at.enactmentengine.serverless.nodes;

import at.enactmentengine.serverless.exception.MissingInputDataException;
import at.enactmentengine.serverless.exception.NoSwitchCaseFulfilledException;
import at.uibk.dps.afcl.functions.objects.Case;
import at.uibk.dps.afcl.functions.objects.DataEval;
import at.uibk.dps.afcl.functions.objects.DataIns;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Control node which manages the tasks at the start of a switch element.
 *
 * @author markusmoosbrugger, jakobnoeckl
 */
public class SwitchStartNode extends Node {
    static final Logger logger = LoggerFactory.getLogger(SwitchStartNode.class);
    private List<DataIns> dataIns;
    private List<Case> cases;
    private DataEval dataEval;

    public SwitchStartNode(String name, List<DataIns> dataIns, DataEval dataEval, List<Case> cases) {
        super(name, "");
        this.dataIns = dataIns;
        this.dataEval = dataEval;
        this.cases = cases;
    }

    /**
     * Checks the dataValues and parses the switch condition. Depending on the input
     * values a different switch case is executed.
     */
    @Override
    public Boolean call() throws Exception {
        final Map<String, Object> switchInputValues = new HashMap<>();
        for (DataIns data : dataIns) {
            if (!dataValues.containsKey(data.getSource())) {
                throw new MissingInputDataException(
                        SwitchStartNode.class.getCanonicalName() + ": " + name + " needs " + data.getSource() + "!");
            } else {
                switchInputValues.put(name + "/" + data.getName(), dataValues.get(data.getSource()));
            }
        }
        if (!dataValues.containsKey(dataEval.getSource())) {
            throw new MissingInputDataException(
                    SwitchStartNode.class.getCanonicalName() + ": " + name + " needs " + dataEval.getSource() + "!");
        }

        logger.info("Executing {} SwitchStartNodeOld", name);

        Object switchValue = parseSwitchCondition();
        // goes through all cases and executes a case if the switch value matches this
        // case
        for (int i = 0; i < cases.size(); i++) {
            if (caseMatches(cases.get(i).getValue(), switchValue)) {
                logger.info("Switch case {} fulfilled with value {}", cases.get(i).getValue(), switchValue);
                children.get(i).passResult(switchInputValues);
                if (getLoopCounter() != -1) {
                    children.get(i).setLoopCounter(loopCounter);
                    children.get(i).setMaxLoopCounter(maxLoopCounter);
                }
                children.get(i).call();
                return true;
            } else if (children.size() > cases.size()) {
                logger.info("Switch default case is executed.");
                children.get(children.size() - 1).passResult(switchInputValues);
                if (getLoopCounter() != -1) {
                    children.get(children.size() - 1).setLoopCounter(loopCounter);
                    children.get(children.size() - 1).setMaxLoopCounter(maxLoopCounter);
                }
                children.get(children.size() - 1).call();
                return true;
            }
        }
        throw new NoSwitchCaseFulfilledException(
                "No matching switch case found for value " + switchValue + " in node " + name);
    }

    /**
     * Checks if the input value matches with the defined case. Integers and strings
     * are supported.
     *
     * @param object    The defined switch case value
     * @param switchVal The input value
     * @return true if switch case value matches the input value, otherwise false.
     */
    private boolean caseMatches(Object object, Object switchVal) {
        switch (dataEval.getType()) {
            case "string":
                return object.equals(switchVal);
            case "number":
                return switchVal != null && Double.parseDouble((String) object) == (Double) switchVal;
            default:
                logger.info("Unknown type for condition data type {}", dataEval.getType());
        }
        return false;
    }

    /**
     * Parses the input value for the switch condition.
     *
     * @return A string or an integer with the value.
     */
    private Object parseSwitchCondition() {
        switch (dataEval.getType()) {
            case "string":
                return dataValues.get(dataEval.getSource());
            case "number":
                return dataValues.get(dataEval.getSource());
            default:
                logger.info("Unknown type for condition data type {}", dataEval.getType());
        }
        return null;

    }

    /**
     * Sets the passed result as data values.
     */
    @Override
    public void passResult(Map<String, Object> input) {
        synchronized (this) {
            if (dataValues == null) {
                dataValues = new HashMap<>();
            }
            for (DataIns data : dataIns) {
                if (input.containsKey(data.getSource())) {
                    dataValues.put(data.getSource(), input.get(data.getSource()));
                }
            }
            if (input.containsKey(dataEval.getSource())) {
                dataValues.put(dataEval.getSource(), input.get(dataEval.getSource()));
            }
        }

    }

    @Override
    public Map<String, Object> getResult() {
        return null;
    }

}
