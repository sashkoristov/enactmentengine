package at.enactmentengine.serverless.model;

import java.util.List;

/**
 * Model class for a switch case.
 * 
 * @author markusmoosbrugger, jakobnoeckl
 *
 */
public class Case {
	private Object value;
	private List<Object> functions;
	private List<WorkflowElement> parsedFunctions;

	public Object getValue() {
		return value;
	}

	public void setValue(Object value) {
		this.value = value;
	}

	public List<Object> getFunctions() {
		return functions;
	}

	public void setFunctions(List<Object> functions) {
		this.functions = functions;
	}

	public List<WorkflowElement> getParsedFunctions() {
		return parsedFunctions;
	}

	public void setParsedFunctions(List<WorkflowElement> parsedFunctions) {
		this.parsedFunctions = parsedFunctions;
	}
}
