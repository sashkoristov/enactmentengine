# *xAFCL EE* - Portable and scalable enactment engine to run and simulate serverless workflows across multiple FaaS systems with fault tolerance

This project provides a portable and scalable middleware service *xAFCL Enactment Engine (xAFCL EE)* that can simultaneously execute individual functions of a serverless workflow application (Function Choreographies - *FCs*) across multiple FaaS systems (AWS Lambda, IBM Cloud Functions, Google Cloud Functions, Alibaba Function Compute, and Microsoft Functions). 

*xAFCL EE* is the core part of the overall [AFCL Environment], a platform to develop, deploy, and fault tolerant execution of FCs developed in our Abstract Function Choreography Language ([AFCL](https://doi.org/10.1016/j.future.2020.08.012)).

*xAFCL EE* integrates the component [FTjFaaS](https://github.com/sashkoristov/FTjFaaS) for optional fault tolerant execution of FC functions (supported all widely-known FaaS systems AWS Lambda, IBM Cloud Functions, Google Cloud Functions, and Microzoft Azure Functions).

*xAFCL EE* integrates the component [jFaaS](https://github.com/sashkoristov/jFaaS) for portable execution of individual FC functions (supported all widely-known FaaS systems AWS Lambda, IBM Cloud Functions, Google Cloud Functions, Microzoft Azure Functions, and Alibaba).


## File structure

- **[externalJars](externalJars)** contain pre-build jars used within the EE. Keep them upToDate.
- **[src/main](src/main)** 
    - **[/java](src/main/java)** contains the source code of the EE.

## Requirements

### credentials.properties

You will need to create a **credentials.properties** file in the root directory of the project, which contains the login credentials for the FaaS providers. This file will be ignored from git (**[.gitignore](.gitignore)**).

The file should look as follows:

````
aws_access_key_id=
aws_secret_access_key=
aws_session_token=              // (needed for AWS Academy)
ibm_api_key=
google_sa_key={\
 "type": "",\
 "project_id": "",\
 "private_key_id": "",\
 "private_key": "-----BEGIN PRIVATE KEY-----\\n ... \\n-----END PRIVATE KEY-----\\n",\
 "client_email": "",\
 "client_id": "",\
 "auth_uri": "",\
 "token_uri": "",\
 "auth_provider_x509_cert_url": "",\
 "client_x509_cert_url": ""\
}
azure_key=
````

### mongoDatabase.properties

You will need to create a **mongoDatabase.properties** file in the root directory of the project, which contains the login credentials for the execution log. This file will be ignored from git (**[.gitignore](.gitignore)**).

The file should look as follows:

````
host=
port=
database=
collection=
username=
password=
````

You need to have access to a MongoDB server, with created database and a collection.


## Deploy

### Local
<!--- Configure the line mainClassName = 'at.enactmentengine.serverless.main.Local' instead of mainClassName = 'at.enactmentengine.serverless.main.Service' to build a jar for local execution. --->

Simply run the [main method in Local.java](src/main/java/at/enactmentengine/serverless/main/Local.java) and pass the workflow yaml file as parameter, as well as the input JSON file. 
 
or 
 
````
gradle standalone
````

### Service
Run the [main method in Service.java](src/main/java/at/enactmentengine/serverless/main/Service.java). The Service will wait on port 9000 for a `.yaml` file and return the result of the execution in json format.

or

````
gradle shadowJar       
````
<!---
### Docker
````
gradle updateDocker         // to start the service in a docker container with a gradle task.
````

Alternatively, follow [/docker/README.md](docker/README.md) to run the container without gradle. --->
<!---
NOT SUPPORTED RIGHT NOW:

### AWS
1. Create an AWS Lambda function representing the EE (Upload the .jar)
   ````
   Runtime: Java8
   Handler: at.enactmentengine.serverless.main.LambdaHandler::handleRequest
   ````
2. Invoke the function with a specific input file:
    ````
    {
      "filename": "<your_workflow.yaml>",
      "language": "yaml"
    }
    ````
    or
    ````
    {
      "workflow": "<your_workflow_json_string>",
      "language": "json"
    }
    ````
    Use tools like https://www.json2yaml.com/ to convert from yaml to json and https://www.freeformatter.com/json-escape.html to escape characters.
    
### IBM
1. Create an IBM action representing the EE
    ````
    wsk action create EnactmentEngine <exported_ee.jar> --main at.enactmentengine.serverless.main.OpenWhiskHandler#main
    ````
2. Invoke the action with a specific input file:
    Input:
    ````
    wsk action invoke --result EnactmentEngine --param filename <your_workflow.yaml>
    ````
    or
    ````
    {
      "filename": "<your_workflow.yaml>",
      "language": "yaml"
    }
    ````
    or
    ````
    {
      "workflow": "<your_workflow_json_string>",
      "language": "json"
    }
    ````
    Use tools like https://www.json2yaml.com/ to convert from yaml to json and https://www.freeformatter.com/json-escape.html to escape characters.
--->


## Run an FC

In order to run an FC, you need to develop a proper FC.yaml file for the FC and the FC input.json file. You can use the [FCEditor](https://github.com/sashkoristov/FCEDitor) to develop your FC, which is currently deployed and publicly available on the following [link](http://fceditor.dps.uibk.ac.at:8180/).


````
java -jar enactment-engine-all.jar FC.yaml input.json
````

Examples of FCs yaml files can be found in **[examples/faultTolerance/](examples/faultTolerance/)**. 

All functions of the FC need to be deployed in order to be able to run them.

----

## Simulate an FC execution

When properly configured, *xAFCL* may also simulate execution of a function choreography across any of the top five cloud providers. You can use the same yaml file for simulation with a very few adaptations if you would like to simulate functions that were not even executed.

### mariaDatabase.properties (needed for simulation)

In order to use simulation, you will need to create a **mariaDatabase.properties** file in the root directory of the project, which contains the login credentials for the AFCL metadata database. This file will be ignored from git (**[.gitignore](.gitignore)**).

The file should look as follows:

````
host=
port=
database=
username=
password=
````

You need to have access to a MariaDB server, with created database and filled minimum required metadata entries for

- cloud providers
- cloud regions
- function types
- function implementations
- function deployments

An example of the metadata database schema, including configuration data from Innsbruck to multiple AWS, Google, and IBM cloud regions will be available soon.

Example workflows to run and simulate will be also availble soon.

### Usage

````
java -jar enactment-engine-all.jar FC.yaml input.json --simulate
````

### Simulate `parallelFor` loops and *siblings* and *twins* of functions

AFCL language offers to describe `parallelFor` loops with a dynamic loop iteration count which may be known during runtime. For instance, as an output of another predecessor function. In order to be able to simulate such FCs, a user may specify the loop iteration count in the field *simValue* for the parameter that is determined dynamically during runtime. See the following example:

````yaml
- function:
    name: "MonteCarlo"
    type: "MonteCarloType"
    deployment: "AWS_ap-southeast-1_128"
    dataOuts:
    - name: "counter"
      type: "number"
      properties:
      - name: "simValue" 
        value: 5 # specified value for simulation
    properties:
    - name: "resource"
      value: "arn:aws:lambda:ap-southeast-1:xxx:function:MC"
````

xAFCLSim can determine all siblings and twins of a function if you specify the provider, region, and assigned memory of a function, as it is shown in the field *deployment* in the above example. Use the following format *"\<providerName\>\_\<RegionCode\>\_\<memoryInMB\>"*. In the given example, the function MonteCarlo is deployed on AWS Tokyo with 128 MB.

Note: 

- *Siblings* of a function are all deployments in the same cloud region with different memory.
- *Twins* of a function are all deployments in another cloud region of the same cloud provider with the same memory

In order to be able to simulate a function, *xAFCLSim* uses the values from the MariaDB metadata with the following priority (from the highest to the lowest):

- execution log from the same function deployment
- execution log from some twin
- execution log from some siblings, applying the linear speedup by default compared to the sibling with 128 MB.



---------------

## Fault Tolerance

The *xAFCL EE* will automatically pass functions with fault tolerance and constraint settings to the fault tolerance module (`FTjFaaS`) for execution.

### Features

This section will show the features of the Fault Tolerance engine with some very simple examples. All examples are based on the following simple workflow (only the constraints field of a function needs to be changed within the CFCL file):

````yaml
---
name: "exampleWorkflow"
workflowBody:
- function:
    name: "hello"
    type: "helloType"
    properties:
    - name: "resource"
      value: "python:https://eu-gb.functions.cloud.ibm.com/<link.to.function>/hello.json"
    constraints:
    - name: "<FT-Name>"
      value: "<FT-Value>"
    dataOuts:
    - name: "message"
      type: "string"
dataOuts:
- name: "OutVal"
  type: "string"
  source: "hello/message"
````

- **FT-Retries** and **FT-AltStrat-requiredAvailability**

First we specify the number of times a function should be retried (`FT-Retries`) before the alternative strategy is executed. Additionally we can specify the required availability (`FT-AltStrat-requiredAvailability`) as a double value. `FT-Retries` is mandatory, while `FT-AltStrat-requiredAvailability` is optional when using the fault tolerant execution. 

````yaml
constraints:
- name: "FT-Retries"
  value: "2"
- name: "FT-AltStrat-requiredAvailability"
  value: "0.9"
````

The `FT-AltStrat-requiredAvailability` requires some additional steps to find the alternative plan which is understandable by *xAFCL EE*. For simplicity you can use the `addAlternativePlansToYAML()` method from [AlternativePlanScheduler](src/main/java/at/enactmentengine/serverless/scheduler/AlternativePlanScheduler.java) to find alternative plan(s). 
<!--- Please note that this might be changed in the future and this method will be ported to another module.-->

In order for the [AlternativePlanScheduler](src/main/java/at/enactmentengine/serverless/scheduler/AlternativePlanScheduler.java) to work properly the following steps are required:

1. A database with function invocations is required to retrieve old executions. Example: [FTDatabase.db](Database/FTDatabase.db). The database should contain the function which will be executed in the **Function** table.
2. (To generate random invocations to fill the database [DataBaseFiller](src/main/java/at/enactmentengine/serverless/main/DataBaseFiller.java) can be used) 
3. Now the `addAlternativePlansToYAML("path/to/file.yaml", "path/to/newly/created/optimizedFile.yaml")` can be used to generate the alternative strategy at runtime.
4. The optimized file can now be executed by the *xAFCL EE*.

Possible output from `addAlternativePlansToYAML()`:
````yaml
constraints:
    - name: "FT-AltPlan-0"
      value: "0.9879;https://jp-tok.functions.appdomain.cloud/api/v1/web/<link.to.function>/hello.json;https://eu-gb.functions.cloud.ibm.com/api/v1/web/<link.to.function>/hello.json;"
    - name: "FT-AltPlan-1"
      value: "0.9808;https://eu-de.functions.appdomain.cloud/api/v1/web/<link.to.function>/hello.json;https://us-south.functions.appdomain.cloud/api/v1/web/<link.to.function>/hello.json;"
````

The following example would invoke each function within an alternative plan `2` times:
````yaml
constraints:
    - name: "FT-Retries"
      value: "2"
    - name: "FT-AltPlan-0"
      value: "0.9879;https://jp-tok.functions.appdomain.cloud/api/v1/web/<link.to.function>/hello.json;https://eu-gb.functions.cloud.ibm.com/api/v1/web/<link.to.function>/hello.json;"
    - name: "FT-AltPlan-1"
      value: "0.9808;https://eu-de.functions.appdomain.cloud/api/v1/web/<link.to.function>/hello.json;https://us-south.functions.appdomain.cloud/api/v1/web/<link.to.function>/hello.json;"
````

- **C-latestStartingTime**

````yaml
constraints:
- name: "C-latestStartingTime"
  value: "2011-10-02 18:48:05.123"
````

will throw an `at.uibk.dps.exception.LatestStartingTimeException` if the start time of the function is before the specified time. 

- **C-latestFinishingTime**

````yaml
constraints:
- name: "C-latestFinishingTime"
  value: "2011-10-02 18:48:05.123"
````

will throw an `at.uibk.dps.exception.LatestFinishingTimeException` if the function did not finish before the specified time.

- **C-maxRunningTime**

````yaml
constraints:
- name: "C-maxRunningTime"
  value: "1240"
````

will stop waiting for a response of the cloud function after `1240` milliseconds. The *xAFCL EE* will throw an `at.uibk.dps.exception.MaxRunningTimeException` exception if the specified runtime is exceeded.

## Examples

You can find many examples for FCs including fault tolerance in the folder [examples](/examples/).


## Contributions

Several bachelor theses at department of computer science, University of Innsbruck, supervised by Dr. Sashko Ristov contributed to this project:

- "*xAFCLSim* simulation framework", Mika Hautz, WS2021 (Simulation)
- "Fault-tolerant execution of serverless functions across multiple FaaS systems", Matteo Bernard, Battaglin, SS2020.
- "Multi-provider enactment engine (EE) for serverless workflow applications", Jakob Nöckl, Markus Moosbrugger, SS2019. `Among top three theses for 2019` at the institute of computer science. (The initial version of the *xAFCL EE*)

## Publications

### *rAFCL*

S. Ristov, D. Kimovski and T. Fahringer, "FaaScinating Resilience for Serverless Function Choreographies in Federated Clouds," in *IEEE Transactions on Network and Service Management*, doi: [10.1109/TNSM.2022.3162036](https://doi.org/10.1109/TNSM.2022.3162036).

````
@ARTICLE{RistovrAFCL:2022,
  author={Ristov, Sasko and Kimovski, Dragi and Fahringer, Thomas},
  journal={IEEE Transactions on Network and Service Management}, 
  title={FaaScinating Resilience for Serverless Function Choreographies in Federated Clouds}, 
  year={2022},
  volume={},
  number={},
  pages={1-1},
  doi={10.1109/TNSM.2022.3162036}}
````

### *xAFCL*

S. Ristov, S. Pedratscher and T. Fahringer, "xAFCL: Run Scalable Function Choreographies Across Multiple FaaS Systems," in *IEEE Transactions on Services Computing*, [10.1109/TSC.2021.3128137](https://doi.org/10.1109/TSC.2021.3128137).

````
@article{RistovxAFCL:2021,
  author={Ristov, Sasko and Pedratscher, Stefan and Fahringer, Thomas},
  journal={IEEE Transactions on Services Computing}, 
  title={{xAFCL}: Run Scalable Function Choreographies Across Multiple {FaaS} Systems}, 
  year={2021},
  volume={},
  number={},
  pages={1-1},
  doi={10.1109/TSC.2021.3128137}
}
````

### *AFCL*

S. Ristov, S. Pedratscher, T. Fahringer, "AFCL: An Abstract Function Choreography Language for serverless workflow specification", *Future Generation Computer Systems*, Volume 114, 2021, Pages 368-382, ISSN 0167-739X, [10.1016/j.future.2020.08.012](https://doi.org/10.1016/j.future.2020.08.012).

````
@article{ristovAFCL:2020,
  title={AFCL: An Abstract Function Choreography Language for serverless workflow specification},
  author={Ristov, Sasko and Pedratscher, Stefan and Fahringer, Thomas},
  journal={Future Generation Computer Systems},
  volume={114},
  pages={368--382},
  publisher={Elsevier},
  doi={https://doi.org/10.1016/j.future.2020.08.012}
}
````




## Support

If you need any additional information, please do not hesitate to contact us.