package at.enactmentengine.serverless.main;

import at.enactmentengine.serverless.exception.MissingInputDataException;
import at.enactmentengine.serverless.nodes.ExecutableWorkflow;
import at.enactmentengine.serverless.parser.Language;
import at.enactmentengine.serverless.parser.YAMLParser;
import com.google.gson.JsonArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

/**
 * Main class of enactment engine which specifies the input file and starts the
 * workflow on the machine on which it gets started.
 * <p>
 * based on @author markusmoosbrugger, jakobnoeckl
 * extended by @author stefanpedratscher
 */
public class App {
	

    static final Logger logger = LoggerFactory.getLogger(App.class);

    
    public Map<String, Object> executeWorkflow(String fileName) {
    
        long time = System.currentTimeMillis();
     
        // Disable hostname verification (enable OpenWhisk connections)
        final Properties props = System.getProperties();
        props.setProperty("jdk.internal.httpclient.disableHostnameVerification", Boolean.TRUE.toString());

        // Get the input file as argument or default string
        if (fileName == null) {
        	System.out.println("Please specify a filename");
        }

        // Create an executable workflow
        YAMLParser yamlParser = new YAMLParser();
        ExecutableWorkflow ex = yamlParser.parseExecutableWorkflow(fileName, Language.YAML);
        Map<String, Object> output = null;
        if (ex != null) {

            // Set some example workflow input
            Map<String, Object> input = new HashMap<String, Object>();
            input.put("some source", "34477227772222299999");// for ref gate
            JsonArray arr = new JsonArray();
            JsonArray arr2 = new JsonArray();
            JsonArray arr3 = new JsonArray();
            JsonArray arr4 = new JsonArray();
            int arr1Size = 2000;
            int arr2Size = 0;
            int arr3Size = 0;
            int arr4Size = 0;
            int total = arr1Size + arr2Size + arr3Size + arr4Size; // each
            for(int i = 0; i < arr1Size; i++){
            //for(int i = 0; i < total; i++){
                arr.add(1);
            }
            for(int i = 0; i < arr2Size; i++){
               arr2.add(1);
            }
            for(int i = 0; i < arr3Size; i++){
                arr3.add(1);
             }
            for(int i = 0; i < arr4Size; i++){
                arr4.add(1);
             }
            input.put("each", 1);
            input.put("total", total);
            input.put("array", arr);
            input.put("array2", arr2);
            input.put("array3", arr3);
            input.put("array4", arr4);
            // input.put("some source", "4");// for anomaly
            // input.put("some source", 50);// for parallel and basic files
            input.put("some camera source", "0");
            input.put("some sensor source", "0");

            // Execute the workflow
            try {
            	output = ex.executeWorkflow(input);
            } catch (MissingInputDataException e) {
                logger.error(e.getMessage(), e);
            } catch (Exception e) {
				e.printStackTrace();
			}

            logger.info("Execution took " + (System.currentTimeMillis() - time) + "ms.");
        }
        return output;
    }
}
