package at.enactmentengine.serverless.main;

// import at.uibk.dps.socketutils.ConstantsNetwork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Start the enactment engine as a service.
 *
 * @author stefanpedratscher
 */
public class Service {

    /**
     * Determines if the enactment-engine service is running.
     */
    private static boolean running = true;

    /**
     * The logger fot the enactment-engine service class.
     */
    static final Logger logger = LoggerFactory.getLogger(Service.class);

    /**
     * Starting point of the service.
     *
     * @param args input arguments for the service.
     */
    public static void main(String[] args) {

        // Start the service
        /*
        try (ServerSocket serverSocket = new ServerSocket(ConstantsNetwork.EE_PORT)) {

            logger.info("Server is up and running at {}:{}", InetAddress.getLocalHost().getHostAddress(), ConstantsNetwork.EE_PORT);

            Socket socket = null;
            while (running) {
                logger.info("Waiting for client(s)...");
                socket = serverSocket.accept();

                Thread handler = new Thread(new Handler(socket));
                handler.start();
                logger.info("Handle client in thread {}", handler.getId());
            }

            assert socket != null;
            socket.close();
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
        */
    }
}
