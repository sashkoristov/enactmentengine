package at.enactmentengine.serverless.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Model class for a data object.
 * 
 * @author markusmoosbrugger, jakobnoeckl
 * 
 *
 */
public class Data {
	private String name;
	private String source;
	private String type;
	private boolean passing = false;
	private DataFlow dataFlow;

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public DataFlow getDataFlow() {
		return dataFlow;
	}

	public void setDataFlow(DataFlow dataFlow) {
		this.dataFlow = dataFlow;
	}

	public boolean isPassing() {
		return passing;
	}

	public void setPassing(boolean passing) {
		this.passing = passing;
	}
}