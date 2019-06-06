package org.hps.online.recon;

import java.io.PrintWriter;
import java.net.Socket;

public class Client {

    private String hostname;
    private int port;
    
    public Client(String hostname, int port) {
        this.hostname = hostname;
        this.port = port;
    }
    
    void run() {
        try (Socket socket = new Socket(hostname, port)) {
            //DataInputStream is = new DataInputStream(socket.getInputStream());
            PrintWriter writer = new PrintWriter(socket.getOutputStream());
            writer.write("hey there\n");
            writer.flush();
            writer.close();
        } catch (Exception e) {
            throw new RuntimeException("Client error", e);
        }
    }
    
    public static void main(String[] args) {
        if (args.length < 2) {
            throw new RuntimeException("Not enough args");
        }
        String hostname = args[0];
        int port = Integer.parseInt(args[1]);
        Client client = new Client(hostname, port);
        client.run();
    }   
}
