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