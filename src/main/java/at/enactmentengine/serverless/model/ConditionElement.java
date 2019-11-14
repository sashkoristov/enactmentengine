package at.enactmentengine.serverless.model;

/**
 * Model class for a single condition element.
 * 
 * @author markusmoosbrugger, jakobnoeckl
 *
 */
public class ConditionElement {

	private String data1;
	private String data2;
	private String operator;

	public String getData1() {
		return data1;
	}

	public void setData1(String data1) {
		this.data1 = data1;
	}

	public String getData2() {
		return data2;
	}

	public void setData2(String data2) {
		this.data2 = data2;
	}

	public String getOperator() {
		return operator;
	}

	public void setOperator(String operator) {
		this.operator = operator;
	}
}
