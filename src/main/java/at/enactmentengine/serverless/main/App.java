package at.enactmentengine.serverless.main;

import at.enactmentengine.serverless.exception.MissingInputDataException;
import at.enactmentengine.serverless.nodes.ExecutableWorkflow;
import at.enactmentengine.serverless.nodes.ExecutableWorkflowOld;
import at.enactmentengine.serverless.parser.YAMLParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Main class of enactment engine which specifies the input file and starts the
 * workflow on the machine on which it gets started.
 *
 * based on @author markusmoosbrugger, jakobnoeckl
 * extended by @author stefanpedratscher
 */
public class App {

    public static void main(String[] args) throws MissingInputDataException {
        long time = System.currentTimeMillis();
        final Logger logger = LoggerFactory.getLogger(App.class);

        YAMLParser yamlParser = new YAMLParser();

        // Disable hostname verification (enable OpenWhisk connections)
        final Properties props = System.getProperties();
        props.setProperty("jdk.internal.httpclient.disableHostnameVerification", Boolean.TRUE.toString());

        // Get the input file as argument or default string
        String fileName;
        if (args.length > 0)
            fileName = args[0];
        else
            fileName = "src/main/resources/yaml_files/gateChangeAlertCFCL_AWS_1.yaml";

        // Create an executable workflow

        ExecutableWorkflow ex = yamlParser.parseExecutableWorkflow(fileName);
        if (ex != null) {

            // Set workflow input
            Map<String, Object> input = new HashMap<String, Object>();
            input.put("some source", "34477227772222299999");// for ref gate
            // input.put("some source", "4");// for anomaly
            input.put("some source", 50);// for parallel and basic files
            input.put("some camera source", "0");
            input.put("some sensor source", "0");

            // Execute the workflow
            try {
                ex.executeWorkflow(input);
            } catch (MissingInputDataException e) {
                logger.error(e.getMessage(), e);
            }

            logger.info("Execution took " + (System.currentTimeMillis() - time) + "ms!");
        }
    }
}
