package at.enactmentengine.serverless.main;

import com.google.gson.Gson;

import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class Handler implements Runnable {

    private Socket socket;

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

        byte[] bytes = new byte[16 * 1024];
        int count;
        try {
            assert in != null;
            while ((count = in.read(bytes)) >= 0) {
                assert out != null;
                out.write(bytes, 0, count);
                if (in.available() == 0) {
                    break;
                }
            }
            assert out != null;
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Executor executor = new Executor();
        Map<String, Object> result = new HashMap<>();
        result.put("wfResult", executor.executeWorkflow(Thread.currentThread().getId() + ".yaml"));

        DataOutputStream dOut;
        try {
            dOut = new DataOutputStream(socket.getOutputStream());
            dOut.writeUTF(new Gson().toJson(result));
            dOut.flush();
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        try {
            in.close();
            assert out != null;
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
