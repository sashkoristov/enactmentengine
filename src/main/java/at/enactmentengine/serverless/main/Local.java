package at.enactmentengine.serverless.main;

import at.enactmentengine.serverless.nodes.FunctionNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Main class of enactment engine which specifies the input file and starts the
 * workflow on the machine on which it gets started.
 * <p>
 * based on @author markusmoosbrugger, jakobnoeckl
 * extended by @author stefanpedratscher
 */
public class Local {


    static final Logger logger = LoggerFactory.getLogger(Local.class);

    public static void main(String[] args) {
        Executor executor = new Executor();

        FunctionNode.logResults = false;

        Map<String, Object> result = null;
        if (args.length > 2) {
            FunctionNode.logResults = Boolean.parseBoolean(args[2]);
            result = executor.executeWorkflow(args[0], args[1],  -1);
        }else if (args.length > 1) {
            result = executor.executeWorkflow(args[0], args[1],  -1);
        } else if (args.length > 0) {
            result = executor.executeWorkflow(args[0], null,  -1);
        } else {
            logger.error("Usage: java -jar enactment-engine-all.jar path/to/workflow.yaml [path/to/input.json]");
        }

        logger.info("Result: {}", result);
    }
}
