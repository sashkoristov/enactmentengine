package at.enactmentengine.serverless.Handler;

import at.enactmentengine.serverless.nodes.*;
import at.enactmentengine.serverless.object.FunctionAttributes;
import at.enactmentengine.serverless.object.Utils;
import at.uibk.dps.*;
import at.uibk.dps.afcl.functions.objects.DataIns;
import at.uibk.dps.afcl.functions.objects.PropertyConstraint;
import at.uibk.dps.database.SQLLiteDatabase;
import at.uibk.dps.exception.CancelInvokeException;
import at.uibk.dps.exception.InvokationFailureException;
import at.uibk.dps.function.AlternativeStrategy;
import at.uibk.dps.function.ConstraintSettings;
import at.uibk.dps.function.FaultToleranceSettings;
import at.uibk.dps.function.Function;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static java.lang.Thread.sleep;

public class AsyncHandler{

    private boolean isAsync;
    private ArrayList<String> functions;
    private List<Node> parent;
    private ArrayList<String> running;
    private ArrayList<String> failed;
    private ArrayList<String> finished;
    private long time;
    private AWSAccount awsAccount;
    private IBMAccount ibmAccount;
    private GoogleFunctionAccount googleFunctionAccount;
    private AzureAccount azureAccount;
    private Map<String, Object> dataValues;

    /**
     * @param isAsync if its executed async or not
     * @param functions the function to check for
     * @param parents the parents in the workflow
     * @param dataValues the input of the node
     */
    public AsyncHandler(boolean isAsync, List<DataIns> functions, List<Node> parents,Map<String, Object> dataValues)
    {
        this.dataValues = dataValues;
        this.isAsync = isAsync;
        this.functions = new ArrayList<>(Arrays.asList(functions.get(0).getValue().split(("((, )|,)"))));
        this.failed = new ArrayList<>();
        this.parent = parents;
        this.running = new ArrayList<>();
        this.finished = new ArrayList<>();
    }

