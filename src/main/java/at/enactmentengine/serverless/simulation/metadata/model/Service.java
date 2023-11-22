package at.enactmentengine.serverless.simulation.metadata.model;

import java.io.Serializable;

public class Service implements Entity<Integer>, Serializable {
    @IdColumn(name = "id", clazz = Integer.class)
    private Integer id;

    @Column(name = "type", clazz = String.class)
    private String type;

    @Column(name = "providerID", clazz = Integer.class)
    private Integer providerID;

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

    public Integer getProviderID() {
        return providerID;
    }

    public void setProviderID(Integer providerID) {
        this.providerID = providerID;
    }
}
