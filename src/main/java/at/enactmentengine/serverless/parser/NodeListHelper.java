package at.enactmentengine.serverless.parser;

import at.enactmentengine.serverless.exception.MissingResourceLinkException;
import at.enactmentengine.serverless.nodes.*;
import at.enactmentengine.serverless.object.ListPair;
import at.enactmentengine.serverless.object.Utils;
import at.uibk.dps.afcl.Function;
import at.uibk.dps.afcl.functions.*;
import at.uibk.dps.afcl.functions.objects.Case;
import at.uibk.dps.afcl.functions.objects.Section;
import at.uibk.dps.util.Provider;
import org.apache.commons.lang3.NotImplementedException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper class to create NodeLists out of function constructs
 *
 * @author stefanpedratscher, extended by @author mikahautz
 */
class NodeListHelper {

    /**
     * Flag used to determine whether to simulate or execute.
     */
    private final boolean simulate;

    /**
     * Flag used to determine if the AWS session overhead has already been applied.
     */
    private boolean usedAwsSessionOverhead;

    int executionId;

    /**
     * Default constructor for NodeList helper
     */
    public NodeListHelper() {
        simulate = false;
    }

    /**
     * Constructor for NodeList helper
     *
     * @param simulate whether to simulate or execute
     */
    public NodeListHelper(boolean simulate) {
        this.simulate = simulate;
        usedAwsSessionOverhead = false;
    }


    /**
     * Convert a function to NodeList
     *
     * @param function compound or atomic function
     *
     * @return NodeList
     */
    ListPair<Node, Node> toNodeList(Function function) {
        if (function instanceof AtomicFunction && simulate) {
            AtomicFunction tmp = (AtomicFunction) function;
            boolean useSessionOverhead = false;

            // check if it is the first occurrence of an AWS function, if it is, a session overhead has to be added
            if (!usedAwsSessionOverhead) {
                String resourceLink = null;
                try {
                    resourceLink = Utils.getResourceLink(tmp.getProperties(), null);
                } catch (MissingResourceLinkException e) {
                    throw new RuntimeException(e);
                }
                Provider provider = Utils.detectProvider(resourceLink);
                if (provider == Provider.AWS) {
                    usedAwsSessionOverhead = true;
                    useSessionOverhead = true;
                }
            }

            SimulationNode simulationNode = new SimulationNode(tmp.getName(), tmp.getType(), tmp.getDeployment(),
                    tmp.getProperties(), tmp.getConstraints(), tmp.getDataIns(), tmp.getDataOuts(), executionId, useSessionOverhead);
            return new ListPair<>(simulationNode, simulationNode);
        } else if (function instanceof AtomicFunction) {
            AtomicFunction tmp = (AtomicFunction) function;
            FunctionNode functionNode = new FunctionNode(tmp.getName(), tmp.getType(), tmp.getDeployment(),
                    tmp.getProperties(), tmp.getConstraints(), tmp.getDataIns(), tmp.getDataOuts(), executionId);
            return new ListPair<>(functionNode, functionNode);
        } else if (function instanceof IfThenElse) {
            return toNodeListIf((IfThenElse) function);
        } else if (function instanceof Parallel) {
            return toNodeListParallel((Parallel) function, simulate);
        } else if (function instanceof ParallelFor) {
            return toNodeListParallelFor((ParallelFor) function, simulate);
        } else if (function instanceof Sequence) {
            return toNodeListSequence((Sequence) function);
        } else if (function instanceof Switch) {
            return toNodeListSwitch((Switch) function);
        }
        throw new NotImplementedException("Conversion toNodeList not implemented for " + function.getName());
    }

