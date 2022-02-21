# Example Workflows

In this directory some examples of workflows are provided to easily understand workflows and test them with the engine.

## Contents

Short overview of provided workflows

### Examples for Existing Version

#### FunctionToFunction
Simple Workflow with three sequential functions

#### FunctionToIf
Function followed by a if-construct

#### FunctionToIfToFunction
Function followed by a if-construct and a function

#### FunctionToParallelForToFunction
Workflow containing a function, followed by a parallelFor construct and another function

#### FunctionToParallelToFunction
Workflow containing two functions and a parallel section

### Examples with new Features

#### FunctionToFunction
With this example the new data-flow is shown. It is now possible to just use a dataout from f1 in f3, without passing it through f2.

#### IfCombinedSource
Simple if-construct where at the dataouts of a multiple source is defined.

``` json
    dataOuts:
    - name: "ifout1"
      type: "number"
      source: "[f1/f1out1, f2/f2out1]"
```

In this case the engine takes one of the two defined datasources. Especially, here would only be one datasource available and the other one undefined, because the if-construct only executes one branch.

#### IfCombinedSourceFunction
In this case we see, that we do not have to define the dataouts of a if-construct, but can simply define the datains with a combined source in the next function.

``` json
    - name: "f3input2"
      type: "number"
      source: "[f1/f1out1, f2/f2out1]"
```

#### IfCombinedSourceParallel
This example is similar to 'IfCombinedSourceFunction', but here we define the datain of a parallel with a combined source.

#### IfCombinedSourceParallelFor
Similar example as 'IfCombinedSourceParallel', but for the parallelFor-construct.

## Example structure

You will find one directory for workflows which are working with the old version of the engine and one directory with example using new features of the engine.

Each directory contains one specific example of a workflow. You can find in each directory:

- *.yaml: file which specifies the workflow
- *.py: code for all necessary Lambda functions
- input.json: input file for the workflow

> You need to fill in the ARN of each function (properties -> value) in the .yaml-file.