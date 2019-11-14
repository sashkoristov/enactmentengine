package at.enactmentengine.serverless.parser;

import at.enactmentengine.serverless.nodes.ExecutableWorkflow;
import at.enactmentengine.serverless.nodes.ListPair;
import at.enactmentengine.serverless.nodes.Node;
import com.dps.afcl.utils.Utils;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

/**
 * 
 * @author markusmoosbrugger, jakobnoeckl
 * extended by @author stefanpedratscher
 *
 *         Class for parsing YAML files into executable workflows.
 */
public class YAMLParser {

	final static Logger logger = LoggerFactory.getLogger(YAMLParser.class);

	/**
	 * Parses a given YAML file to an workflow which can be executed.
	 *
	 * @return Instance of class Executable workflow.
	 */
	public ExecutableWorkflow parseExecutableWorkflow(String filename) {
		ExecutableWorkflow executableWorkflow = null;

		com.dps.afcl.Workflow workflow = Utils.readYAMLNoValidation(filename);
		if (workflow != null) {
			NodeListHelper nodeListHelper = new NodeListHelper();

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

			executableWorkflow = new ExecutableWorkflow(workflow.getName(), workflowPair, workflow.getDataIns());

			logger.info("Workflow was converted into an executable workflow.");
		}

		return executableWorkflow;
	}
}