# enactmentEngine (EE)

- **/externalJars** contain prebuild jars used within the EE. If you import the EE project, you will need to link these external libraries. Keep them upToDate if they work properly.
- **/src/main** 
    - **/java** contains the source code of the EE.
    - **/resources** contains example yaml files to test the execution
- **/src/main/resources/credentials.properties** contains the login credentials for the FaaS providers. This file should be filled by the users own credentials (it is added to **.gitignore**)