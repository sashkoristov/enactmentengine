package at.enactmentengine.serverless.object;

public class PairResult<Long, String> {

    private Long RTT;

    private String output;

    public PairResult(Long RTT, String output) {
        this.RTT = RTT;
        this.output = output;
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
}
