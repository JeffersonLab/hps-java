package org.hps.online.recon;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import org.json.JSONObject;

/**
 * Server for managing instances of the online reconstruction.
 */
public class Server {
    
    private class ClientTask implements Runnable {
        
        private final Socket socket;
        
        ClientTask(Socket socket) {
            this.socket = socket;
        }

        public void run() {               
            try {
                Scanner in = new Scanner(socket.getInputStream());
                
                JSONObject jo = new JSONObject(in.nextLine());
                System.out.println("JSON from client: " + jo.toString());
                
                String command = jo.getString("command");
                System.out.println("command: " + command);
                JSONObject params = jo.getJSONObject("parameters");
                System.out.println("parameters: " + params);    
                
                CommandResult res = null;
                try {
                    if (command.equals("start")) {
                        res = new StartCommandHandler().execute(params);
                    } else {
                        res = new CommandResult(STATUS_ERROR, "Unknown command <" + command + ">");
                    }
                } catch (Exception e) {                   
                    e.printStackTrace();
                    res = new CommandResult(STATUS_ERROR, e.getMessage());
                }
                
                System.out.println("result: " + res.toString());
                
                // TODO: send back res to client
                
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    abstract class CommandHandler {
        abstract CommandResult execute(JSONObject jo);
    }
       
    class CommandResult {
        
        String message;
        String status;
        
        CommandResult(String status, String message) {
            this.status = status;
            this.message = message;
        }
        
        JSONObject toJSON() {
            JSONObject jo = new JSONObject();
            jo.put("status", status);
            jo.put("message", message);
            return jo;
        }
        
        public String toString() {
            return toJSON().toString();
        }
    }
    
    class StartCommandHandler extends CommandHandler {
        CommandResult execute(JSONObject parameters) {
            CommandResult res = null;
            if (!parameters.has("properties")) {
                throw new RuntimeException("No properties file found in parameters.");
            }
            try {                                
                Server.this.processManager.createProcess(parameters);
                res = new CommandResult(STATUS_SUCCESS, "New process started successfully.");
            } catch (IOException e) {
                e.printStackTrace();
                res = new CommandResult(STATUS_ERROR, "Error creating new process.");
            }
            return res;
        }
    }
    
    static Logger LOGGER = Logger.getLogger(Server.class.getPackageName());
        
    public static final String STATUS_ERROR = "ERROR";    
    public static final String STATUS_SUCCESS = "SUCCESS";
    
    public static void main(String args[]) {
        if (args.length < 1) {
            throw new RuntimeException("Not enough args");
        }
        int port = Integer.parseInt(args[0]);        
        Server server = new Server(port);
        server.start();
    }
            
    final ExecutorService clientProcessingPool = Executors.newFixedThreadPool(10);
    
    private int port;
    
    private ProcessManager processManager;
        
    private String stationBase = "HPS_RECON";
        
    private File workDir = null;
    
    private String workPath = System.getProperty("user.dir");
    
    public Server() {        
        this.processManager = new ProcessManager(this);
    }
    
    public Server(int port) {
        this.port = port;       
        this.processManager = new ProcessManager(this);
    }
        
    File createProcessDir(String name) {
        String path = this.workDir.getPath() + File.separator + name;
        File dir = new File(path);
        dir.mkdir();
        if (!dir.isDirectory()) {
            throw new RuntimeException("Error creating dir <" + dir.getPath() + ">");
        }
        return dir;
    }
        
    public String getStationBaseName() {
        return this.stationBase;
    }
    
    public File getWorkDir() {
        return this.workDir;
    }
    
    private void setupWorkDir() {
        this.workDir = new File(this.workPath);
        if (!this.workDir.exists()) {
            throw new RuntimeException("Work dir does not exist <" + this.workPath + ">");
        }
        if (!this.workDir.isDirectory()) {
            throw new RuntimeException("Work path is not a directory <" + this.workPath + ">");
        }
        if (!this.workDir.canWrite()) {
            throw new RuntimeException("Work dir <" + this.workPath + "> is not writable.");
        }
    }
    
    public void start() {
        
        setupWorkDir();        
        
        LOGGER.info("Starting server on port <" + this.port + ">");
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                clientProcessingPool.submit(new ClientTask(clientSocket));
            }
        } catch (IOException e) {
            throw new RuntimeException("Server exception", e);
        }                       
    }
        
    /*
    String line;
    BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
    while ((line = input.readLine()) != null) {
        System.out.println(line);
    }
    input.close();                
    */
}
