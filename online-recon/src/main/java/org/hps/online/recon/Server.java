package org.hps.online.recon;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    
    private int port;
    
    final ExecutorService clientProcessingPool = Executors.newFixedThreadPool(10);
    
    public Server(int port) {
        this.port = port;
    }
    
    public Server() {        
    }
    
    public static void main(String args[]) {
        if (args.length < 1) {
            throw new RuntimeException("Not enough args");
        }
        int port = Integer.parseInt(args[0]);        
        Server server = new Server(port);
        server.start();
    }
    
    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                clientProcessingPool.submit(new ClientTask(clientSocket));
            }
        } catch (IOException e) {
            throw new RuntimeException("Server exception", e);
        }                       
    }    
    
    private class ClientTask implements Runnable {
        
        private final Socket socket;
        
        ClientTask(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            
            System.out.println("Got a client!");
                        
            try {
                Scanner in = new Scanner(socket.getInputStream());
                System.out.println("client said: " + in.nextLine());
                in.close();
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }            
        }        
    }
}
