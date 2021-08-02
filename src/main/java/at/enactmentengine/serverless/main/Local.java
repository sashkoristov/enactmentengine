package at.enactmentengine.serverless.main;

import at.enactmentengine.serverless.Simulation.SimulationParameters;
import at.uibk.dps.databases.MongoDBAccess;
import ch.qos.logback.classic.Level;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Main class of enactment engine which specifies the input file and starts the workflow on the machine on which it gets
 * started.
 * <p>
 * based on @author markusmoosbrugger, jakobnoeckl extended by @author stefanpedratscher; extended again as a part of
 * the simulator by @author mikahautz
 */
public class Local {

    /**
     * Logger for the local execution.
     */
    static final Logger logger = LoggerFactory.getLogger(Local.class);

    /**
     * Indicates whether the connection to the DB should be closed at the end.
     */
    private static boolean close = true;

    /**
     * Starting point of the local execution.
     *
     * @param args workflow.yaml [input.json]
     */
    public static void main(String[] args) {
        // sets the logging level to INFO only
        ch.qos.logback.classic.Logger rootLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
        rootLogger.setLevel(Level.INFO);
        /* Workflow executor */
        Executor executor = new Executor();
        Simulator simulator = new Simulator();

        /* Check for inputs and execute workflow */
        Map<String, Object> result = null;
        try {
            int length = args.length;
            List<String> parameterList = Arrays.asList(args);
            boolean simulate = parameterList.contains("--simulate");
            if (simulate) {
                SimulationParameters.IGNORE_FT = parameterList.contains("--ignore-FT") || parameterList.contains("--ignore-ft");
            }
            boolean export = parameterList.contains("--export");
            if (export) {
                length -= 1;
            }

            if (length > 2 && args[2].equals("--simulate")) {
                result = simulator.simulateWorkflow(args[0], args[1], -1);
            } else if (length > 1 && args[1].equals("--simulate")) {
                result = simulator.simulateWorkflow(args[0], null, -1);
            } else if (length > 1) {
                result = executor.executeWorkflow(args[0], args[1], -1);
            } else if (length > 0) {
                result = executor.executeWorkflow(args[0], null, -1);
            } else {
                logger.error("Usage: java -jar enactment-engine-all.jar path/to/workflow.yaml [path/to/input.json] [--simulate] [--ignore-FT]");
            }
            if (!simulate) {
                logger.info("Result: {}", result);
            }
            if (export) {
                exportLogsToFile();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            MongoDBAccess.addAllEntries();
            if (close) {
                MongoDBAccess.close();
            }
        }
    }

    /**
     * Executes the main method and returns a list of all logs.
     *
     * @param args given arguments
     *
     * @return a list of all logs
     */
    public static List<Document> executeAndGetLogs(String[] args) {
        close = false;
        main(args);
        List<Document> logs = MongoDBAccess.getAllEntries();
        MongoDBAccess.close();
        return logs;
    }

    /**
     * Exports all log entries to a file called "output.csv".
     */
    private static void exportLogsToFile() {
        StringBuilder header = new StringBuilder();
        StringBuilder sb = new StringBuilder();
        PrintWriter writer = null;
        String filename = "output.csv";
        try {
            writer = new PrintWriter(new FileWriter(filename, true));
        } catch (IOException e) {
            e.printStackTrace();
        }
        // get all log entries
        List<Document> logs = MongoDBAccess.getAllEntries();

        int i = 0;
        for (Document d : logs) {
            // iterate over every field in the document
            for (Map.Entry<String, Object> entry : d.entrySet()) {
                String key = entry.getKey();
                // ignore unneeded fields for the user
                if (key.equals("workflow_id") || key.equals("done") || key.equals("_id")) {
                    continue;
                }
                // if it is the first iteration, build the header
                if (i == 0) {
                    header.append(key);
                    header.append(",");
                }
                // if the entry is a date, reformat and append
                if (entry.getValue() instanceof Date) {
                    Date date = (Date) entry.getValue();
                    sb.append(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX").format(date));
                } else if (entry.getValue() != null) {
                    // if the entry is not null, append it to the string-builder
                    sb.append(entry.getValue().toString());
                }
                sb.append(",");
            }
            // if it is the first iteration and the file is empty, write the header
            if (i == 0 && new File(filename).length() == 0) {
                header.setLength(header.length() - 1);
                header.append("\n");
                writer.write(header.toString());
            }
            // remove the last char (= ",") and append a newline
            sb.setLength(sb.length() - 1);
            sb.append("\n");
            // write the values to the file
            writer.write(sb.toString());
            // reset the string-builder
            sb.setLength(0);
            i++;
        }
        writer.close();
    }
}
