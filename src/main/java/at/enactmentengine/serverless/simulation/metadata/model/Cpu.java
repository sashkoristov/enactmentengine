package at.enactmentengine.serverless.simulation.metadata.model;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.io.Serializable;

public class Cpu implements Entity<Integer>, Serializable {
    @IdColumn(name = "id", clazz = Integer.class)
    private Integer id;

    @Column(name = "provider", clazz = Long.class)
    private Long provider;

    @Column(name = "region", clazz = Long.class)
    private Long region;

    @Column(name = "name", clazz = String.class)
    private String name;

    @Column(name = "parallel", clazz = Integer.class)
    private Integer parallel;

    @Column(name = "MIPS", clazz = Double.class)
    private Double mips;

    @Column(name = "from_percentage", clazz = Integer.class)
    private Integer from_percentage;

    @Column(name = "to_percentage", clazz = Integer.class)
    private Integer to_percentage;

    @Override
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Long getProvider() {
        return provider;
    }

    public void setProvider(Long provider) {
        this.provider = provider;
    }

    public Long getRegion() {
        return region;
    }

    public void setRegion(Long region) {
        this.region = region;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getParallel() {
        return parallel;
    }

    public void setParallel(Integer parallel) {
        this.parallel = parallel;
    }

    public Double getMips() {
        return mips;
    }

    public void setMips(Double mips) {
        this.mips = mips;
    }

    public Integer getFrom_percentage() {
        return from_percentage;
    }

    public void setFrom_percentage(Integer from_percentage) {
        this.from_percentage = from_percentage;
    }

    public Integer getTo_percentage() {
        return to_percentage;
    }

    public void setTo_percentage(Integer to_percentage) {
        this.to_percentage = to_percentage;
    }
}
