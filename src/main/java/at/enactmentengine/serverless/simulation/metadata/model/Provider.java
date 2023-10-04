package at.enactmentengine.serverless.simulation.metadata.model;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.io.Serializable;

/**
 * This file was originally part of the bachelor thesis 'Tracing and Simulation Framework for AFCL'
 * supervised by Sasko Ristov, written by
 *
 * @author Philipp Gritsch
 * <p>
 * The file was moved to this project by Mika Hautz.
 */
public class Provider implements Entity<Integer>, Serializable {
    @IdColumn(name = "id", clazz = Integer.class)
    private Integer id;

    @Column(name = "name", clazz = String.class)
    private String name;

    @Column(name = "invocationCost", clazz = Double.class)
    private Double invocationCost;

    @Column(name = "durationGBpsCost", clazz = Double.class)
    private Double durationGBpsCost;

    @Column(name = "durationGHzpsCost", clazz = Double.class)
    private Double durationGHzpsCost;

    @Column(name = "unitTimems", clazz = Integer.class)
    private Integer unitTimems;

    @Column(name = "maxConcurrency", clazz = Integer.class)
    private Integer maxConcurrency;

    @Column(name = "maxThroughput", clazz = Integer.class)
    private Integer maxThroughput;

    @Column(name = "maxDurationSec", clazz = Integer.class)
    private Integer maxDurationSec;

    @Column(name = "maxDataInputMB", clazz = Integer.class)
    private Integer maxDataInputMB;

    @Column(name = "maxDataOutputMB", clazz = Integer.class)
    private Integer maxDataOutputMB;

    @Column(name = "concurrencyOverheadMs", clazz = Integer.class)
    @JsonAlias({"concurrencyOverheadMs", "concurrencyOverheadms"})
    private Integer concurrencyOverheadMs;

    @Column(name = "faasSystemOverheadms", clazz = Integer.class)
    private Integer faasSystemOverheadms;

    private Integer cryptoOverheadms;

    private Integer deploymentOverheadms;

    private Integer roundTrips;

    private Integer sessionOverheadms;

    private Integer r;

    public Integer getCryptoOverheadms() {
        return cryptoOverheadms;
    }

    public void setCryptoOverheadms(Integer cryptoOverheadms) {
        this.cryptoOverheadms = cryptoOverheadms;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getInvocationCost() {
        return invocationCost;
    }

    public void setInvocationCost(Double invocationCost) {
        this.invocationCost = invocationCost;
    }

    public Double getDurationGBpsCost() {
        return durationGBpsCost;
    }

    public void setDurationGBpsCost(Double durationGBpsCost) {
        this.durationGBpsCost = durationGBpsCost;
    }

    public Double getDurationGHzpsCost() {
        return durationGHzpsCost;
    }

    public void setDurationGHzpsCost(Double durationGHzpsCost) {
        this.durationGHzpsCost = durationGHzpsCost;
    }

    public Integer getUnitTimems() {
        return unitTimems;
    }

    public void setUnitTimems(Integer unitTimems) {
        this.unitTimems = unitTimems;
    }

    public Integer getMaxConcurrency() {
        return maxConcurrency;
    }

    public void setMaxConcurrency(Integer maxConcurrency) {
        this.maxConcurrency = maxConcurrency;
    }

    public Integer getMaxThroughput() {
        return maxThroughput;
    }

    public void setMaxThroughput(Integer maxThroughput) {
        this.maxThroughput = maxThroughput;
    }

    public Integer getMaxDurationSec() {
        return maxDurationSec;
    }

    public void setMaxDurationSec(Integer maxDurationSec) {
        this.maxDurationSec = maxDurationSec;
    }

    public Integer getMaxDataInputMB() {
        return maxDataInputMB;
    }

    public void setMaxDataInputMB(Integer maxDataInputMB) {
        this.maxDataInputMB = maxDataInputMB;
    }

    public Integer getMaxDataOutputMB() {
        return maxDataOutputMB;
    }

    public void setMaxDataOutputMB(Integer maxDataOutputMB) {
        this.maxDataOutputMB = maxDataOutputMB;
    }

    public Integer getConcurrencyOverheadMs() {
        return concurrencyOverheadMs == null ? 0 : concurrencyOverheadMs;
    }

    public void setConcurrencyOverheadMs(Integer concurrencyOverheadMs) {
        this.concurrencyOverheadMs = concurrencyOverheadMs;
    }

    public Integer getFaasSystemOverheadms() {
        return faasSystemOverheadms;
    }

    public void setFaasSystemOverheadms(Integer faasSystemOverheadms) {
        this.faasSystemOverheadms = faasSystemOverheadms;
    }

    public Integer getDeploymentOverheadms() {
        return deploymentOverheadms;
    }

    public void setDeploymentOverheadms(Integer deploymentOverheadms) {
        this.deploymentOverheadms = deploymentOverheadms;
    }

    public Integer getRoundTrips() {
        return roundTrips;
    }

    public void setRoundTrips(Integer roundTrips) {
        this.roundTrips = roundTrips;
    }

    public Integer getSessionOverheadms() {
        return sessionOverheadms;
    }

    public void setSessionOverheadms(Integer sessionOverheadms) {
        this.sessionOverheadms = sessionOverheadms;
    }

    public Integer getR() {
        return r;
    }

    public void setR(Integer r) {
        this.r = r;
    }
}
