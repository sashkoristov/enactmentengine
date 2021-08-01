package at.enactmentengine.serverless.Handler;

import at.enactmentengine.serverless.nodes.FunctionNode;
import at.enactmentengine.serverless.nodes.Node;
import at.uibk.dps.afcl.functions.objects.DataIns;
import at.uibk.dps.afcl.functions.objects.PropertyConstraint;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

public class AsyncHandler{

    private boolean isAsync;
    private String[] functions;
    private List<Node> parent;
    private ArrayList<String> running;
    private ArrayList<String> failed;
    private ArrayList<String> finished;
    private long time;

    public AsyncHandler(boolean isAsync, List<DataIns> functions, List<Node> parents)
    {
        this.isAsync = isAsync;
        this.functions = functions.get(0).getValue().split(("((, )|,)"));
        this.failed = new ArrayList<>();
        this.parent = parents;
        this.running = new ArrayList<>();
        this.finished = new ArrayList<>();
    }

    public void run() {

        for (String functionName: this.functions) {
            try {
                String awsName = getAwsFunctionName(functionName, this.parent);
                if(awsName != null){
                    this.handle(awsName,functionName);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String getAwsFunctionName(String workflowFunctionName, List<Node> parents){
        String arn = getAwsArn(workflowFunctionName,parents);
        if(arn == null){
            return null;
        }
        String[] strings = arn.split(":");
        return strings[strings.length-1];
    }

    private String getAwsArn(String workflowFunctionName, List<Node> parents){
        //iterates over every parent
        for (Node parentNode:parents) {
            Node currentNode = parentNode;
            if (currentNode.getName().equals(workflowFunctionName)) {
                for (PropertyConstraint property : ((FunctionNode) currentNode).getProperties()) {
                    if (property.getName().equals("resource")) {
                        return property.getValue();
                    }
                }
            }
            // if the the node has parents we call it recursive
            if (currentNode.getParents() != null) {
                String parentArn = getAwsArn(workflowFunctionName,currentNode.getParents());
                if(parentArn != null){
                    return parentArn;
                }
            }
        }

        return null;
    }

    private void handle(String awsName, String workflowName) throws IOException {
        String queryId = makeLogQuery(awsName);
        String returnString = retrieveLogData(queryId);
        checkFunction(returnString,awsName,workflowName);
    }

    /**
     * checks if a function is running, failed or finished
     *
     * @param returnString the log data for the function
     * @param awsName the function we check
     */
    private void checkFunction(String returnString,String awsName,String workflowName){
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
                        this.running.add(workflowName+","+awsName);
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
                                    this.failed.add(workflowName+","+awsName);
                                    break outerLoop;
                                }
                                if (field.equals("@message") && value.startsWith("START")) {
                                    this.finished.add(workflowName+","+awsName);
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
    private String retrieveLogData(String queryId){
        //assample the terminal command
        String command = "aws logs get-query-results --query-id " + queryId;
        String[] exec = new String[]{"/bin/bash", "-c", command};

        String returnString = "";
        try {
            Process proc = Runtime.getRuntime().exec(exec);

            // Read the output from the command
            BufferedReader stdInput = new BufferedReader(new
                    InputStreamReader(proc.getInputStream()));
            System.out.println("Here is the standard output of the command:\n");
            String s = null;

            while ((s = stdInput.readLine()) != null) {
                System.out.println(s);
                returnString += s;
            }
            // Read any errors from the attempted command
            BufferedReader stdError = new BufferedReader(new
                    InputStreamReader(proc.getErrorStream()));
            System.out.println("Here is the standard error of the command (if any):\n");
            while ((s = stdError.readLine()) != null) {
                System.out.println(s);
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
    private String makeLogQuery(String function) {
        //create timestamp for start and end time
        Calendar calendar = Calendar.getInstance(TimeZone.getDefault());
        calendar.clear();
        LocalTime.now(TimeZone.getDefault().toZoneId());
        calendar.set(LocalDate.now().getYear(),LocalDate.now().getMonth().getValue()-1,LocalDate.now().getDayOfMonth(),
                LocalTime.now().getHour(),LocalTime.now().getMinute(),LocalTime.now().getSecond());
        long inputEndTime = calendar.getTimeInMillis() ;
        long inputStartTime = calendar.getTimeInMillis() - 86400000L;

        System.out.println(inputStartTime);
        System.out.println(inputEndTime);
        System.out.println(function);

        // create terminal command and execute it ----------------------------------------------------------------------
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
            System.out.println("Here is the standard output of the command:\n");
            String s = null;

            while ((s = stdInput.readLine()) != null) {
                System.out.println(s);
                returnString += s;
            }

            // Read any errors from the attempted command
            BufferedReader stdError = new BufferedReader(new
                    InputStreamReader(proc.getErrorStream()));
            System.out.println("Here is the standard error of the command (if any):\n");
            while ((s = stdError.readLine()) != null) {
                System.out.println(s);
            }

        } catch (Exception e) {
            //print exception
            System.out.println("Exception:\n" + e);
        }
        //get the query id from the returnValue
        return returnString.split("queryId\": \"")[1].split("\"}")[0];

    }

    // getter
    public ArrayList<String> getFailed() {
        return this.failed;
    }
    public ArrayList<String> getFinished() {
        return this.finished;
    }
    public ArrayList<String> getRunning() {
        return this.running;
    }
}
