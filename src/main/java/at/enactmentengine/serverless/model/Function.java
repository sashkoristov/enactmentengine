package at.enactmentengine.serverless.model;

import java.util.List;

import at.enactmentengine.serverless.nodes.FunctionNodeOld;
import at.enactmentengine.serverless.nodes.ListPair;
import at.enactmentengine.serverless.nodes.Node;

/**
 * Model class for a function.
 * 
 * @author markusmoosbrugger, jakobnoeckl
 *
 */
public class Function extends WorkflowElement {
	private String name;
	private String type;
	private List<Property> properties;
	private List<Data> dataIns;
	private List<Data> dataOuts;

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

	public List<Property> getProperties() {
		return properties;
	}

	public void setProperties(List<Property> properties) {
		this.properties = properties;
	}

	public List<Data> getDataIns() {
		return dataIns;
	}

	public void setDataIns(List<Data> dataIns) {
		this.dataIns = dataIns;
	}

	public List<Data> getDataOuts() {
		return dataOuts;
	}

	public void setDataOuts(List<Data> dataOuts) {
		this.dataOuts = dataOuts;
	}

	public FunctionNodeOld toFunction() {

		return new FunctionNodeOld(name, type, properties, dataIns, dataOuts);
	}

	/**
	 * Creates a ListPair which includes a function node twice with a name, type,
	 * properties, data input and data outputs.
	 */
	@Override
	public ListPair<Node, Node> toNodeList() {
		FunctionNodeOld f = new FunctionNodeOld(name, type, properties, dataIns, dataOuts);
		return new ListPair<Node, Node>(f, f);
	}

}