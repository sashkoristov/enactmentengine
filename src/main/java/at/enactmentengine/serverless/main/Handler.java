package at.enactmentengine.serverless.main;

import at.uibk.dps.SocketUtils;
import at.uibk.dps.communication.EnactmentEngineRequest;
import at.uibk.dps.communication.EnactmentEngineResponse;
import at.uibk.dps.communication.entity.Statistics;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.*;
import java.net.Socket;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class to handle service requests.
 *
 * @author stefanpedratscher
 */
public class Handler implements Runnable {

    /**
     * Logger for request handler.
     */
    private static final Logger LOGGER = Logger.getLogger(Handler.class.getName());

    /**
     * Connected client sending the request.
     */
    private Socket socket;

    /**
     * Default constructor for handler.
     *
     * @param socket client connecting to service.
     */
    public Handler(Socket socket) {
        this.socket = socket;
    }

    /**
     * Handle request.
     */
    @Override
    public void run() {

        try {
            /* Wait for request */
            EnactmentEngineRequest enactmentEngineRequest = SocketUtils.receiveJsonObject(socket, EnactmentEngineRequest.class);

            /* Store to file */
            SocketUtils.writeToFile(enactmentEngineRequest.getWorkflowFileContent(), Thread.currentThread().getId() + ".yaml");

            /* Start measuring time for workflow execution */
            long start = System.currentTimeMillis();

            /* Get the execution id of the workflow execution */
            int executionId = getExecutionId();

            /* Execute the workflow */
            Executor executor = new Executor();
            Map<String, Object> executionResult = executor.executeWorkflow(Thread.currentThread().getId() + ".yaml", executionId);

            /* Stop measuring time for workflow execution */
            long end = System.currentTimeMillis();

            /* Prepare the execution result */
            EnactmentEngineResponse response = new EnactmentEngineResponse(
                    new Gson().toJsonTree(executionResult).getAsJsonObject(),
                    executionId,
                    new Statistics(new Timestamp(start + TimeZone.getTimeZone("Europe/Rome").getOffset(start)), new Timestamp(end + TimeZone.getTimeZone("Europe/Rome").getOffset(start)))
            );

            /* Send back json string because other modules might not have GSON */
            LOGGER.log(Level.INFO, "Sending back result");

            /* Send response back to client */
            SocketUtils.sendJsonObject(socket, response);

            /* Close connection */
            socket.close();

        } catch (IOException ex) {
            LOGGER.severe(ex.getMessage());
        }
    }

    /**
     * Get the execution identifier from the logger service
     *
     * @return execution identifier
     */
    private int getExecutionId(){

        /* Connect to logger service */
        LOGGER.info("Connecting to logger service...");

        try (Socket loggerService = new Socket("logger-service", 9005)) {

            /* Prepare and send request */
            JsonObject request = new JsonObject();
            request.addProperty("requestType", "GET_EXECUTION_ID");
            String requestString = request.toString();
            LOGGER.log(Level.INFO, "Sending request {}...",  requestString);
            ObjectOutputStream out = new ObjectOutputStream(loggerService.getOutputStream());
            out.writeObject(requestString);
            out.flush();

            /* Wait for response (wait for filtered resources) */
            LOGGER.info("Waiting for response from logger service...");
            ObjectInputStream objectInputStream = new ObjectInputStream(loggerService.getInputStream());
            String result = (String) objectInputStream.readObject();

            /* Close connection */
            LOGGER.info("Closing connection to logger service...");

            /* Check if logger service returned a valid execution identifier */
            if(result == null || ("-1".equals(result))){
                LOGGER.warning("Logger service returned an invalid executionId.");
                return -1;
            }

            /* Return response */
            LOGGER.log(Level.INFO, "Response got: {}", result);
            return Integer.parseInt(result);
        } catch (IOException | ClassNotFoundException e) {

            /* Log error on failure */
            LOGGER.severe("Could not get execution Id: " + e.getLocalizedMessage());
            return -1;
        }
    }
}
