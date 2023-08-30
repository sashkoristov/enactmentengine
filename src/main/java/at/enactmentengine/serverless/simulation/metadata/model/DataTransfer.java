package at.enactmentengine.serverless.simulation.metadata.model;

import java.io.Serializable;

public class DataTransfer  implements Entity<Integer>, Serializable {
    @IdColumn(name = "id", clazz = Integer.class)
    private Integer id;

    @Column(name = "type", clazz = String.class)
    private String type;

    @Column(name = "functionRegionID", clazz = Integer.class)
    private Integer functionRegionID;

    @Column(name = "storageRegionID", clazz = Integer.class)
    private Integer storageRegionID;

    @Column(name = "bandwidth", clazz = Double.class)
    private Double bandwidth;

    @Column(name = "latency", clazz = Double.class)
    private Double latency;

    @Override
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getFunctionRegionID() {
        return functionRegionID;
    }

    public void setFunctionRegionID(Integer functionRegionID) {
        this.functionRegionID = functionRegionID;
    }

    public Integer getStorageRegionID() {
        return storageRegionID;
    }

    public void setStorageRegionID(Integer storageRegionID) {
        this.storageRegionID = storageRegionID;
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
}
