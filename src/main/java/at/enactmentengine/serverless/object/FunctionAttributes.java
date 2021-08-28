package at.enactmentengine.serverless.object;


import at.uibk.dps.afcl.functions.objects.PropertyConstraint;
import at.uibk.dps.function.FaultToleranceSettings;
import at.uibk.dps.function.Function;

import java.util.List;

/**
 * class for the evaluation in async handler
 * this is specified for the aws use case
 */
public class FunctionAttributes {
    private String name;
    private String awsName;
    private Function function;
    private int parallelCounter;
    private int functionInput;
    private boolean isAsync;

    public void increaseCounter(){
        this.parallelCounter++;
    }
    public void decreaseCounter(){
        this.parallelCounter--;
    }

    public FunctionAttributes(){
        this.parallelCounter = 0;
    }
    //getter, setter -----------------------------------------------------------------------
    public int getParallelCounter() {
        return parallelCounter;
    }
    public Function getFunction() {
        return function;
    }
    public void setFunction(Function function) {
        this.function = function;
    }
    public String getAwsName() {
        return awsName;
    }
    public void setAwsName(String awsName) {
        this.awsName = awsName;
    }
    public int getFunctionInput() {
        return functionInput;
    }
    public void setFunctionInput(int functionInput) {
        this.functionInput = functionInput;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public void setAsync(boolean async) {
        isAsync = async;
    }
    public boolean isAsync() {
        return isAsync;
    }
}
