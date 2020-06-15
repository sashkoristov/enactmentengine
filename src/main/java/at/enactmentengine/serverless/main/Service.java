package at.enactmentengine.serverless.main;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class Service {
	
	public static int port = 9898;
	public static boolean running = true;
	
	
	public static void main(String[] args) throws IOException {
		
		ServerSocket serverSocket = null;
        serverSocket = new ServerSocket(port);
        
        System.out.println("Server is up and running at " + InetAddress.getLocalHost().getHostAddress() + ":" + port);
        
        Socket socket = null;
        while(running) {
        	System.out.println("Waiting for client(s)...");
        	socket = serverSocket.accept();
        	
        	Thread handler = new Thread(new Handler(socket));
        	handler.start();
        	System.out.println("Handle client in thread " + handler.getId());
        }
        
        socket.close();    
        serverSocket.close();
	}
}
