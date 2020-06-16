package at.enactmentengine.serverless.parser;

import at.enactmentengine.serverless.nodes.*;
import at.uibk.dps.afcl.functions.*;
import at.uibk.dps.afcl.functions.objects.Case;
import at.uibk.dps.afcl.functions.objects.Section;
import at.uibk.dps.afcl.Function;
import org.apache.commons.lang3.NotImplementedException;

/**
 * Helper class to create NodeLists out of function constructs
 *
 * @author stefanpedratscher
 */
class NodeListHelper {

    /**
     * Default constructor for NodeList helper
     */
    NodeListHelper() {
    }


    /**
     * Convert a function to NodeList
     *
     * @param function compound or atomic function
     * @return NodeList
     */
    ListPair<Node, Node> toNodeList(Function function) {
        if (function instanceof AtomicFunction) {
            AtomicFunction tmp = (AtomicFunction) function;
            FunctionNode functionNode = new FunctionNode(tmp.getName(), tmp.getType(), tmp.getProperties(), tmp.getConstraints(), tmp.getDataIns(), tmp.getDataOuts());
            return new ListPair<>(functionNode, functionNode);
        } else if (function instanceof IfThenElse) {
            return toNodeListIf((IfThenElse) function);
        } else if (function instanceof Parallel) {
            return toNodeListParallel((Parallel) function);
        } else if (function instanceof ParallelFor) {
            return toNodeListParallelFor((ParallelFor) function);
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
     * @return NodeList
     */
    private ListPair<Node, Node> toNodeListSequence(Sequence function) {
        throw new NotImplementedException("Sequence is default and explicit sequence construct is not implemented.");
    }

    /**
     * Convert a parallelFor compound to a NodeList
     *
     * @param function parallelFor function
     * @return NodeList
     */
    private ListPair<Node, Node> toNodeListParallelFor(ParallelFor function) {
        ParallelForStartNode parallelForStartNode = new ParallelForStartNode(function.getName(), "type", function.getDataIns(), function.getLoopCounter());
        ParallelForEndNode parallelForEndNode = new ParallelForEndNode(function.getName(), "", function.getDataOuts());

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
     * @return NodeList
     */
    private ListPair<Node, Node> toNodeListParallel(Parallel function) {
        ParallelStartNode start = new ParallelStartNode(function.getName(), "test", function.getDataIns());
        ParallelEndNode end = new ParallelEndNode(function.getName(), "test", function.getDataOuts());

        for (int i = 0; i < function.getParallelBody().size(); i++) {
            ListPair<Node, Node> currentListPair = toNodeListSection(function.getParallelBody().get(i));
            currentListPair.getEnd().addChild(end);
            currentListPair.getStart().addParent(start);
            start.addChild(currentListPair.getStart());
            end.addParent(currentListPair.getEnd());
        }

        return new ListPair<>(start, end);
    }

    /**
     * Convert a section compound to a NodeList
     *
     * @param section section function
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