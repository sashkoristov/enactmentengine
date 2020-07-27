package at.enactmentengine.serverless.main;

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

        Map<String, Object> result;
        if (args.length > 0) {
            result = executor.executeWorkflow(args[0], -1);
        } else {
            result = executor.executeWorkflow("src/main/resources/new_files/emptyFunction.yaml", -1);
        }

        logger.info("Result: " + result);
    }
}
