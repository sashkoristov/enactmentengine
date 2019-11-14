package at.enactmentengine.serverless.model;

/**
 * Model class for loop counter.
 * 
 * @author markusmoosbrugger, jakobnoeckl
 *
 */
public class LoopCounter {

	private String name;
	private String type;
	private String from;
	private String to;
	private String step;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getFrom() {
		return from;
	}

	public void setFrom(String from) {
		this.from = from;
	}

	public String getTo() {
		return to;
	}

	public void setTo(String to) {
		this.to = to;
	}

	public String getStep() {
		return step;
	}

	public void setStep(String step) {
		this.step = step;
	}
}
