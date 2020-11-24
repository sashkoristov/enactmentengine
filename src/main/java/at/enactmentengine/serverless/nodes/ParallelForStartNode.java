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
 */
public class ParallelForStartNode extends Node {
    static final Logger logger = LoggerFactory.getLogger(ParallelForStartNode.class);

    private List<DataIns> definedInput;
    private Map<String, Object> counterValues;
    private int counterStart;
    private int counterEnd;
    private int counterStepSize;
    private String[] counterVariableNames;
    public static final int MAX_NUMBER_THREADS = 1000;

    public ParallelForStartNode(String name, String type, List<DataIns> definedInput, LoopCounter loopCounter) {
        super(name, type);
        this.definedInput = definedInput;
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

        try {
            counterStart = Integer.valueOf(loopCounter.getFrom());
        } catch (NumberFormatException e) {
            counterVariableNames[0] = loopCounter.getFrom();
        }
        try {
            counterEnd = Integer.valueOf(loopCounter.getTo());
        } catch (NumberFormatException e) {
            counterVariableNames[1] = loopCounter.getTo();
        }

        counterStepSize = 1;

    }

    /**
     * Saves the passed result as dataValues.
     */
    @Override
    public void passResult(Map<String, Object> input) {
        synchronized (this) {
            if (dataValues == null) {
                dataValues = new HashMap<>();
            }
            if (counterValues == null) {
                counterValues = new HashMap<>();
            }
            if (definedInput != null) {
                for (DataIns data : definedInput) {
                    if (input.containsKey(data.getSource())) {
                        dataValues.put(data.getSource(), input.get(data.getSource()));
                    }
                }
            }
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
     */
    @Override
    public Boolean call() throws Exception {
        final Map<String, Object> outVals = new HashMap<>();
        if (definedInput != null) {
            for (DataIns data : definedInput) {
                if (!dataValues.containsKey(data.getSource())) {
                    throw new MissingInputDataException(ParallelForStartNode.class.getCanonicalName() + ": " + name
                            + " needs " + data.getSource() + "!");
                } else {
                    outVals.put(name + "/" + data.getName(), dataValues.get(data.getSource()));
                }
            }
        }

        // distribute value for next functions

        logger.info("Executing {} ParallelForStartNodeOld", name);

        addChildren();
        ExecutorService exec = Executors
                .newFixedThreadPool(children.size() > MAX_NUMBER_THREADS ? MAX_NUMBER_THREADS : children.size());
        List<Future<Boolean>> futures = new ArrayList<>();

        List<Map<String, Object>> outValsForChilds = transferOutVals(children.size(), outVals);

        for (int i = 0; i < children.size(); i++) {
            Node node = children.get(i);
            if (i < outValsForChilds.size()) {
                node.passResult(outValsForChilds.get(i));
            }
            futures.add(exec.submit(node));
        }
        for (Future<Boolean> future : futures) {
            future.get();
        }
        exec.shutdown();
        return true;
    }

    /**
     * Adds a specific number of children depending on the values counterStart,
     * counterEnd and counterStepSize.
     *
     * @throws MissingInputDataException  on missing input
     * @throws CloneNotSupportedException on unsupported clone
     */
    private void addChildren() throws MissingInputDataException, CloneNotSupportedException {
        // add children depending on the counter value
        for (String counterKeyName : counterVariableNames) {
            if (counterKeyName != null && !counterValues.containsKey(counterKeyName)) {
                throw new MissingInputDataException(
                        ParallelForStartNode.class.getCanonicalName() + ": " + name + " needs " + counterKeyName + "!");
            }
        }
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

        ParallelForEndNode endNode = findParallelForEndNode(children.get(0), 0);

        for (int i = counterStart; i < counterEnd - 1; i += counterStepSize) {
            Node node = children.get(0).clone(endNode);
            children.add(node);

        }
        assert endNode != null;
        endNode.setNumberOfChildren(children.size());
    }

    /**
     * Finds the matching ParallelForEndNodeOld recursively.
     *
     * @param currentNode
     * @param depth
     * @return
     */
    private ParallelForEndNode findParallelForEndNode(Node currentNode, int depth) {
        for (Node child : currentNode.getChildren()) {
            if (child instanceof ParallelForEndNode) {
                if (depth == 0) {
                    return (ParallelForEndNode) child;
                } else {
                    return findParallelForEndNode(child, depth - 1);
                }

            } else if (child instanceof ParallelForStartNode) {
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
     * @param childs  The number of children.
     * @param outVals The output values.
     * @return the transferred output values.
     */
    private ArrayList<Map<String, Object>> transferOutVals(int childs, Map<String, Object> outVals) {
        ArrayList<Map<String, Object>> values = new ArrayList<>();
        if (definedInput != null) {
            for (DataIns data : definedInput) {
                if (data.getConstraints() != null) {
                    if(dataValues.get(data.getSource()) instanceof ArrayList || dataValues.get(data.getSource()) instanceof JsonArray){
                        JsonArray dataElements = new Gson().toJsonTree(dataValues.get(data.getSource())).getAsJsonArray();
                        List<JsonArray> distributedElements = distributeElements(dataElements, data.getConstraints(), childs);

                        checkDistributedElements(distributedElements, data, values);
                    } else {
                        if (dataValues.get(data.getSource()) instanceof Double){
                            JsonArray dataElements = new JsonArray();
                            dataElements.add((Double) dataValues.get(data.getSource()));
                            List<JsonArray> distributedElements = distributeElements(dataElements, data.getConstraints(), childs);

                            checkDistributedElements(distributedElements, data, values);
                        } else if (dataValues.get(data.getSource()) instanceof Integer){
                            JsonArray dataElements = new JsonArray();
                            dataElements.add((Integer) dataValues.get(data.getSource()));
                            List<JsonArray> distributedElements = distributeElements(dataElements, data.getConstraints(), childs);

                            checkDistributedElements(distributedElements, data, values);
                        }else{
                            throw new NotImplementedException("Not implemented: " + dataValues.get(data.getSource()).getClass());
                        }
                    }

                } else {
                    if (data.getPassing() != null && data.getPassing()) {
                        checkDataPassing(outVals, data, childs, values);
                    }
                }
            }
        }
        return values;
    }

    private void checkDataPassing(Map<String, Object> outVals, DataIns data, int childs, ArrayList<Map<String, Object>> values) {
        if (outVals.containsKey(this.name + "/" + data.getName())) {
            for (int i = 0; i < childs; i++) {
                if (values.size() > i) {
                    values.get(i).put(data.getName(), outVals.get(this.name + "/" + data.getName()));
                } else {
                    Map<String, Object> tmp = new HashMap<>();
                    tmp.put(data.getName(), outVals.get(this.name + "/" + data.getName()));
                    values.add(i, tmp);
                }
            }
        } else {
            logger.error("Cannot Pass data {}. No such matching value could be found", data.getName());
        }
    }

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

    @Override
    public Map<String, Object> getResult() {
        return null;
    }

    public List<DataIns> getDefinedInput() {
        return definedInput;
    }

    public void setDefinedInput(List<DataIns> definedInput) {
        this.definedInput = definedInput;
    }
}
