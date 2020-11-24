package at.enactmentengine.serverless.nodes;

import at.enactmentengine.serverless.exception.MissingInputDataException;
import at.uibk.dps.afcl.functions.objects.ACondition;
import at.uibk.dps.afcl.functions.objects.Condition;
import at.uibk.dps.afcl.functions.objects.DataIns;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Control node which manages the tasks at the start of a if element.
 *
 * @author markusmoosbrugger, jakobnoeckl
 * adapted by @author stefanpedratscher
 */
public class IfStartNode extends Node {

    /**
     * Logger for the an if-start node.
     */
    static final Logger logger = LoggerFactory.getLogger(IfStartNode.class);

    /**
     * Condition of the if node (if statement).
     */
    private Condition condition;

    /**
     * The input specified in the workflow file.
     */
    private List<DataIns> dataIns;

    /**
     * Constructor for a if-start node.
     *
     * @param name of the if construct.
     * @param dataIns input specified in the workflow file.
     * @param condition of the if node (if statement).
     */
    public IfStartNode(String name, List<DataIns> dataIns, Condition condition) {
        super(name, "");
        this.condition = condition;
        this.dataIns = dataIns;
    }

    /**
     * Checks the dataValues and evaluates the condition. Depending on the
     * evaluation either the if or else branch is executed.
     *
     * @return boolean representing success of the node execution.
     * @throws Exception on failure.
     */
    @Override
    public Boolean call() throws Exception {

        /* Iterate over all specified inputs and check if they are present */
        final Map<String, Object> ifInputValues = new HashMap<>();

        /* Check if input data is specified */
        if(dataIns != null){

            /* Iterate over every input specified in the workflow file */
            for (DataIns data : dataIns) {

                /* Check if the actual input does not contains the specified input */
                if (!dataValues.containsKey(data.getSource())) {
                    throw new MissingInputDataException(
                            IfStartNode.class.getCanonicalName() + ": " + name + " needs " + data.getSource() + "!");
                } else {
                    ifInputValues.put(name + "/" + data.getName(), dataValues.get(data.getSource()));
                }
            }
        }


        /* Keeps track whether the condition of th if statement is valid */
        boolean statementEvaluationValue = false;

        /* Iterate over all part-conditions */
        for (ACondition conditionElement : condition.getConditions()) {

            /* Evaluate if condition */
            statementEvaluationValue = evaluate(conditionElement, ifInputValues);

            /* Check if we can stop checking the statement */
            if (("or".equals(condition.getCombinedWith()) && statementEvaluationValue) ||
                    ("and".equals(condition.getCombinedWith()) && !statementEvaluationValue)) {
                break;
            }
        }

        Node node;
        if (statementEvaluationValue) {

            /* Execute the if branch */
            // TODO can this be hardcoded?
            node = children.get(0);
            logger.info("Executing {} IfStartNodeOld in if branch.", name);
        } else {

            /* Execute the else branch */
            // TODO can this be hardcoded?
            node = children.get(1);
            logger.info("Executing {} IfStartNodeOld in else branch.", name);
        }

        /* Pass data to the according branch and execute */
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
     * false.
     * @throws MissingInputDataException on missing input data description
     */
    private boolean evaluate(ACondition conditionElement, Map<String, Object> ifInputValues)
            throws MissingInputDataException {

        // TODO maybe use Number datatype? Allows to caompare also int and double?!
        //Number data1 = (Number) ifInputValues.get(conditionElement.getData1());

        /* Get the data which should be evaluated */
        int data1 = parseCondition(conditionElement.getData1(), ifInputValues);
        int data2 = parseCondition(conditionElement.getData2(), ifInputValues);

        /* Evaluate the condition */
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
                logger.info("Operator {} not supported ", conditionElement.getOperator());
        }
        return false;
    }

    /**
     * Sets the passed result as dataValues.
     */
    @Override
    public void passResult(Map<String, Object> input) {
        synchronized (this) {

            /* Check if actual input data list is already created */
            if (dataValues == null) {
                dataValues = new HashMap<>();
            }

            /* Check if there is input specified in the workflow file */
            if(dataIns != null){
                for (DataIns data : dataIns) {

                    /* Add specified inputs to the actual inputs list */
                    if (input.containsKey(data.getSource())) {
                        dataValues.put(data.getSource(), input.get(data.getSource()));
                    }
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
     * @throws MissingInputDataException on missing input
     */
    private int parseCondition(String string, Map<String, Object> ifInputValues) throws MissingInputDataException {

        // TODO do we really need this function?
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

    /**
     * Get the result of the if-start construct.
     *
     * @return null because the if-start does not generate a result.
     */
    @Override
    public Map<String, Object> getResult() {
        return null;
    }

}
