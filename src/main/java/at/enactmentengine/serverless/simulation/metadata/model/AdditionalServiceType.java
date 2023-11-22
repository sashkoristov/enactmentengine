package at.enactmentengine.serverless.simulation.metadata.model;

import java.io.Serializable;

/**
 * This file was originally part of the bachelor thesis 'Tracing and Simulation Framework for AFCL'
 * supervised by Sasko Ristov, written by
 *
 * @author Philipp Gritsch
 * <p>
 * The file was moved to this project by Mika Hautz.
 */
public class AdditionalServiceType implements Entity<Long>, Serializable {

    @Column(name = "id", clazz = Long.class)
    private Long id;

    @Column(name = "name", clazz = String.class)
    private String name;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}