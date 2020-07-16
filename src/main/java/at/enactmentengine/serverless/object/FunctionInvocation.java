package at.enactmentengine.serverless.object;

/**
 * Entry for a function invocation.
 */
public class FunctionInvocation {

    private String functionLink;
    private String provider;
    private String region;
    private String invokeTime;
    private String returnTime;
    private long executionTime;
    private String status;
    private String errorMessage;

    /**
     * Default constructor for a function invocation.
     *
     * @param functionLink link of the function (identifier).
     * @param provider on which the function runs.
     * @param region on which the function runs.
     * @param invokeTime of the function.
     * @param returnTime of the function.
     * @param executionTime of the function.
     * @param status of the function (OK or ERROR).
     * @param errorMessage of if failure.
     */
    public FunctionInvocation(String functionLink, String provider, String region, String invokeTime, String returnTime,
                              long executionTime, String status, String errorMessage){
        this.functionLink = functionLink;
        this.provider = provider;
        this.region = region;
        this.invokeTime = invokeTime;
        this.returnTime = returnTime;
        this.executionTime = executionTime;
        this.status = status;
        this.errorMessage = errorMessage;
    }


    /**
     * Getter and Setter.
     */
    public String getFunctionLink() {
        return functionLink;
    }

    public void setFunctionLink(String functionLink) {
        this.functionLink = functionLink;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getInvokeTime() {
        return invokeTime;
    }

    public void setInvokeTime(String invokeTime) {
        this.invokeTime = invokeTime;
    }

    public String getReturnTime() {
        return returnTime;
    }

    public void setReturnTime(String returnTime) {
        this.returnTime = returnTime;
    }

    public long getExecutionTime() {
        return executionTime;
    }

    public void setExecutionTime(long executionTime) {
        this.executionTime = executionTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
