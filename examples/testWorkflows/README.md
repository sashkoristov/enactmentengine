# Example Workflows

In this directory some examples of workflows are provided to easily understand workflows and test them with the engine.

## Contents

Short overview of provided workflows

### FunctionToFunction
Simple Workflow with three sequential functions

### FunctionToIf
Function followed by a if-construct

### FunctionToIfToFunction
Function followed by a if-construct and a function

### FunctionToParallelForToFunction
Workflow containing a function, followed by a parallelFor construct and another function

### FunctionToParallelToFunction
Workflow containing two functions and a parallel section

## Example structure

Each directory contains one specific example of a workflow. You can find in each directory:

- *.yaml: file which specifies the workflow
- *.py: code for all necessary Lambda functions
- input.json: input file for the workflow

> You need to fill in the ARN of each function (properties -> value) in the .yaml-file.