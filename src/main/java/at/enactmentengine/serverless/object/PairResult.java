package at.enactmentengine.serverless.object;

/**
 * Class used as a return value when simulating functions.
 *
 * @author mikahautz
 */
public class PairResult<Long, Double> {

    /**
     * The round trip time of the function.
     */
    private Long rtt;

    /**
     * The cost of the function.
     */
    private Double cost;

    /**
     * Constructs an instance of the PairResult.
     * @param rtt of the function
     * @param cost of the function
     */
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
