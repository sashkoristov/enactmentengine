package at.enactmentengine.serverless.nodes;

import at.enactmentengine.serverless.parser.ElementIndex;
import at.uibk.dps.afcl.functions.objects.PropertyConstraint;
import at.enactmentengine.serverless.exception.MissingInputDataException;
import at.uibk.dps.afcl.functions.objects.DataIns;
import at.uibk.dps.afcl.functions.objects.LoopCounter;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import org.apache.commons.lang3.NotImplementedException;
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
 * Control node which manages the tasks at the start of a parallel for loop.
 *
 * @author markusmoosbrugger, jakobnoeckl
 * adapted by @author stefanpedratscher
 */
public class ParallelForStartNode extends Node {

    /**
     * Logger for parallel-for-start node.
     */
    static final Logger logger = LoggerFactory.getLogger(ParallelForStartNode.class);

    /**
     * Input data defined in the workflow file.
     */
    private List<DataIns> dataIns;

    /**
     * The actual values of the counter variables.
     */
    private Map<String, Object> counterValues;

    /**
     * The start value of the loop counter.
     */
    private int counterStart;

    /**
     * The end value of the loop counter.
     */
    private int counterEnd;

    /**
     * The step size for the loop counter.
     */
    private int counterStepSize;

    /**
     * Contains all counter variable names.
     */
    private String[] counterVariableNames;

    /**
     * The maximum number of concurrent function executions.
     */
    public static final int MAX_NUMBER_THREADS = 1000;

    /**
     * Default constructor for the parallel-for-start node.
     *
     * @param name of the parallel-for-start node.
     * @param type of the parallel-for-start node.
     * @param dataIns specified in the workflow file.
     * @param loopCounter of the parallel-for-start node.
     */
    public ParallelForStartNode(String name, String type, List<DataIns> dataIns, LoopCounter loopCounter) {
        super(name, type);
        this.dataIns = dataIns;
        counterVariableNames = new String[3];
        parseLoopCounter(loopCounter);
    }

    /**
     * Parses the loop counter. Tries to cast each value as integer. If casting as
     * integer is not possible it assumes that the value comes from a variable.
     *
     * @param loopCounter loopCounter
     */
    private void parseLoopCounter(LoopCounter loopCounter) {

        /* Try to parse the from value of the loop counter */
        try {
            counterStart = Integer.valueOf(loopCounter.getFrom());
        } catch (NumberFormatException e) {
            counterVariableNames[0] = loopCounter.getFrom();
        }

        /* Try to parse the to value of the loop counter */
        try {
            counterEnd = Integer.valueOf(loopCounter.getTo());
        } catch (NumberFormatException e) {
            counterVariableNames[1] = loopCounter.getTo();
        }

        /* Try to parse the step value of the loop counter */
        try {
            counterStepSize = Integer.valueOf(loopCounter.getStep());
        } catch (NumberFormatException e) {

            // TODO should string be allowed for dynamic variables?
            counterStepSize = 1;
        }
    }

    /**
     * Saves the passed result as dataValues.
     *
     * @param input values to pass.
     */
    @Override
    public void passResult(Map<String, Object> input) {
        synchronized (this) {

            /* Prepare data value holders, if not already done */
            if (dataValues == null) {
                dataValues = new HashMap<>();
            }
            if (counterValues == null) {
                counterValues = new HashMap<>();
            }

            /* Check if there is an input specified */
            if (dataIns != null) {

                /* Iterate over inputs and add corresponding values to the data values */
                for (DataIns data : dataIns) {
                    if (input.containsKey(data.getSource())) {
                        dataValues.put(data.getSource(), input.get(data.getSource()));
                    }
                }
            }

            /* Iterate over counter variables and check if the input contains the values */
            for (String counterValue : counterVariableNames) {
                if (input.containsKey(counterValue)) {
                    counterValues.put(counterValue, input.get(counterValue));
                }
            }
        }
    }

