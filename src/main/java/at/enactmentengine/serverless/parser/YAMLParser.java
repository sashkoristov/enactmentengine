package at.enactmentengine.serverless.parser;

import at.enactmentengine.serverless.nodes.ExecutableWorkflow;
import at.enactmentengine.serverless.nodes.ListPair;
import at.enactmentengine.serverless.nodes.Node;
import com.dps.afcl.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for parsing YAML files into an executable workflow.
 *
 * @author markusmoosbrugger, jakobnoeckl
 * extended by @author stefanpedratscher
 */
public class YAMLParser {

    final static Logger logger = LoggerFactory.getLogger(YAMLParser.class);

    /**
     * Parses a given YAML file to a workflow, which can be executed.
     *
     * @param filename yaml file to parse
     * @return Instance of class Executable workflow.
     */
    public ExecutableWorkflow parseExecutableWorkflow(String filename) {
        ExecutableWorkflow executableWorkflow = null;

        // Parse yaml file
        com.dps.afcl.Workflow workflow = Utils.readYAMLNoValidation(filename);

        if (workflow != null) {
            NodeListHelper nodeListHelper = new NodeListHelper();

            // Create node pairs from workflow functions
            ListPair<Node, Node> workflowPair = new ListPair<Node, Node>();
            ListPair<Node, Node> startNode = nodeListHelper.toNodeList(workflow.getWorkflowBody().get(0));
            workflowPair.setStart(startNode.getStart());
            Node currentEnd = startNode.getEnd();
            for (int i = 1; i < workflow.getWorkflowBody().size(); i++) {
                ListPair<Node, Node> current = nodeListHelper.toNodeList(workflow.getWorkflowBody().get(i));
                currentEnd.addChild(current.getStart());
                current.getStart().addParent(currentEnd);
                currentEnd = current.getEnd();
            }
            workflowPair.setEnd(currentEnd);

            // Create executable workflow from node pairs
            executableWorkflow = new ExecutableWorkflow(workflow.getName(), workflowPair, workflow.getDataIns());

            logger.info("Workflow was converted to an executable workflow.");
        }

        return executableWorkflow;
    }
}