package at.enactmentengine.serverless.object;

public class TripleResult<Long, String, Boolean> {

    private Long RTT;

    private String output;

    private Boolean success;

    public TripleResult(Long RTT, String output, Boolean success) {
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

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public Boolean isSuccess() { return success; }

    public void setSuccess(Boolean success) { this.success = success; }
}
