package at.enactmentengine.serverless.object;

import at.uibk.dps.afcl.functions.objects.PropertyConstraint;

import java.util.List;

public class FunctionAttributes {
    private String awsName;
    private List<PropertyConstraint> constraints;
    private int parallelCounter;

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
    public List<PropertyConstraint> getConstraints() {
        return constraints;
    }
    public void setConstraints(List<PropertyConstraint> constraints) {
        this.constraints = constraints;
    }
    public String getAwsName() {
        return awsName;
    }
    public void setAwsName(String awsName) {
        this.awsName = awsName;
    }
}