    /**
     * Checks the input values, adds specific number of children depending on the
     * input values and creates a thread pool for execution of the children.
     *
     * @return True on success, False otherwise
     * @throws Exception on failure
     */
    @Override
    public Boolean call() throws Exception {

        /* Prepare the output values */
        final Map<String, Object> outValues = new HashMap<>();

        /* Check if there is input defined in the workflow file */
        if (dataIns != null) {

            /* Iterate over the input data and handle input values */
            for (DataIns data : dataIns) {
                if (!dataValues.containsKey(data.getSource())) {
                    throw new MissingInputDataException(ParallelForStartNode.class.getCanonicalName() + ": " + name
                            + " needs " + data.getSource() + "!");
                } else {
                    outValues.put(name + "/" + data.getName(), dataValues.get(data.getSource()));
                }
            }
        }

        logger.info("Executing {} ParallelForStartNodeOld", name);

        /* Create all children functions (all functions inside the parallel-for) */
        addChildren();

        /* Create a fixed thread-pool managing the parallel executions */
        ExecutorService exec = Executors
                .newFixedThreadPool(children.size() > MAX_NUMBER_THREADS ? MAX_NUMBER_THREADS : children.size());
        List<Future<Boolean>> futures = new ArrayList<>();
        List<Map<String, Object>> outValuesForChildren = transferOutVals(children.size(), outValues);

        /* Iterate over all children */
        for (int i = 0; i < children.size(); i++) {

            Node node = children.get(i);

            /* Pass results to the children (if there is an output value left) */
            if (i < outValuesForChildren.size()) {
                node.passResult(outValuesForChildren.get(i));
            }

            /* Execute the child node */
            futures.add(exec.submit(node));
        }

        /* Wait for all children to finish */
        for (Future<Boolean> future : futures) {
            future.get();
        }

        /* Terminate executor */
        exec.shutdown();

        return true;
    }

    /**
     * Adds a specific number of children depending on the values counterStart,
     * counterEnd and counterStepSize.
     *
     * @throws MissingInputDataException  on missing input.
     * @throws CloneNotSupportedException on unsupported clone.
     */
    private void addChildren() throws MissingInputDataException, CloneNotSupportedException {

        /* Iterate over counter variables and check if there is the according value */
        for (String counterKeyName : counterVariableNames) {
            if (counterKeyName != null && !counterValues.containsKey(counterKeyName)) {
                throw new MissingInputDataException(
                        ParallelForStartNode.class.getCanonicalName() + ": " + name + " needs " + counterKeyName + "!");
            }
        }

        // TODO could counterStart, counterEnd and counterStepSize be of type NUMBER?

        /* Parse actual value of the defined variables */
        if (counterVariableNames[0] != null) {
            counterStart = Integer.parseInt((String) counterValues.get(counterVariableNames[0]));
        }
        if (counterVariableNames[1] != null) {
            counterEnd = ((int) Double.parseDouble(counterValues.get(counterVariableNames[1]).toString()));
        }
        if (counterVariableNames[2] != null) {
            counterStepSize = Integer.parseInt((String) counterValues.get(counterVariableNames[2]));
        }

        logger.info("Counter values for "+ParallelForStartNode.class.getCanonicalName()+" : " +
                        "counterStart: "+counterStart+", counterEnd: "+counterEnd+", stepSize: "+counterStepSize+"");

        /* Search the end node of the parallel-for */
        ParallelForEndNode endNode = findParallelForEndNode(children.get(0), 0);

        /* Add children to the list of children */
        for (int i = counterStart; i < counterEnd - 1; i += counterStepSize) {
            Node node = children.get(0).clone(endNode);
            children.add(node);
        }

        assert endNode != null;

        /* Set the number all children in the parallel-for */
        endNode.setNumberOfParents(children.size());
    }

    /**
     * Finds the matching ParallelForEndNodeOld recursively.
     *
     * @param currentNode node to start looking for.
     * @param depth recursive depth to check which parallel-for node is checked.
     *
     * @return the end node of the parallel-for.
     */
    private ParallelForEndNode findParallelForEndNode(Node currentNode, int depth) {

        /* Iterate over all children */
        for (Node child : currentNode.getChildren()) {
            if (child instanceof ParallelForEndNode) {

                /* Check if we found the end node of the correct parallel-for node */
                if (depth == 0) {
                    return (ParallelForEndNode) child;
                } else {

                    /* We found an end node of a nested parallel-for */
                    return findParallelForEndNode(child, depth - 1);
                }
            } else if (child instanceof ParallelForStartNode) {

                /* We found the start node of another parallel-for */
                return findParallelForEndNode(child, depth + 1);
            } else {
                return findParallelForEndNode(child, depth);
            }
        }
        return null;
    }

