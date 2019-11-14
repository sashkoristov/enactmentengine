package at.enactmentengine.serverless.model;

import java.util.Arrays;

/**
 * 
 * Enum for allowed body types parallel, parallelFor, switch, if and function.
 * Other body types are not supported yet.
 * 
 * @author markusmoosbrugger, jakobnoeckl
 * 
 */
public enum BodyTypes {
	PARALLEL("parallel"), PARALLELFOR("parallelFor"), FUNCTION("function"), SWITCH("switch"), IF("if");

	private String value;

	BodyTypes(final String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	@Override
	public String toString() {
		return this.getValue();
	}

	/**
	 * Returns the enum value for a given string.
	 * 
	 * @return the Enum representation for the given string.
	 * @throws IllegalArgumentException if unknown string.
	 */
	public static BodyTypes fromString(String s) throws IllegalArgumentException {
		return Arrays.stream(BodyTypes.values()).filter(v -> v.value.equals(s)).findFirst()
				.orElseThrow(() -> new IllegalArgumentException("unknown value: " + s));
	}

}
