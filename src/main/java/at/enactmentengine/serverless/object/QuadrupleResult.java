package at.enactmentengine.serverless.object;

import java.util.Map;

/**
 * Class used as a return value when simulating functions.
 *
 * @author mikahautz
 */
public class QuadrupleResult<Long, Double, JsonObject, Boolean> {

    /**
     * The round trip time of the function.
     */
    private Long rtt;

    /**
     * The cost of the function.
     */
    private Double cost;

    /**
     * The return value of the function.
     */
    private Map<String, Object> output;

    /**
     * If simulating the function is successful or not.
     */
    private Boolean success;

    /**
     * Constructs an instance of the QuadrupleResult.
     *
     * @param rtt     of the function
     * @param cost    of the function
     * @param output  of the function
     * @param success of the function
     */
    public QuadrupleResult(Long rtt, Double cost, Map<String, Object> output, Boolean success) {
        this.rtt = rtt;
        this.cost = cost;
        this.output = output;
        this.success = success;
    }

    public Long getRTT() {
        return rtt;
    }

    public void setRTT(Long rtt) {
        this.rtt = rtt;
    }

    public Double getCost() {
        return cost;
    }

    public void setCost(Double cost) {
        this.cost = cost;
    }

    public Map<String, Object> getOutput() {
        return output;
    }

    public void setOutput(Map<String, Object> output) {
        this.output = output;
    }

    public Boolean isSuccess() { return success; }

    public void setSuccess(Boolean success) { this.success = success; }
}
