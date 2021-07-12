package at.enactmentengine.serverless.object;

import java.util.Map;

public class QuadrupleResult<Long, Double, JsonObject, Boolean> {

    private Long rtt;

    private Double cost;

    private Map<String, Object> output;

    private Boolean success;

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
