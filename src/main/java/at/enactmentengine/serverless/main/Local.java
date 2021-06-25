package at.enactmentengine.serverless.main;

import at.enactmentengine.serverless.Simulation.SimulationParameters;
import at.uibk.dps.databases.MongoDBAccess;
import ch.qos.logback.classic.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Main class of enactment engine which specifies the input file and starts the workflow on the machine on which it gets
 * started.
 * <p>
 * based on @author markusmoosbrugger, jakobnoeckl extended by @author stefanpedratscher
 */
public class Local {

    /**
     * Logger for the local execution.
     */
    static final Logger logger = LoggerFactory.getLogger(Local.class);

    /**
     * Starting point of the local execution.
     *
     * @param args workflow.yaml [input.json]
     */
    public static void main(String[] args) {
        ch.qos.logback.classic.Logger rootLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
        rootLogger.setLevel(Level.INFO);
        /* Workflow executor */
        Executor executor = new Executor();
        Simulator simulator = new Simulator();

        /* Check for inputs and execute workflow */
        Map<String, Object> result = null;
        try {
            List<String> parameterList = Arrays.asList(args);
            if (parameterList.contains("--simulate")) {
                SimulationParameters.USE_COLD_START = parameterList.contains("--cold-start");
                SimulationParameters.USE_AUTHENTICATION = parameterList.contains("--authenticate");
            }

            if (args.length > 2 && args[2].equals("--simulate")) {
                result = simulator.simulateWorkflow(args[0], args[1], -1);
            } else if (args.length > 1 && args[1].equals("--simulate")) {
                result = simulator.simulateWorkflow(args[0], null, -1);
            } else if (args.length > 1) {
                result = executor.executeWorkflow(args[0], args[1], -1);
            } else if (args.length > 0) {
                result = executor.executeWorkflow(args[0], null, -1);
            } else {
                logger.error("Usage: java -jar enactment-engine-all.jar path/to/workflow.yaml [path/to/input.json]");
            }
            logger.info("Result: {}", result);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            MongoDBAccess.addAllEntries();
//        MariaDBAccess.doSth();
            MongoDBAccess.close();
        }
    }
}
