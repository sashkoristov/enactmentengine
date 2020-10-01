package at.enactmentengine.serverless.nodes;

import at.enactmentengine.serverless.exception.MissingInputDataException;
import at.enactmentengine.serverless.parser.Language;
import at.enactmentengine.serverless.parser.YAMLParser;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class ExecutableWorkflowTest {

    /**
     * Missing workflow input of an executable workflow.
     *
     * @author stefanpedratscher
     */
    @Test(expected = MissingInputDataException.class)
    public void missingWorkflowInput() throws MissingInputDataException, ExecutionException, InterruptedException, IOException {

        /* Read a workflow and parse to an executable workflow */
        ExecutableWorkflow executableWorkflow = new YAMLParser().parseExecutableWorkflow(
                FileUtils.readFileToByteArray(new File("src/test/resources/simpleWorkflow.yaml")), Language.YAML, 0);

        /* Execute workflow with missing input data */
        executableWorkflow.executeWorkflow(null);
    }
}