    /**
     * executes the the async handler
     */
    public void run()  {
        ArrayList<String> runningFunctions = this.functions;
        do {
            long timeStart = System.currentTimeMillis();
            runHelper(runningFunctions);
            long timeEnd = System.currentTimeMillis();
            runningFunctions = this.running;
            this.running = new ArrayList<>();

            //end the checking if its async or all functions are finished
            if(isAsync || runningFunctions.size()==0){
                break;
            }
            try {
                //todo check time to wait
                TimeUnit.MILLISECONDS.sleep(500000-(timeStart+timeEnd)/1000000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }   while(true);
        //set the running for the function output
        this.running = runningFunctions;
    }

    /**
     * checks each functions state
     *
     * @param functions the function to be checked for
     */
    private void runHelper(ArrayList<String> functions){
        for (String functionName : functions) {
            try {
                FunctionAttributes functionAttributes = getFunctionAttributesHelper(functionName, this.parent);
                //do not check the function if not executed in async
                if(!functionAttributes.isAsync()){
                    this.finished.add(functionAttributes.getName());
                    continue;
                }
                if (functionAttributes.getAwsName() != null) {
                    int size = this.failed.size();
                    this.handleAwsFunction(functionAttributes.getAwsName(), functionName);
                    if(this.failed.size()>size){
                        if(functionAttributes.getFunction().hasFTSet()) {
                            runFT(functionAttributes);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String invokeAlternativeStrategy(Function function) throws Exception {
        if (function.getFTSettings().getAltStrategy() == null) {
            throw new Exception("No alternative Strategy defined");
        } else {
            int i = 0;
            Iterator var3 = function.getFTSettings().getAltStrategy().iterator();

            while(var3.hasNext()) {
                List<Function> alternativePlan = (List)var3.next();
                ++i;

                try {
                    String result = this.parallelInvoke(alternativePlan);
                    return result;
                } catch (CancelInvokeException var6) {
                    throw var6;
                } catch (Exception var7) {
                }
            }

            throw new Exception("Failed after entire Alternative Strategy");
        }
    }
    private String parallelInvoke(List<Function> functionList) throws CancelInvokeException, InvokationFailureException {
        boolean running = true;
        List<InvokationThread> workerList = new ArrayList(functionList.size());
        if (functionList != null && functionList.size() > 0) {
            Iterator var4 = functionList.iterator();

            while(var4.hasNext()) {
                Function functionToBeInvoked = (Function)var4.next();
                InvokationThread invocationThread = null;
                if (this.awsAccount != null && this.ibmAccount != null && this.googleFunctionAccount != null && this.azureAccount != null) {
                    invocationThread = new InvokationThread(this.googleFunctionAccount, this.azureAccount, this.awsAccount, this.ibmAccount, functionToBeInvoked);
                } else if (this.awsAccount != null && this.googleFunctionAccount != null && this.azureAccount != null) {
                    invocationThread = new InvokationThread(this.googleFunctionAccount, this.azureAccount, this.awsAccount, functionToBeInvoked);
                } else if (this.azureAccount != null && this.googleFunctionAccount != null && this.ibmAccount != null) {
                    invocationThread = new InvokationThread(this.googleFunctionAccount, this.azureAccount, this.ibmAccount, functionToBeInvoked);
                } else if (this.azureAccount != null && this.googleFunctionAccount != null) {
                    invocationThread = new InvokationThread(this.googleFunctionAccount, this.azureAccount, functionToBeInvoked);
                } else {
                    //invocationThread = new InvokationThread(this.awsAccount, this.ibmAccount, functionToBeInvoked);
                }

                Thread thread = new Thread(invocationThread);
                thread.start();
                workerList.add(invocationThread);
            }

            do {
                if (!running) {
                    throw new InvokationFailureException("Failed");
                }

                int indexOfSucessfull = this.successfullThread(workerList);
                if (indexOfSucessfull != -1) {
                    String correctResult = ((InvokationThread)workerList.get(indexOfSucessfull)).getResult();
                    this.terminateAll(workerList);
                    return correctResult;
                }

                if (this.allThreadsDone(workerList) && this.successfullThread(workerList) == -1) {
                    running = false;
                    throw new InvokationFailureException("All Threads Failed");
                }

                try {
                    Thread.sleep(50L);
                } catch (InterruptedException var8) {
                    int a=1;
                }
            } while(true);
        } else {
            throw new InvokationFailureException("FunctionList is empty or null!");
        }
    }
    private int successfullThread(List<InvokationThread> workerList) {
        int numThreads = workerList.size();

        for(int index = 0; index < numThreads; ++index) {
            if (((InvokationThread)workerList.get(index)).isFinished() && ((InvokationThread)workerList.get(index)).getResult() != null) {
                return index;
            }
        }

        return -1;
    }
    private void terminateAll(List<InvokationThread> workerList) {
        Iterator var2 = workerList.iterator();

        while(var2.hasNext()) {
            InvokationThread thread = (InvokationThread)var2.next();
            thread.stop();
        }

    }
    private boolean allThreadsDone(List<InvokationThread> workerList) {
        int numThreads = workerList.size();

        for(int index = 0; index < numThreads; ++index) {
            if (!((InvokationThread)workerList.get(index)).isFinished()) {
                return false;
            }
        }

        return true;
    }

    /**
     * gets the function attributes for the workflow
     *
     * @param workflowFunctionName the function name in the workflow
     * @param parents the parents from the async handler
     *
     * @return functionAttributes
     */
    private FunctionAttributes getFunctionAttributesHelper(String workflowFunctionName, List<Node> parents){
        FunctionAttributes functionAttributes = new FunctionAttributes();
        functionAttributes = getFunctionAttributes(workflowFunctionName,parents,functionAttributes);

        return functionAttributes;
    }

    /**
     * the recursive function to get the attributes from the workflow
     *
     * @param workflowFunctionName the function name in the workflow
     * @param parents the parents from the async handler
     * @param functionAttributes the previous functionAttributes
     *
     * @return functionAttributes
     */
    private FunctionAttributes getFunctionAttributes(String workflowFunctionName, List<Node> parents, FunctionAttributes functionAttributes){
        //iterates over every parent
        for (Node currentNode:parents) {
            //increase the counter when seeing an end node
            if(currentNode.getClass().equals(ParallelEndNode.class) || currentNode.getClass().equals(ParallelForEndNode.class)){
                functionAttributes.increaseCounter();
            }
            //decrease the counter when seeing a start node
            if(currentNode.getClass().equals(ParallelStartNode.class) || currentNode.getClass().equals(ParallelForStartNode.class)){
                functionAttributes.decreaseCounter();
            }
            if (currentNode.getName().equals(workflowFunctionName)) {
                String arn = "";
                FunctionNode currentFunction = ((FunctionNode) currentNode);
                functionAttributes.setDataValues(currentFunction.getDataValues());
                for (PropertyConstraint property : currentFunction.getProperties()) {
                    if (property.getName().equals("resource")) {
                        arn = property.getValue();
                        String[] strings = arn.split(":");
                        functionAttributes.setAwsName(strings[strings.length-1]);
                        functionAttributes.setName(workflowFunctionName);
                    }
                    if(property.getName().equals("invoke-type")){
                        functionAttributes.setAsync(Objects.equals(property.getValue(), "ASYNC"));
                    }
                }
                //get ft shit
                Map<String, Object> actualFunctionInputs = new HashMap<>();
                /* Iterate over all specified inputs */
                if(currentFunction.getInput()!= null){
                    for (DataIns data : currentFunction.getInput()) {
                        /* Check if the element should be passed to the output */
                        if (data.getPassing() != null && data.getPassing()) {
                        } else {
                            if(functionAttributes.getDataValues().containsKey(data.getSource()))
                                actualFunctionInputs.put(data.getName(), functionAttributes.getDataValues().get(data.getSource()));
                            else if(!currentFunction.getDataValues().containsKey(data.getName())) {
                                actualFunctionInputs.put(data.getName(), dataValues.get(data.getSource()));
                            }
                        }
                    }
                }
                /* Simulate Availability if specified*/
                if (Utils.SIMULATE_AVAILABILITY) {
                    SQLLiteDatabase db = new SQLLiteDatabase("jdbc:sqlite:Database/FTDatabase.db");
                    double simAvail = db.getSimulatedAvail(arn);
                    actualFunctionInputs = checkFunctionSimAvail(simAvail, actualFunctionInputs);
                }
                functionAttributes.setFunction(parseFTConstraints(currentFunction.getConstraints(),"function",actualFunctionInputs,arn));

                return functionAttributes;
            }
            // if the node has parents we call it recursive
            if (currentNode.getParents() != null) {
                functionAttributes = getFunctionAttributes(workflowFunctionName,currentNode.getParents(),functionAttributes);
                if(functionAttributes.getAwsName()!= null) {
                    return functionAttributes;
                }
            }
        }

        return functionAttributes;
    }

    /**
     * handles the checking for aws functions
     *
     * @param awsName the aws name of the function
     * @param workflowName the workflow name of the function
     *
     * @throws IOException
     */
    private void handleAwsFunction(String awsName, String workflowName) throws IOException {
        String queryId = makeAwsLogQuery(awsName);
        String returnString = retrieveAwsLogData(queryId);
        checkAwsFunction(returnString,workflowName);
    }

    /**
     * checks if a function is running, failed or finished
     *
     * @param returnString the log data for the function
     * @param workflowName the function we check
     */
    private void checkAwsFunction(String returnString, String workflowName){
        JSONParser jsonParser = new JSONParser();
        JSONObject jsonObject = new JSONObject();
        try {
            //convert the string to a json
            jsonObject = (JSONObject) jsonParser.parse(returnString);
            //get the query results out of the return string
            JSONArray results = (JSONArray) jsonObject.get("results");

            //iterates over rows from log
            outerLoop:
            for (Object lastResult: results) {
                //JSONObject[] array = (JSONObject[]) lastResult;
                //iterates over items in row
                for (Object o: (JSONArray)lastResult) {
                    JSONObject object = (JSONObject) o;
                    String value = (String)object.get("value");
                    String field = (String)object.get("field");
                    if (field.equals("@message") && value.startsWith("START")) {
                        this.running.add(workflowName);
                        break outerLoop;
                    } else if (field.equals("@message") && value.startsWith("END")) {
                        //iterates over rows in log
                        for (Object e : results) {
                            //iterates over items in row
                            JSONArray entries = (JSONArray) e;
                            for (Object entry : entries) {
                                object = (JSONObject)entry;
                                value = (String)object.get("value");
                                field = (String)object.get("field");
                                if (field.equals("@message") && value.startsWith("[ERROR]")) {
                                    this.failed.add(workflowName);
                                    break outerLoop;
                                }
                                if (field.equals("@message") && value.startsWith("START")) {
                                    this.finished.add(workflowName);
                                    break outerLoop;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            //print exception
            System.out.println("Exception:\n" + e);
        }
    }

    /**
     * retrieves the data from a query
     *
     * @param queryId the query id for the query we want the data
     *
     * @return the data in string form
     */
    private String retrieveAwsLogData(String queryId){
        //assemble the terminal command
        String command = "aws logs get-query-results --query-id " + queryId;
        String[] exec = new String[]{"/bin/bash", "-c", command};

        String returnString = "";
        try {
            Process proc = Runtime.getRuntime().exec(exec);

            // Read the output from the command
            BufferedReader stdInput = new BufferedReader(new
                    InputStreamReader(proc.getInputStream()));
            //System.out.println("Here is the standard output of the command:\n");
            String s = null;

            while ((s = stdInput.readLine()) != null) {
                //System.out.println(s);
                returnString += s;
            }
            // Read any errors from the attempted command
            BufferedReader stdError = new BufferedReader(new
                    InputStreamReader(proc.getErrorStream()));
            //System.out.println("Here is the standard error of the command (if any):\n");
            while ((s = stdError.readLine()) != null) {
                //System.out.println(s);
            }
        } catch (Exception e) {
            //print exception
            System.out.println("Exception:\n" + e);
        }
        return returnString;
    }

    /**
     * makes a log query for the function
     *
     * @param function the function we want the logs for
     *
     * @return the query id from the executed query
     */
    private String makeAwsLogQuery(String function) {
        //create timestamp for start and end time
        Calendar calendar = Calendar.getInstance(TimeZone.getDefault());
        calendar.clear();
        LocalTime.now(TimeZone.getDefault().toZoneId());
        calendar.set(LocalDate.now().getYear(),LocalDate.now().getMonth().getValue()-1,LocalDate.now().getDayOfMonth(),
                LocalTime.now().getHour(),LocalTime.now().getMinute(),LocalTime.now().getSecond());
        long inputEndTime = calendar.getTimeInMillis() ;
        long inputStartTime = calendar.getTimeInMillis() - 86400000L;

        // create terminal command and execute it
        String command = "aws logs start-query --log-group-name \"/aws/lambda/"
                + function + "\" --start-time " + inputStartTime + " --end-time "
                + inputEndTime + " --query-string \"fields @timestamp,@message,@type\"";
        String[] exec = {"/bin/bash", "-c", command};
        String returnString = "";
        try {
            //execute the command
            Process proc = Runtime.getRuntime().exec(exec);

            // Read the output from the command
            BufferedReader stdInput = new BufferedReader(new
                    InputStreamReader(proc.getInputStream()));
            //System.out.println("Here is the standard output of the command:\n");
            String s = null;

            while ((s = stdInput.readLine()) != null) {
                //System.out.println(s);
                returnString += s;
            }

            // Read any errors from the attempted command
            BufferedReader stdError = new BufferedReader(new
                    InputStreamReader(proc.getErrorStream()));
            //System.out.println("Here is the standard error of the command (if any):\n");
            while ((s = stdError.readLine()) != null) {
                //System.out.println(s);
            }

        } catch (Exception e) {
            //print exception
            System.out.println("Exception:\n" + e);
        }
        //get the query id from the returnValue
        return returnString.split("queryId\": \"")[1].split("\"}")[0];

    }

    //FT for failed async functions ------------------------------------------------------------------------------------

    /**
     * executes the the FT if a function is failed
     * @param functionAttributes the attributes of the failed function
     */
    private void runFT(FunctionAttributes functionAttributes)
    {
        // if the function is in a parallel section we do not support FT
        if(functionAttributes.getParallelCounter()!=0){
            this.failed.remove(functionAttributes.getName());
            this.failed.add(functionAttributes.getName()+"(cannot support FT in parallel)");
            return;
        }
        //if the function has not FT we return
        if(!functionAttributes.getFunction().hasFTSet()){
            return;
        }
        //execute FT for the failed function
        if (functionAttributes.getFunction().getFTSettings().hasAlternativeStartegy()) {
            try {
                String result = this.invokeAlternativeStrategy(functionAttributes.getFunction());
            } catch (Exception e) {
                this.failed.remove(functionAttributes.getName());
                //add suffix for dev feedback
                this.failed.add(functionAttributes.getName()+"(FT failed too)");
                return;
            }
            this.failed.remove(functionAttributes.getName());
            //add suffix for dev feedback
            this.finished.add(functionAttributes.getName()+"(with FT)");
        }
    }

    /**
     * Add availability value to the function input.
     *
     * @param simAvail       the simulated availability from the database.
     * @param functionInputs the actual function input.
     * @return the new function input.
     */
    private Map<String, Object> checkFunctionSimAvail(double simAvail, Map<String, Object> functionInputs) {

        /* Check if this functions avail should be simulated */
        if (simAvail != 1) {
            functionInputs.put("availability", simAvail);
        }
        return functionInputs;
    }

    /**
     * Parse the fault tolerance constraints.
     *
     * @param resourceLink   the resource link of the base function.
     * @param functionInputs inputs to the base function.
     *
     * @return function object with correctly set ft values.
     */
    public Function parseFTConstraints(
            List<PropertyConstraint> constraints,
            String type,
            Map<String,
            Object> functionInputs,
            String resourceLink)
    {
        /* Keeps track of all constraint settings */
        List<PropertyConstraint> cList = new LinkedList<>();

        /* Keeps track of all fault tolerance settings */
        List<PropertyConstraint> ftList = new LinkedList<>();

        /* Check if there are constraints set */
        if (constraints == null) {
            return null;
        }

        /* Iterate over constraints and look for according settings */
        for (PropertyConstraint constraint : constraints) {
            if (constraint.getName().startsWith("FT-")) {
                ftList.add(constraint);
            } else if (constraint.getName().startsWith("C-")) {
                cList.add(constraint);
            }
        }

        /* Parse fault tolerance settings */
        FaultToleranceSettings ftSettings = getFaultToleranceSettings(ftList, functionInputs,type);

        /* Parse constraint settings */
        ConstraintSettings cSettings = getConstraintSettings(cList);

        return new Function(resourceLink, type, functionInputs, ftSettings.isEmpty() ? null : ftSettings,
                cSettings.isEmpty() ? null : cSettings);
    }

    /**
     * Look for fault tolerance settings.
     *
     * @param ftList         all fault tolerance settings.
     * @param functionInputs the input of the base function.
     *
     * @return fault tolerance settings.
     */
    private FaultToleranceSettings getFaultToleranceSettings(List<PropertyConstraint> ftList,
                                                             Map<String, Object> functionInputs,String type) {

        /* Set the default fault tolerance settings to zero retries */
        FaultToleranceSettings ftSettings = new FaultToleranceSettings(0);

        /* Create a lis for the alternative strategy */
        List<List<Function>> alternativeStrategy = new LinkedList<>();

        /* Iterate over all fault tolerance constraints and check for supported ones */
        for (PropertyConstraint ftConstraint : ftList) {
            if (ftConstraint.getName().compareTo("FT-Retries") == 0) {

                /*
                 * Set the given number of retries a base function should be repeated if a
                 * failure happens
                 */
                ftSettings.setRetries(Integer.valueOf(ftConstraint.getValue()));
            } else if (ftConstraint.getName().startsWith("FT-AltPlan-")) {

                /* Pack all alternative function into an alternative plan */
                List<Function> alternativePlan = new LinkedList<>();
                String possibleResources = ftConstraint.getValue().substring(ftConstraint.getValue().indexOf(";") + 1);
                while (possibleResources.contains(";")) {
                    String funcString = possibleResources.substring(0, possibleResources.indexOf(";"));
                    Function tmpFunc = new Function(funcString, type, functionInputs);
                    possibleResources = possibleResources.substring(possibleResources.indexOf(";") + 1);

                    alternativePlan.add(tmpFunc);
                }
                alternativeStrategy.add(alternativePlan);
            }
        }
        ftSettings.setAltStrategy(new AlternativeStrategy(alternativeStrategy));
        return ftSettings;
    }
    /**
     * Look for constraint settings.
     *
     * @param cList all constraint settings.
     *
     * @return constraint settings.
     */
    private ConstraintSettings getConstraintSettings(List<PropertyConstraint> cList) {

        /* Set the default constraint settings */
        ConstraintSettings cSettings = new ConstraintSettings(null, null, 0);

        /* Iterate over all constraint settings and check for supported ones */
        for (PropertyConstraint cConstraint : cList) {
            if (cConstraint.getName().compareTo("C-latestStartingTime") == 0) {
                cSettings.setLatestStartingTime(Timestamp.valueOf(cConstraint.getValue()));
            } else if (cConstraint.getName().compareTo("C-latestFinishingTime") == 0) {
                cSettings.setLatestFinishingTime(Timestamp.valueOf(cConstraint.getValue()));
            } else if (cConstraint.getName().compareTo("C-maxRunningTime") == 0) {
                cSettings.setMaxRunningTime(Integer.valueOf(cConstraint.getValue()));
            }
        }
        return cSettings;
    }

    //getter / setter --------------------------------------------------------------------------------------------------
    public void setAccounts(
            AWSAccount awsAccount,
            IBMAccount ibmAccount,
            GoogleFunctionAccount googleFunctionAccount,
            AzureAccount azureAccount)
    {
        this.awsAccount= awsAccount;
        this.ibmAccount = ibmAccount;
        this.googleFunctionAccount= googleFunctionAccount;
        this.azureAccount= azureAccount;
    }

    public ArrayList<String> getFailed() {
        return this.failed;
    }
    public ArrayList<String> getFinished() {
        return this.finished;
    }
    public ArrayList<String> getRunning() {
        return this.running;
    }
    public ArrayList<String> getFunctions() {
        return functions;
    }
    public AWSAccount getAWSAccount() {
        return awsAccount;
    }
    public AzureAccount getAzureAccount() {
        return azureAccount;
    }
    public GoogleFunctionAccount getGoogleAccount() {
        return googleFunctionAccount;
    }
    public IBMAccount getIBMAccount() {
        return ibmAccount;
    }
}
