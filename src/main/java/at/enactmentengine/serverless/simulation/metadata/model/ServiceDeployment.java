package at.enactmentengine.serverless.simulation.metadata.model;

import java.io.Serializable;

public class ServiceDeployment implements Entity<Integer>, Serializable {
    @IdColumn(name = "id", clazz = Long.class)
    private Integer id;

    @Column(name = "serviceID", clazz = Integer.class)
    private Integer serviceID;

    @Column(name = "regionID", clazz = Integer.class)
    private Integer regionID;

    @Column(name = "velocity", clazz = Double.class)
    private Double velocity;

    @Column(name = "startup", clazz = Double.class)
    private Double startup;

    @Override
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getServiceID() {
        return serviceID;
    }

    public void setServiceID(Integer serviceID) {
        this.serviceID = serviceID;
    }

    public Integer getRegionID() {
        return regionID;
    }

    public void setRegionID(Integer regionID) {
        this.regionID = regionID;
    }

    public Double getVelocity() {
        return velocity;
    }

    public void setVelocity(Double velocity) {
        this.velocity = velocity;
    }

    public Double getStartup() {
        return startup;
    }

    public void setStartup(Double startup) {
        this.startup = startup;
    }
}