    /**
     * Convert a switch compound to a NodeList
     *
     * @param function switch function
     *
     * @return NodeList
     */
    private ListPair<Node, Node> toNodeListSwitch(Switch function) {
        SwitchStartNode start = new SwitchStartNode(function.getName(), function.getDataIns(), function.getDataEval(), function.getCases());
        SwitchEndNode end = new SwitchEndNode(function.getName(), function.getDataOuts());

        // Switch cases
        for (Case switchCase : function.getCases()) {
            ListPair<Node, Node> switchPair = new ListPair<>();
            ListPair<Node, Node> startNode = toNodeList(switchCase.getFunctions().get(0));
            switchPair.setStart(startNode.getStart());
            Node currentEnd = startNode.getEnd();
            for (int i = 1; i < switchCase.getFunctions().size(); i++) {
                ListPair<Node, Node> currentListPair = toNodeList(switchCase.getFunctions().get(i));
                currentEnd.addChild(currentListPair.getStart());
                currentListPair.getStart().addParent(currentEnd);
                currentEnd = currentListPair.getEnd();

            }
            switchPair.setEnd(currentEnd);
            start.addChild(switchPair.getStart());
            currentEnd.addChild(end);
            end.addParent(switchPair.getEnd());

        }

        // Default case
        if (function.getDefault() != null) {
            ListPair<Node, Node> switchPair = new ListPair<>();
            ListPair<Node, Node> startNode = toNodeList(function.getDefault().get(0));
            switchPair.setStart(startNode.getStart());
            Node currentEnd = startNode.getEnd();
            for (int i = 1; i < function.getDefault().size(); i++) {
                ListPair<Node, Node> currentListPair = toNodeList(function.getDefault().get(i));
                currentEnd.addChild(currentListPair.getStart());
                currentListPair.getStart().addParent(currentEnd);
                currentEnd = currentListPair.getEnd();

            }
            switchPair.setEnd(currentEnd);
            start.addChild(switchPair.getStart());
            currentEnd.addChild(end);
            end.addParent(switchPair.getEnd());
        }

        return new ListPair<>(start, end);
    }

    /**
     * Convert a sequence compound to a NodeList
     *
     * @param function sequence function
     *
     * @return NodeList
     */
    private ListPair<Node, Node> toNodeListSequence(Sequence function) {
        throw new NotImplementedException("Sequence is default and explicit sequence construct is not implemented. Sequence: " + function.getName());
    }

    /**
     * Convert a parallelFor compound to a NodeList
     *
     * @param function parallelFor function
     *
     * @return NodeList
     */
    private ListPair<Node, Node> toNodeListParallelFor(ParallelFor function, boolean simulate) {
        ParallelForStartNode parallelForStartNode = new ParallelForStartNode(function.getName(), "type", function.getDataIns(), function.getLoopCounter(), function.getProperties(), function.getConstraints());
        ParallelForEndNode parallelForEndNode = new ParallelForEndNode(function.getName(), "", function.getDataOuts(), simulate);

        // Create parallel compound NodeList
        ListPair<Node, Node> firstPair = toNodeList(function.getLoopBody().get(0));
        Node currentEnd = firstPair.getEnd();
        parallelForStartNode.addChild(firstPair.getStart());
        for (int j = 1; j < function.getLoopBody().size(); j++) {
            ListPair<Node, Node> current = toNodeList(function.getLoopBody().get(j));
            currentEnd.addChild(current.getStart());
            current.getStart().addParent(currentEnd);
            currentEnd = current.getEnd();
        }
        currentEnd.addChild(parallelForEndNode);
        parallelForEndNode.addParent(currentEnd);

        return new ListPair<>(parallelForStartNode, parallelForEndNode);
    }

    /**
     * Convert a parallel compound to a NodeList
     *
     * @param function parallel function
     *
     * @return NodeList
     */
    private ListPair<Node, Node> toNodeListParallel(Parallel function, boolean simulate) {
        ParallelStartNode start = new ParallelStartNode(function.getName(), "test", function.getDataIns());
        ParallelEndNode end = new ParallelEndNode(function.getName(), "test", function.getDataOuts());
        boolean reuseSessionOverhead = !usedAwsSessionOverhead;
        // map stores the section number and the iteration depth of where the first aws function occurs
        Map<Integer, Integer> tmpMap = new HashMap<>();
        List<ListPair<Node, Node>> pairList = new ArrayList<>();

        for (int i = 0; i < function.getParallelBody().size(); i++) {
            ListPair<Node, Node> currentListPair = toNodeListSection(function.getParallelBody().get(i));
            currentListPair.getEnd().addChild(end);
            currentListPair.getStart().addParent(start);
            start.addChild(currentListPair.getStart());
            end.addParent(currentListPair.getEnd());

            if (simulate && reuseSessionOverhead) {
                usedAwsSessionOverhead = false;
                tmpMap.put(i, findSessionOverheadIteration(currentListPair.getStart(), 0, true));
                pairList.add(currentListPair);
            }
        }

        if (!tmpMap.isEmpty() && tmpMap.values().stream().anyMatch(value -> value >= 0)) {
            // find the iteration in which an AWS function is started first (= the iteration whose function will start first)
            int minValue = Integer.MAX_VALUE;
            for (Integer value : tmpMap.values()) {
                if (value >= 0 && value < minValue) {
                    minValue = value;
                }
            }
            // reset the SO for all nodes that do not invoke an AWS function in the smallest iteration
            for (Map.Entry<Integer, Integer> entry : tmpMap.entrySet()) {
                if (entry.getValue() != minValue) {
                    resetSessionOverheadIteration(pairList.get(entry.getKey()).getStart());
                }
            }

            usedAwsSessionOverhead = true;
        }

        return new ListPair<>(start, end);
    }

