package at.enactmentengine.serverless.object;

import java.util.Map;

public class TripleResult<Long, JsonObject, Boolean> {

    private Long RTT;

    private Map<String, Object> output;

    private Boolean success;

    public TripleResult(Long RTT, Map<String, Object> output, Boolean success) {
        this.RTT = RTT;
        this.output = output;
        this.success = success;
    }

    public Long getRTT() {
        return RTT;
    }

    public void setRTT(Long RTT) {
        this.RTT = RTT;
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
