# enactmentEngine (EE)

- **/externalJars** contain prebuild jars used within the EE. If you import the EE project, you will need to link these external libraries. Keep them upToDate if they work properly.
- **/src/main** 
    - **/java** contains the source code of the EE.
    - **/resources** contains example yaml files to test the execution
- You will need to create a **/src/main/resources/credentials.properties** file, which contains the login credentials for the FaaS providers. This file will be ignored from git (**.gitignore**).

    The file should look as follows:
    ````
    aws_access_key=<your_aws_access_key>
    aws_secret_key=<your_aws_secret_key>
    ibm_api_key=<your_ibm_api_key>
    ````
    
---------------
    
## Deploy

#### Local
Simply run the [main method](src/main/java/at/enactmentengine/serverless/main/App.java) and pass the workflow yaml file as parameter.

#### AWS
1. Create an AWS Lambda function representing the EE (Upload the .jar)
   ````
   Runtime: Java8
   Handler: at.enactmentengine.serverless.main.LambdaHandler::handleRequest
   ````
2. Invoke the function with a specific input file:
    ````
    {
      "filename": "<your_workflow.yaml>"
    }
    ````
    or
    ````
    {
      "workflow": "<your_workflow_json_string>"
    }
    ````
    Use tools like https://www.json2yaml.com/ to convert from yaml to json and https://www.freeformatter.com/json-escape.html to escape characters.
    
#### IBM
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
        "filename": "<your_workflow.yaml>"
    }
    ````
    or
    ````
    {
      "workflow": "<your_workflow_json_string>"
    }
    ````
    Use tools like https://www.json2yaml.com/ to convert from yaml to json and https://www.freeformatter.com/json-escape.html to escape characters.
    


  