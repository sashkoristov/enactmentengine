package at.enactmentengine.serverless.nodes;

import at.uibk.dps.afcl.functions.objects.PropertyConstraint;
import at.enactmentengine.serverless.exception.MissingInputDataException;
import at.uibk.dps.afcl.functions.objects.DataIns;
import at.uibk.dps.afcl.functions.objects.LoopCounter;
import com.google.gson.JsonArray;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Control node which manages the tasks at the start of a parallel for loop.
 *
 * @author markusmoosbrugger, jakobnoeckl
 */
public class ParallelForStartNode extends Node {
    final static Logger logger = LoggerFactory.getLogger(ParallelForStartNode.class);

    // private String distribution;
    public List<DataIns> definedInput;
    private Map<String, Object> counterValues;
    private int counterStart;
    private int counterEnd;
    private int counterStepSize;
    private String[] counterVariableNames;
    private static int MAX_NUMBER_THREADS = 1000;

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
     * @param loopCounter
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
                dataValues = new HashMap<String, Object>();
            }
            if (counterValues == null) {
                counterValues = new HashMap<String, Object>();
            }
            if(definedInput != null){
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
        if(definedInput != null){
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

        logger.info("Executing " + name + " ParallelForStartNodeOld");

        addChildren(outVals);
        ExecutorService exec = Executors
                .newFixedThreadPool(children.size() > MAX_NUMBER_THREADS ? MAX_NUMBER_THREADS : children.size());
        List<Future<Boolean>> futures = new ArrayList<>();

        List<Map<String, Object>> outValsForChilds = transferOutVals(children.size(), outVals);
        
        for (int i = 0; i < children.size(); i++) {
            Node node = children.get(i);
            // outVals.put("/EE/"+name+"/counter", new Integer(i));
            if(outValsForChilds != null && i < outValsForChilds.size()) {
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
     * @param outVals The output values of the parent functions. These are also the
     *                input values for the children of this node.
     * @throws MissingInputDataException
     * @throws CloneNotSupportedException
     */
    private void addChildren(Map<String, Object> outVals) throws MissingInputDataException, CloneNotSupportedException {
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
            counterEnd = ((Integer) counterValues.get(counterVariableNames[1])).intValue();
        }
        if (counterVariableNames[2] != null) {
            counterStepSize = Integer.parseInt((String) counterValues.get(counterVariableNames[2]));
        }
        logger.info("Counter values for " + ParallelForStartNode.class.getCanonicalName() + ": counterStart: "
                + counterStart + ", counterEnd: " + counterEnd + ", stepSize: " + counterStepSize);

        ParallelForEndNode endNode = findParallelForEndNode(children.get(0), 0);

        for (int i = counterStart; i < counterEnd - 1; i += counterStepSize) {
            Node node = (Node) children.get(0).clone(endNode);
            children.add(node);

        }
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
     * @throws Exception
     */
    private ArrayList<Map<String, Object>> transferOutVals(int childs, Map<String, Object> outVals) throws Exception {
        ArrayList<Map<String, Object>> values = new ArrayList<>();
        if(definedInput != null){
            for (DataIns data : definedInput) {
                if(data.getConstraints() != null) {
                    for (PropertyConstraint constraint : data.getConstraints()) {
                        if (constraint.getName().equals("distribution")) {
                            if (constraint.getValue().contains("BLOCK")) {
                                String blockValue = constraint.getValue().replaceAll("[^0-9?!\\.]", "");
                                ArrayList<Map<String, Object>> tmp = distributeOutValsBlock(data, blockValue, childs);
                                for (int i = 0; i < tmp.size(); i++) {
                                    if (values.size() > i) {
                                        values.get(i).putAll(tmp.get(i));
                                    } else {
                                        values.add(i, tmp.get(i));
                                    }
                                }
                            } else {
                                throw new NotImplementedException("Distribution type for " + constraint.getValue() + " not implemented.");
                            }
                        } else if (constraint.getName().equals("element-index")) {
                            throw new NotImplementedException("Element index " + constraint.getValue() + " not implemented.");
                        } else {
                            throw new NotImplementedException("Constraint " + constraint.getName() + " not implemented.");
                        }
                    }
                }else{
                    if(data.getPassing() != null && data.getPassing()){
                        if(outVals.containsKey(this.name + "/" + data.getName())){
                            for(int i = 0; i < childs; i++){
                                if (values.size() > i) {
                                    values.get(i).put(data.getName(), outVals.get(this.name + "/" + data.getName()));
                                } else {
                                    Map<String, Object> tmp = new HashMap<>();
                                    tmp.put(data.getName(), outVals.get(this.name + "/" + data.getName()));
                                    values.add(i, tmp);
                                }
                            }
                        }else{
                            System.err.println("Cannot Pass data " + data.getName() + ". No such matching value could be found");
                        }
                    }
                }
            }
        }
        return values;
    }

    /**
     * Distributes the output values in BLOCK mode. The collection is splitted into
     * blocks of the given size.
     *
     * @param data      The data element.
     * @param blockSize The block size of each block.
     * @param childs    The number of children.
     * @return An ArrayList with the data blocks.
     */
    private ArrayList<Map<String, Object>> distributeOutValsBlock(DataIns data, String blockSize, int childs) {
        JsonArray jsonArr = (JsonArray) dataValues.get(data.getSource());
        int size = Integer.parseInt(blockSize);
        JsonArray distributedArray = new JsonArray();
        ArrayList<Map<String, Object>> distributedValues = new ArrayList<>();
        for (int i = 1; i <= jsonArr.size(); i++) {
            distributedArray.add(jsonArr.get(i - 1));
            if ((i % size == 0 && i - size >= 0) || i == jsonArr.size()) {
                Map<String, Object> map = new HashMap<>();
                if (distributedArray.size() == 1) {
                    Object o;
                    if (data.getType().equals("number")) {
                        o = new Integer(distributedArray.get(0).getAsInt());
                    } else {
                        o = distributedArray.get(0).getAsString();
                    }
                    map.put(name + "/" + data.getName(), o);
                } else
                    map.put(name + "/" + data.getName(), distributedArray);

                distributedValues.add(map);
                distributedArray = new JsonArray();
            }

        }

        return distributedValues;
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
