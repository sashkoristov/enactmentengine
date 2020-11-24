package at.enactmentengine.serverless.parser;

import at.enactmentengine.serverless.nodes.ExecutableWorkflow;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class YAMLParserTest {

    /**
     * Parse a given YAML file to an executable workflow.
     */
    @Test
    public void parseYAMLToExecutableWorkflow() throws IOException {
        ExecutableWorkflow executableWorkflow = new YAMLParser().parseExecutableWorkflow(
                FileUtils.readFileToByteArray(new File("src/test/resources/allConstructs.yaml")),
                Language.YAML, 0);

        Assert.assertEquals("all_supported_constructs", executableWorkflow.getWorkflowName());
        //Assert.assertEquals(, executableWorkflow.getDataIns());
        //Assert.assertEquals(, executableWorkflow.getStartNode());
        //Assert.assertEquals(, executableWorkflow.getEndNode());
        // TODO add test check
    }
}
