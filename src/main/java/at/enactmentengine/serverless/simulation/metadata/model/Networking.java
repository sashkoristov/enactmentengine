package at.enactmentengine.serverless.simulation.metadata.model;

import java.io.Serializable;

public class Networking implements Entity<Integer>, Serializable {
    @Column(name = "sourceRegionID", clazz = Integer.class)
    private Integer sourceRegionID;

    @Column(name = "destinationRegionID", clazz = Integer.class)
    private Integer destinationRegionID;

    @Column(name = "bandwidth", clazz = Double.class)
    private Double bandwidth;

    @Column(name = "latency", clazz = Double.class)
    private Double latency;

    public Integer getSourceRegionID() {
        return sourceRegionID;
    }

    public void setSourceRegionID(Integer sourceRegionID) {
        this.sourceRegionID = sourceRegionID;
    }

    public Integer getDestinationRegionID() {
        return destinationRegionID;
    }

    public void setDestinationRegionID(Integer destinationRegionID) {
        this.destinationRegionID = destinationRegionID;
    }

    public Double getBandwidth() {
        return bandwidth;
    }

    public void setBandwidth(Double bandwidth) {
        this.bandwidth = bandwidth;
    }

    public Double getLatency() {
        return latency;
    }

    public void setLatency(Double latency) {
        this.latency = latency;
    }

    @Override
    public Integer getId() {
        // not needed
        return null;
    }
}