    /**
     * Transfers the output values depending on the specified dataFlow type.
     *
     * @param children  The number of children.
     * @param outValues The output values.
     *
     * @return the transferred output values.
     */
    private ArrayList<Map<String, Object>> transferOutVals(int children, Map<String, Object> outValues) {

        ArrayList<Map<String, Object>> values = new ArrayList<>();

        /* Check if there is an input defined */
        if (dataIns != null) {

            /* Iterate over the input data defined in the workflow file */
            for (DataIns data : dataIns) {

                /* Check of there are constraints defined */
                if (data.getConstraints() != null) {

                    /* Check if the actual input is an array */
                    if(dataValues.get(data.getSource()) instanceof ArrayList || dataValues.get(data.getSource()) instanceof JsonArray){

                        /* Convert the data to an array */
                        JsonArray dataElements = new Gson().toJsonTree(dataValues.get(data.getSource())).getAsJsonArray();

                        /* Check if a distribution is specified */
                        List<JsonArray> distributedElements = distributeElements(dataElements, data.getConstraints(), children);
                        checkDistributedElements(distributedElements, data, values);
                    } else {

                        // TODO can the following be simplified and generalized e.g. also for bool etc.?
                        if (dataValues.get(data.getSource()) instanceof Double){
                            JsonArray dataElements = new JsonArray();
                            dataElements.add((Double) dataValues.get(data.getSource()));
                            List<JsonArray> distributedElements = distributeElements(dataElements, data.getConstraints(), children);

                            checkDistributedElements(distributedElements, data, values);
                        } else if (dataValues.get(data.getSource()) instanceof Integer){
                            JsonArray dataElements = new JsonArray();
                            dataElements.add((Integer) dataValues.get(data.getSource()));
                            List<JsonArray> distributedElements = distributeElements(dataElements, data.getConstraints(), children);

                            checkDistributedElements(distributedElements, data, values);
                        } else if (dataValues.get(data.getSource()) instanceof String){
                            JsonArray dataElements = new JsonArray();
                            dataElements.add((String) dataValues.get(data.getSource()));
                            List<JsonArray> distributedElements = distributeElements(dataElements, data.getConstraints(), children);

                            checkDistributedElements(distributedElements, data, values);
                        } else {
                            throw new NotImplementedException("Not implemented: " + dataValues.get(data.getSource()).getClass());
                        }
                    }
                } else {

                    /* Check if data should be passed */
                    if (data.getPassing() != null && data.getPassing()) {
                        passData(outValues, data, children, values);
                    }
                }
            }
        }
        return values;
    }

    /**
     * Pass the data.
     *
     * @param outValues
     * @param data
     * @param numChildren
     * @param values
     */
    private void passData(Map<String, Object> outValues, DataIns data, int numChildren, ArrayList<Map<String, Object>> values) {

        /* Check if the output contains the specified key */
        if (outValues.containsKey(this.name + "/" + data.getName())) {

            /* Iterate over all children */
            for (int i = 0; i < numChildren; i++) {

                /* Check if there is data for the specified child */
                if (values.size() > i) {
                    values.get(i).put(data.getName(), outValues.get(this.name + "/" + data.getName()));
                } else {
                    Map<String, Object> tmp = new HashMap<>();
                    tmp.put(data.getName(), outValues.get(this.name + "/" + data.getName()));
                    values.add(i, tmp);
                }
            }
        } else {
            logger.error("Cannot Pass data {}. No such matching value could be found", data.getName());
        }
    }

    /**
     *
     *
     * @param distributedElements
     * @param data
     * @param values
     */
    private void checkDistributedElements(List<JsonArray> distributedElements, DataIns data, ArrayList<Map<String, Object>> values) {
        for (int i = 0; i < distributedElements.size(); i++) {
            Object block = distributedElements.get(i);
            if (distributedElements.get(i).size() == 1) {
                // Extract single value
                JsonArray arr = distributedElements.get(i);
                block = data.getType().equals("number") ? arr.get(0).getAsInt() : arr.get(0);
            }

            String key = name + "/" + data.getName();
            if (values.size() > i) {
                // Use the child map we already created for another DataIns port
                values.get(i).put(key, block);
            } else {
                Map<String, Object> map = new HashMap<>();
                map.put(key, block);
                values.add(i, map);
            }
        }
    }

