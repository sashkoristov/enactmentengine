package at.enactmentengine.serverless.parser;

import at.enactmentengine.serverless.model.*;
import at.enactmentengine.serverless.nodes.ExecutableWorkflow;
import at.enactmentengine.serverless.nodes.ExecutableWorkflowOld;
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

	/**
	 * Parses a given YAML file to an workflow which can be executed.
	 * 
	 * @param file The YAML file.
	 * @return Instance of class Executable workflow.
	 */
	public ExecutableWorkflowOld parseExecutableWorkflowOld(InputStream file) {
		ExecutableWorkflowOld executableWorkflowOld = null;
		at.enactmentengine.serverless.model.Workflow workflow = parseYAMLFile(file);
		if (workflow != null) {
			executableWorkflowOld = workflow.toExecutableWorkflow();
			logger.info("Workflow was converted into an executable workflow.");
		}

		return executableWorkflowOld;
	}

	/**
	 * Parses a given YAML file into a non executable workflow.
	 * 
	 * @param file the YAML file.
	 * @return Instance of class workflow.
	 */
	public at.enactmentengine.serverless.model.Workflow parseYAMLFile(InputStream file) {
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
		at.enactmentengine.serverless.model.Workflow workflow = null;
		try {
			workflow = mapper.readValue(file, at.enactmentengine.serverless.model.Workflow.class);
			file.close();
			workflow.setWorkflowBodyParsed(parseBody(workflow.getWorkflowBody(), mapper));
			logger.info("Workflow was parsed without errors.");
			logger.info(ReflectionToStringBuilder.toString(workflow, ToStringStyle.MULTI_LINE_STYLE));

		} catch (FileNotFoundException e) {
			logger.error(e.getMessage(), e);
		} catch (JsonParseException e) {
			logger.error(e.getMessage(), e);
		} catch (JsonMappingException e) {
			logger.error(e.getMessage(), e);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
		return workflow;
	}

	/**
	 * Parses the body of a workflow recursively.
	 * 
	 * @param objects The object of the workflow that should be parsed.
	 * @param mapper  The object mapper.
	 * @return A list of workflow elements that represent the workflow body.
	 */
	@SuppressWarnings("unchecked")
	private List<WorkflowElement> parseBody(List<Object> objects, ObjectMapper mapper) {
		List<WorkflowElement> workflowElements = new ArrayList<WorkflowElement>();
		for (Object object : objects) {
			LinkedHashMap<String, String> linkedHashMap = (LinkedHashMap<String, String>) object;
			Entry<String, String> workflowElement = linkedHashMap.entrySet().iterator().next();

			switch (BodyTypes.fromString(workflowElement.getKey())) {
			case PARALLEL:
				WorkflowElementParallel parallel = mapper.convertValue(workflowElement.getValue(),
						WorkflowElementParallel.class);
				for (WorkflowElementSection section : parallel.getParallelBody()) {
					section.setSectionParsed(parseBody(section.getSection(), mapper));
				}
				workflowElements.add(parallel);
				break;
			case FUNCTION:
				at.enactmentengine.serverless.model.Function function = mapper.convertValue(workflowElement.getValue(), at.enactmentengine.serverless.model.Function.class);
				workflowElements.add(function);
				break;
			case PARALLELFOR:
				WorkflowElementParallelFor parallelFor = mapper.convertValue(workflowElement.getValue(),
						WorkflowElementParallelFor.class);
				parallelFor.setLoopBodyParsed(parseBody(parallelFor.getLoopBody(), mapper));
				workflowElements.add(parallelFor);
				break;
			case IF:
				WorkflowElementIf ifElement = mapper.convertValue(workflowElement.getValue(), WorkflowElementIf.class);

				workflowElements.add(ifElement);
				break;
			case SWITCH:
				WorkflowElementSwitch switchElement = mapper.convertValue(workflowElement.getValue(),
						WorkflowElementSwitch.class);
				for (Case switchCase : switchElement.getCases()) {
					switchCase.setParsedFunctions(parseBody(switchCase.getFunctions(), mapper));
				}

				if (switchElement.getWorkflowElementDefault() != null) {
					switchElement.setWorkflowElementDefaultParsed(
							parseBody(switchElement.getWorkflowElementDefault(), mapper));

				}
				workflowElements.add(switchElement);

				break;
			default:
				logger.info("No workflow element type match for " + workflowElement);
			}

		}
		return workflowElements;
	}
}