package at.enactmentengine.serverless.object;

public class PairResult<Long, Double> {

    private Long rtt;

    private Double cost;

    public PairResult(Long rtt, Double cost) {
        this.rtt = rtt;
        this.cost = cost;
    }

    public Long getRtt() {
        return rtt;
    }

    public void setRtt(Long rtt) {
        this.rtt = rtt;
    }

    public Double getCost() {
        return cost;
    }

    public void setCost(Double cost) {
        this.cost = cost;
    }
}
