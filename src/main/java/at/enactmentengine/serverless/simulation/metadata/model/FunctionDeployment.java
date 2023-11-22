package at.enactmentengine.serverless.simulation.metadata.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This file was originally part of the bachelor thesis 'Tracing and Simulation Framework for AFCL'
 * supervised by Sasko Ristov, written by
 *
 * @author Philipp Gritsch
 * <p>
 * The file was moved and adapted to this project by Mika Hautz.
 */
public class FunctionDeployment extends AdditionalServiceType {
    @IdColumn(name = "id", clazz = Long.class)
    private Long id;

    @Column(name = "functionImplementation_id", clazz = Long.class)
    @JsonAlias({"functionImplementation_id"})
    private Long functionImplementationId;

    @Column(name = "regionID", clazz = Long.class)
    @JsonAlias({"regionId", "regionID"})
    private Long regionId;

    @Column(name = "description", clazz = String.class)
    private String description;

    @Column(name = "handlerName", clazz = String.class)
    private String handlerName;

    @Column(name = "memorySize", clazz = int.class)
    private int memorySize;

    @Column(name = "timeout", clazz = int.class)
    private int timeout;

    @Column(name = "input", clazz = String.class)
    private String input;

    @Column(name = "KMS_Arn", clazz = String.class)
    @JsonAlias({"kmsArn", "KMS_Arn"})
    private String kmsArn;

    @Column(name = "isDeployed", clazz = boolean.class)
    @JsonProperty("isDeployed")
    private boolean isDeployed;

    @Column(name = "avgRTT", clazz = Double.class)
    private Double avgRTT;

    @Column(name = "avgRuntime", clazz = Double.class)
    private Double avgRuntime;

    @Column(name = "avgCost", clazz = Double.class)
    private Double avgCost;

    @Column(name = "successRate", clazz = Double.class)
    private Double successRate;

    @Column(name = "computationalSpeed", clazz = Double.class)
    private Double computationalSpeed;

    @Column(name = "memorySpeed", clazz = Double.class)
    private Double memorySpeed;

    @Column(name = "ioSpeed", clazz = Double.class)
    private Double ioSpeed;

    @Column(name = "invocations", clazz = int.class)
    private int invocations;

    private Integer speedup;

    private Integer avgLoopCounter;

    private String namespace;

    private String project;

    private Double avgDeploymentTime;

    private Double estDeploymentTime;

    @JsonProperty("RTTModel")
    private Double RTTModel;

    @JsonProperty("ETModel")
    private Double ETModel;

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public Integer getAvgLoopCounter() {
        return avgLoopCounter;
    }

    public void setAvgLoopCounter(Integer avgLoopCounter) {
        this.avgLoopCounter = avgLoopCounter;
    }

    public Integer getSpeedup() {
        return speedup;
    }

    public void setSpeedup(Integer speedup) {
        this.speedup = speedup;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    @SuppressWarnings("unused")
    public FunctionDeployment() {

    }

    public FunctionDeployment(final FunctionDeployment original) {
        this.setId(original.getRegionId());
        this.setFunctionImplementationId(original.getFunctionImplementationId());
        this.setRegionId(original.getRegionId());
        this.setDescription(original.getDescription());
        this.setHandlerName(original.getHandlerName());
        this.setMemorySize(original.getMemorySize());
        this.setTimeout(original.getTimeout());
        this.setInput(original.getInput());
        this.setKmsArn(original.getKmsArn());
        this.setDeployed(original.isDeployed());
        this.setAvgRTT(original.getAvgRTT());
        this.setAvgRuntime(original.getAvgRuntime());
        this.setAvgCost(original.getAvgCost());
        this.setSuccessRate(original.getSuccessRate());
        this.setComputationalSpeed(original.getComputationalSpeed());
        this.setMemorySpeed(original.getMemorySpeed());
        this.setIoSpeed(original.getIoSpeed());
        this.setInvocations(original.getInvocations());
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public Long getFunctionImplementationId() {
        return functionImplementationId;
    }

    public void setFunctionImplementationId(Long functionImplementationId) {
        this.functionImplementationId = functionImplementationId;
    }

    public Long getRegionId() {
        return regionId;
    }

    public void setRegionId(Long regionId) {
        this.regionId = regionId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getHandlerName() {
        return handlerName;
    }

    public void setHandlerName(String handlerName) {
        this.handlerName = handlerName;
    }

    public int getMemorySize() {
        return memorySize;
    }

    public void setMemorySize(int memorySize) {
        this.memorySize = memorySize;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }

    public String getKmsArn() {
        return kmsArn;
    }

    public void setKmsArn(String kmsArn) {
        this.kmsArn = kmsArn;
    }

    public boolean isDeployed() {
        return isDeployed;
    }

    public void setDeployed(boolean deployed) {
        isDeployed = deployed;
    }

    public Double getAvgRTT() {
        return avgRTT;
    }

    public void setAvgRTT(Double avgRTT) {
        this.avgRTT = avgRTT;
    }

    public Double getAvgRuntime() {
        return avgRuntime == null ? 0 : avgRuntime;
    }

    public void setAvgRuntime(Double avgRuntime) {
        this.avgRuntime = avgRuntime;
    }

    public Double getAvgCost() {
        return avgCost == null ? 0 : avgCost;
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

    public Double getComputationalSpeed() {
        return computationalSpeed;
    }

    public void setComputationalSpeed(Double computationalSpeed) {
        this.computationalSpeed = computationalSpeed;
    }

    public Double getMemorySpeed() {
        return memorySpeed;
    }

    public void setMemorySpeed(Double memorySpeed) {
        this.memorySpeed = memorySpeed;
    }

    public Double getIoSpeed() {
        return ioSpeed;
    }

    public void setIoSpeed(Double ioSpeed) {
        this.ioSpeed = ioSpeed;
    }

    public int getInvocations() {
        return invocations;
    }

    public void setInvocations(int invocations) {
        this.invocations = invocations;
    }

    public Double getAvgDeploymentTime() {
        return avgDeploymentTime;
    }

    public void setAvgDeploymentTime(Double avgDeploymentTime) {
        this.avgDeploymentTime = avgDeploymentTime;
    }

    public Double getEstDeploymentTime() {
        return estDeploymentTime;
    }

    public void setEstDeploymentTime(Double estDeploymentTime) {
        this.estDeploymentTime = estDeploymentTime;
    }

    public Double getRTTModel() {
        return RTTModel;
    }

    public void setRTTModel(Double RTTModel) {
        this.RTTModel = RTTModel;
    }

    public Double getETModel() {
        return ETModel;
    }

    public void setETModel(Double ETModel) {
        this.ETModel = ETModel;
    }
}