    /**
     * Recursively search through a node tree to find the node that has session overhead
     *
     * @param node            The node to start the search from
     * @param iteration       The current iteration or depth level
     * @param isFirstFunction If the node is the first function node in the tree
     *
     * @return The iteration where the node with session overhead was found, or -1 if not found
     */
    private int findSessionOverheadIteration(Node node, int iteration, boolean isFirstFunction) {
        if (node instanceof SimulationNode && ((SimulationNode) node).hasSessionOverhead()) {
            return iteration;
        }

        if (node instanceof SimulationNode) {
            isFirstFunction = false;
        }

        for (Node child : node.getChildren()) {
            int newIteration = iteration;
            if (child instanceof SimulationNode && !isFirstFunction) {
                newIteration += 1;
            }
            int foundIteration = findSessionOverheadIteration(child, newIteration, isFirstFunction);
            if (foundIteration >= 0) {
                return foundIteration;
            }
        }

        return -1;
    }

    /**
     * Recursively resets the session overhead for the function in the node tree
     *
     * @param node The node to start from
     */
    private void resetSessionOverheadIteration(Node node) {
        if (node instanceof SimulationNode && ((SimulationNode) node).hasSessionOverhead()) {
            ((SimulationNode) node).setUseSessionOverhead(false);
            return;
        }

        for (Node child : node.getChildren()) {
            resetSessionOverheadIteration(child);
        }
    }

    /**
     * Convert a section compound to a NodeList
     *
     * @param section section function
     *
     * @return NodeList
     */
    private ListPair<Node, Node> toNodeListSection(Section section) {
        ListPair<Node, Node> sectionPair = new ListPair<>();
        ListPair<Node, Node> startNode = toNodeList(section.getSection().get(0));

        sectionPair.setStart(startNode.getStart());
        Node currentEnd = startNode.getEnd();
        for (int i = 1; i < section.getSection().size(); i++) {
            ListPair<Node, Node> current = toNodeList(section.getSection().get(i));
            currentEnd.addChild(current.getStart());
            current.getStart().addParent(currentEnd);
            currentEnd = current.getEnd();
        }
        sectionPair.setEnd(currentEnd);

        return sectionPair;
    }

    /**
     * Convert an if compound to a NodeList
     *
     * @param function if function
     *
     * @return NodeList
     */
    private ListPair<Node, Node> toNodeListIf(IfThenElse function) {
        IfStartNode start = new IfStartNode(function.getName(), function.getDataIns(), function.getCondition());
        IfEndNode end = new IfEndNode(function.getName(), function.getDataOuts());

        ListPair<Node, Node> thenPair = new ListPair<>();
        ListPair<Node, Node> startNode = toNodeList(function.getThen().get(0));
        thenPair.setStart(startNode.getStart());
        Node currentEnd = startNode.getEnd();
        for (int i = 1; i < function.getThen().size(); i++) {
            ListPair<Node, Node> current = toNodeList(function.getThen().get(i));
            currentEnd.addChild(current.getStart());
            current.getStart().addParent(currentEnd);
            currentEnd = current.getEnd();
        }
        thenPair.setEnd(currentEnd);
        start.addChild(thenPair.getStart());
        currentEnd.addChild(end);
        end.addParent(thenPair.getEnd());

        ListPair<Node, Node> elsePair = new ListPair<>();
        startNode = toNodeList(function.getElse().get(0));
        elsePair.setStart(startNode.getStart());
        currentEnd = startNode.getEnd();
        for (int i = 1; i < function.getElse().size(); i++) {
            ListPair<Node, Node> current = toNodeList(function.getElse().get(i));
            currentEnd.addChild(current.getStart());
            current.getStart().addParent(currentEnd);
            currentEnd = current.getEnd();
        }
        elsePair.setEnd(currentEnd);
        start.addChild(elsePair.getStart());
        currentEnd.addChild(end);
        end.addParent(elsePair.getEnd());

        return new ListPair<>(start, end);
    }
}