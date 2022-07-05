package at.enactmentengine.serverless.main;

// import at.uibk.dps.socketutils.ConstantsNetwork;
// import at.uibk.dps.socketutils.UtilsSocket;
// import at.uibk.dps.socketutils.enactmentengine.RequestEnactmentEngine;
// import at.uibk.dps.socketutils.enactmentengine.ResponseEnactmentEngine;
// import at.uibk.dps.socketutils.enactmentengine.UtilsSocketEnactmentEngine;
// import at.uibk.dps.socketutils.entity.Statistics;
// import at.uibk.dps.socketutils.logger.RequestLoggerExecutionId;
// import at.uibk.dps.socketutils.logger.ResponseLogger;
// import at.uibk.dps.socketutils.logger.UtilsSocketLogger;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.net.Socket;
import java.sql.Timestamp;
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
		/*
		try {
			//
			RequestEnactmentEngine enactmentEngineRequest = UtilsSocket.receiveJsonObject(socket.getInputStream(),
					RequestEnactmentEngine.class);

			// Start measuring time for workflow execution
			long start = System.currentTimeMillis();

			int executionId = -1;
			if (enactmentEngineRequest.isLogResults()) {
				// Get the execution id of the workflow execution
				executionId = getExecutionId();
			}

			// Execute the workflow
			Executor executor = new Executor();
			Map<String, Object> executionResult = executor.executeWorkflow(
					enactmentEngineRequest.getWorkflow(),
					enactmentEngineRequest.getWorkflowInput(), executionId, start);

			// Stop measuring time for workflow execution
			long end = System.currentTimeMillis();

			// Prepare the execution result

			final JsonObject wfResult = new Gson().toJsonTree(executionResult).getAsJsonObject();
			final Statistics executionStats = new Statistics(
					new Timestamp(start + TimeZone.getTimeZone("Europe/Rome").getOffset(start)),
					new Timestamp(end + TimeZone.getTimeZone("Europe/Rome").getOffset(start)));
			ResponseEnactmentEngine response = UtilsSocketEnactmentEngine.generateResponse(wfResult, executionId,
					executionStats);

			// Send back json string because other modules might not have GSON
			LOGGER.log(Level.INFO, "Sending back result");

			// Send response back to client
			UtilsSocket.sendJsonObject(socket.getOutputStream(), response);

			// Close connection
			socket.close();

		} catch (IOException ex) {
			LOGGER.severe(ex.getMessage());
		}
		 */
	}

	/**
	 * Get the execution identifier from the logger service
	 *
	 * @return execution identifier
	 */
	private int getExecutionId() {
		/*
		// Connect to logger service
		LOGGER.info("Connecting to logger service...");

		try (Socket loggerServiceSocket = new Socket(ConstantsNetwork.LOGGER_SERVICE_HOST,
				ConstantsNetwork.LOGGER_SERVICE_PORT)) {

			// Prepare and send request
			RequestLoggerExecutionId invocationLogManagerRequest = UtilsSocketLogger.generateExecutionIdRequest();
			LOGGER.log(Level.INFO, "Sending request to logger service.");
			UtilsSocket.sendJsonObject(loggerServiceSocket.getOutputStream(), invocationLogManagerRequest);

			// Wait for response (wait for filtered resources)
			LOGGER.info("Waiting for response from logger service...");
			ResponseLogger invocationLogManagerResponse = UtilsSocket
					.receiveJsonObject(loggerServiceSocket.getInputStream(), ResponseLogger.class);
			int executionId = invocationLogManagerResponse.getExecutionId();

			// Close connection
			LOGGER.info("Closing connection to logger service...");

			// Check if logger service returned a valid execution identifier
			if (executionId == -1) {
				LOGGER.warning("Logger service returned an invalid executionId.");
				return -1;
			}

			// Return response
			LOGGER.log(Level.INFO, "Got response from logger service");
			return executionId;
		} catch (IOException e) {

			// Log error on failure
			LOGGER.severe("Could not get execution Id: " + e.getLocalizedMessage());
			return -1;
		}
		 */
		return -1;
	}


}
