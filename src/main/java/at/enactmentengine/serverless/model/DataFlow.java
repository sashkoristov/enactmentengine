package at.enactmentengine.serverless.model;

/**
 * Model class for data flow.
 * 
 * @author markusmoosbrugger, jakobnoeckl
 *
 */
public class DataFlow {

	private String type;
	private Size size;

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Size getSize() {
		return size;
	}

	public void setSize(Size size) {
		this.size = size;
	}
}