    /**
     * Distributes the given elements in BLOCK mode. The collection is split into
     * blocks of the given size.
     *
     * @param elements  The data elements to distribute.
     * @param blockSize The block size of each block.
     * @return The data blocks in a list.
     */
    private List<JsonArray> distributeOutValsBlock(JsonArray elements, int blockSize) {
        List<JsonArray> blocks = new ArrayList<>();
        JsonArray currentBlock = new JsonArray();

        for (int i = 0; i < elements.size(); i++) {
            currentBlock.add(elements.get(i));
            // Complete the current block if it is full or we ran out of elements
            if (currentBlock.size() >= blockSize || i == (elements.size() - 1)) {
                blocks.add(currentBlock);
                currentBlock = new JsonArray();
            }
        }

        return blocks;
    }

    /**
     * Distributes the given data elements across loop iterations taking into account the given constraints.
     *
     * @param dataElements the data elements to distribute
     * @param constraints  the constraints to consider
     * @param children     the number of children (iterations)
     * @return a list containing the distributed elements
     */
    protected List<JsonArray> distributeElements(JsonArray dataElements, List<PropertyConstraint> constraints,
                                                 int children) {
        // Check for unknown constraints
        for (PropertyConstraint constraint : constraints) {
            if (constraint.getName().equals("element-index") || constraint.getName().equals("distribution")) {
                continue;
            }
            throw new NotImplementedException("Constraint " + constraint.getName() + " not implemented.");
        }

        // Element-index constraint has higher precedence than distribution constraint
        PropertyConstraint elementIndexConstraint = getPropertyConstraintByName(constraints, "element-index");
        if (elementIndexConstraint != null) {
            // Create a subset of the collection using the indices specified in the element-index constraint
            List<Integer> indices = ElementIndex.parseIndices(elementIndexConstraint.getValue());
            JsonArray subset = new JsonArray(indices.size());
            for (Integer i : indices) {
                subset.add(dataElements.get(i));
            }
            dataElements = subset;
        }

        // Distribute
        List<JsonArray> distributedElements;
        PropertyConstraint distributionConstraint = getPropertyConstraintByName(constraints, "distribution");
        if (distributionConstraint != null) {
            if (distributionConstraint.getValue().contains("BLOCK")) {
                int blockSize = Integer.parseInt(distributionConstraint.getValue().replaceAll("[^0-9?!.]", ""));
                distributedElements = distributeOutValsBlock(dataElements, blockSize);
            } else if (distributionConstraint.getValue().contains("REPLICATE")) {
                int replicaSize;
                if (distributionConstraint.getValue().contains("REPLICATE(*)")) {
                    replicaSize = children;
                } else {
                    replicaSize = Integer.parseInt(distributionConstraint.getValue().replaceAll("[^0-9?!.]", ""));
                }
                distributedElements = new ArrayList<>();
                for (int i = 0; i < replicaSize; i++) {
                    distributedElements.add(dataElements);
                }
            } else {
                throw new NotImplementedException("Distribution type for " + distributionConstraint.getValue()
                        + " not implemented.");
            }
        } else {
            // Provide the same elements to each child if no distribution constraint is specified
            distributedElements = new ArrayList<>();
            for (int i = 0; i < children; i++) {
                distributedElements.add(dataElements);
            }
        }

        return distributedElements;
    }

    /**
     * Returns the first property constraint with the given name or {@code null} if it does not exist.
     *
     * @param propertyConstraints the property constraints
     * @param name                the name of the property constraint to be searched for
     * @return the first property constraint with the given name or {@code null} if it does not exist
     */
    protected PropertyConstraint getPropertyConstraintByName(List<PropertyConstraint> propertyConstraints,
                                                             String name) {
        return propertyConstraints
                .stream()
                .filter(x -> x.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    /** Getter and Setter */

    @Override
    public Map<String, Object> getResult() {
        return null;
    }

    public List<DataIns> getDataIns() {
        return dataIns;
    }

    public void setDataIns(List<DataIns> dataIns) {
        this.dataIns = dataIns;
    }
}
