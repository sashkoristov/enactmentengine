package at.enactmentengine.serverless.main;

import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Map;

public class Handler implements Runnable{

	Socket socket;
	
	public Handler(Socket socket) {
		this.socket = socket;
	}
	
	@Override
	public void run() {
		
		InputStream in = null;
        OutputStream out = null;

		try {
            in = socket.getInputStream();
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        try {
            out = new FileOutputStream(Thread.currentThread().getId() + ".yaml");                        
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        }

        byte[] bytes = new byte[16*1024];
        int count;
        try {
			while ((count = in.read(bytes)) >= 0) {
			    out.write(bytes, 0, count);
			    if(in.available() == 0) break;
			}
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
        
        Executor executor = new Executor();
        Map<String, Object> result = executor.executeWorkflow(Thread.currentThread().getId() + ".yaml");
        
        DataOutputStream dOut;
		try {
			dOut = new DataOutputStream(socket.getOutputStream());
		    dOut.writeUTF(result.toString());
		    dOut.flush();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
        
        try {
			out.close();
            in.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
