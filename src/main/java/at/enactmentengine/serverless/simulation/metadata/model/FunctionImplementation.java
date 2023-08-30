package at.enactmentengine.serverless.simulation.metadata.model;

import com.fasterxml.jackson.annotation.JsonAlias;

/**
 * This file was originally part of the bachelor thesis 'Tracing and Simulation Framework for AFCL'
 * supervised by Sasko Ristov, written by
 *
 * @author Philipp Gritsch
 * <p>
 * The file was moved and adapted to this project by Mika Hautz.
 */
public class FunctionImplementation extends AdditionalServiceType {
    @IdColumn(name = "id", clazz = Long.class)
    private Long id;

    @Column(name = "functionType_id", clazz = Long.class)
    @JsonAlias({"functionTypeId", "functionType_id"})
    private Long functionTypeId;

    @Column(name = "algorithm", clazz = String.class)
    private String algorithm;

    @Column(name = "provider", clazz = Long.class)
    @JsonAlias({"provider", "detailedProviderId"})
    private Long detailedProviderId;

    @Column(name = "implementationFilePath", clazz = String.class)
    private String implementationFilePath;

    @Column(name = "language_id", clazz = Long.class)
    @JsonAlias({"languageId", "language_id"})
    private Long languageId;

    @Column(name = "avgRTT", clazz = Double.class)
    private Double avgRTT;

    @Column(name = "avgCost", clazz = Double.class)
    private Double avgCost;

    @Column(name = "successRate", clazz = Double.class)
    private Double successRate;

    @Column(name = "computationWork", clazz = Double.class)
    private Double computationWork;

    @Column(name = "memoryWork", clazz = Double.class)
    private Double memoryWork;

    @Column(name = "ioWork", clazz = Double.class)
    private Double ioWork;

    @Column(name = "invocations", clazz = int.class)
    private int invocations;


    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public Long getDetailedProviderId() {
        return detailedProviderId;
    }

    public void setDetailedProviderId(Long detailedProviderId) {
        this.detailedProviderId = detailedProviderId;
    }

    public String getImplementationFilePath() {
        return implementationFilePath;
    }

    public void setImplementationFilePath(String implementationFilePath) {
        this.implementationFilePath = implementationFilePath;
    }

    public Long getLanguageId() {
        return languageId;
    }

    public void setLanguageId(Long languageId) {
        this.languageId = languageId;
    }

    public Double getAvgRTT() {
        return avgRTT;
    }

    public void setAvgRTT(Double avgRTT) {
        this.avgRTT = avgRTT;
    }

    public Double getAvgCost() {
        return avgCost;
    }

    public void setAvgCost(Double avgCost) {
        this.avgCost = avgCost;
    }

    public Double getSuccessRate() {
        return successRate;
    }

    public void setSuccessRate(Double successRate) {
        this.successRate = successRate;
    }

    public Double getComputationWork() {
        return computationWork;
    }

    public void setComputationWork(Double computationWork) {
        this.computationWork = computationWork;
    }

    public Double getMemoryWork() {
        return memoryWork;
    }

    public void setMemoryWork(Double memoryWork) {
        this.memoryWork = memoryWork;
    }

    public Double getIoWork() {
        return ioWork;
    }

    public void setIoWork(Double ioWork) {
        this.ioWork = ioWork;
    }

    public int getInvocations() {
        return invocations;
    }

    public void setInvocations(int invocations) {
        this.invocations = invocations;
    }

    public Long getFunctionTypeId() {
        return functionTypeId;
    }

    public void setFunctionTypeId(Long functionTypeId) {
        this.functionTypeId = functionTypeId;
    }
}
