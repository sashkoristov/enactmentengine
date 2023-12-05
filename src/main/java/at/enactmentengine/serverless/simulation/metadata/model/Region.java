package at.enactmentengine.serverless.simulation.metadata.model;

import at.enactmentengine.serverless.simulation.metadata.model.enums.ProviderConverter;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
 * This file was originally part of the bachelor thesis 'Tracing and Simulation Framework for AFCL'
 * supervised by Sasko Ristov, written by
 *
 * @author Philipp Gritsch
 * <p>
 * The file was moved to this project by Mika Hautz.
 */
public class Region implements Entity<Integer>, Serializable {

    @IdColumn(name = "id", clazz = Integer.class)
    private Integer id;

    @Column(name = "region", clazz = String.class)
    private String region;

    @Column(name = "provider", clazz = at.uibk.dps.util.Provider.class, converter = ProviderConverter.class)
    private at.uibk.dps.util.Provider provider;

    @Column(name = "availability", clazz = Double.class)
    private Double availability;

    @Column(name = "providerID", clazz = Long.class)
    @JsonProperty("providerID")
    private Long providerID;

    @Column(name = "location", clazz = String.class)
    private String location;

    @Column(name = "networkOverheadms", clazz = Double.class)
    private Double networkOverheadms;

    @Column(name = "overheadLoadms", clazz = Double.class)
    private Double overheadLoadms;

    @Column(name = "overheadBurstms", clazz = Double.class)
    private Double overheadBurstms;

    @Column(name = "invocationDelayLoadms", clazz = Double.class)
    private Double invocationDelayLoadms;

    @Column(name = "invocationDelayBurstms", clazz = Double.class)
    private Double invocationDelayBurstms;

    @Column(name = "concurrencyOverheadms", clazz = Double.class)
    private Double concurrencyOverheadms;

    @Column(name = "faasSystemOverheadms", clazz = Double.class)
    private Double faasSystemOverheadms;

    private Double bandwidth;

    @Override
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public at.uibk.dps.util.Provider getProvider() {
        return provider;
    }

    public void setProvider(at.uibk.dps.util.Provider provider) {
        this.provider = provider;
    }

    public Double getAvailability() {
        return availability;
    }

    public void setAvailability(Double availability) {
        this.availability = availability;
    }

    public Long getProviderId() {
        return providerID;
    }

    public void setProviderId(Long providerID) {
        this.providerID = providerID;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Double getNetworkOverheadms() {
        return networkOverheadms;
    }

    public void setNetworkOverheadms(Double networkOverheadms) {
        this.networkOverheadms = networkOverheadms;
    }

    public Double getOverheadLoadms() {
        return overheadLoadms;
    }

    public void setOverheadLoadms(Double overheadLoadms) {
        this.overheadLoadms = overheadLoadms;
    }

    public Double getOverheadBurstms() {
        return overheadBurstms;
    }

    public void setOverheadBurstms(Double overheadBurstms) {
        this.overheadBurstms = overheadBurstms;
    }

    public Double getInvocationDelayLoadms() {
        return invocationDelayLoadms;
    }

    public void setInvocationDelayLoadms(Double invocationDelayLoadms) {
        this.invocationDelayLoadms = invocationDelayLoadms;
    }

    public Double getInvocationDelayBurstms() {
        return invocationDelayBurstms;
    }

    public void setInvocationDelayBurstms(Double invocationDelayBurstms) {
        this.invocationDelayBurstms = invocationDelayBurstms;
    }

    public Double getConcurrencyOverheadms() {
        return concurrencyOverheadms;
    }

    public void setConcurrencyOverheadms(Double concurrencyOverheadms) {
        this.concurrencyOverheadms = concurrencyOverheadms;
    }

    public Double getFaasSystemOverheadms() {
        return faasSystemOverheadms;
    }

    public void setFaasSystemOverheadms(Double faasSystemOverheadms) {
        this.faasSystemOverheadms = faasSystemOverheadms;
    }

    public Double getBandwidth() {
        return bandwidth;
    }

    public void setBandwidth(Double bandwidth) {
        this.bandwidth = bandwidth;
    }
}
