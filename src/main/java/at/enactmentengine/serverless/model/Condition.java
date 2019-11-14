package at.enactmentengine.serverless.model;

import java.util.List;

/**
 * Model class for a condition.
 * 
 * @author markusmoosbrugger, jakobnoeckl
 *
 */
public class Condition {
	private String combinedWith;
	private List<ConditionElement> conditions;

	public String getCombinedWith() {
		return combinedWith;
	}

	public void setCombinedWith(String combinedWith) {
		this.combinedWith = combinedWith;
	}

	public List<ConditionElement> getConditions() {
		return conditions;
	}

	public void setConditions(List<ConditionElement> conditions) {
		this.conditions = conditions;
	}
}
