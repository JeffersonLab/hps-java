package org.hps.online.recon;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import org.hps.online.recon.ProcessManager.ProcessInfo;
import org.json.JSONArray;
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
                 
                String command = jo.getString("command");
                JSONObject params = jo.getJSONObject("parameters");
                LOGGER.info("Received client command <" + command + "> with parameters " + params);
                
                CommandResult res = null;
                try {
                    CommandHandler handler = null;
                    // TODO: lookup handlers in a static map
                    if (command.equals("start")) {
                        handler = new StartCommandHandler();
                    } else if (command.equals("stop")) {
                        handler = new StopCommandHandler();
                    } else if (command.equals("list")) {
                        handler = new ListCommandHandler();
                    }
                    
                    if (handler != null) {
                        res = handler.execute(params);
                    } else {
                        res = new CommandStatus(STATUS_ERROR, "Unknown command <" + command + ">");
                    }
                } catch (Exception e) {                   
                    e.printStackTrace();
                    res = new CommandStatus(STATUS_ERROR, e.getMessage());
                }
                                
                // Send command result back to client.
                OutputStream os = socket.getOutputStream();
                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os));
                bw.write(res.toString());
                bw.flush();
                bw.close();
                
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
    
    abstract class CommandResult {
    }
       
    class CommandStatus extends CommandResult {
        
        String message;
        String status;
        
        CommandStatus(String status, String message) {
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
    
    class JSONObjectResult extends CommandResult {
        
        final JSONObject jo;
        
        JSONObjectResult(JSONObject jo) {
            this.jo = jo;
        }
                
        public String toString() {
            return jo.toString();
        }        
    }
            
    class StartCommandHandler extends CommandHandler {
        CommandResult execute(JSONObject parameters) {
            CommandStatus res = null;
            if (!parameters.has("properties")) {
                throw new RuntimeException("No properties file found in parameters.");
            }
            int count = 1;
            if (parameters.has("count")) {
                count = parameters.getInt("count");
            }
            try {                 
                LOGGER.info("Starting <" + count + "> processes");
                for (int i = 0; i < count; i++) {
                    LOGGER.info("Creating process <" + i + ">");
                    Server.this.processManager.createProcess(parameters);
                }
                res = new CommandStatus(STATUS_SUCCESS, "New process started successfully.");
            } catch (IOException e) {
                e.printStackTrace();
                res = new CommandStatus(STATUS_ERROR, "Error creating new process.");
            }
            return res;
        }
    }
    
    class StopCommandHandler extends CommandHandler {
        CommandResult execute(JSONObject parameters) {
            CommandStatus res = null;
            int id = -1;
            if (parameters.has("id")) {
                id = parameters.getInt("id");
            }
            if (id != -1) {
                processManager.stopProcess(id);
                res = new CommandStatus(STATUS_SUCCESS, "Stopped process <" + id + ">");
            } else {
                processManager.stopAll();
                res = new CommandStatus(STATUS_SUCCESS, "Stopped all processes");
            }            
            return res;
        }
    }
    
    class ListCommandHandler extends CommandHandler {

        CommandResult execute(JSONObject parameters) {
            CommandResult res = null;
            int id = -1;
            if (parameters.has("id")) {
                id = parameters.getInt("id");
            }
            if (id != -1) {
                ProcessInfo info = processManager.find(id);
                if (info != null) {
                    res = new JSONObjectResult(info.toJSON());
                } else {
                    res = new CommandStatus(STATUS_ERROR, "Unknown process id <" + id + ">");
                }
            } else {
                JSONArray arr = new JSONArray();
                for (ProcessInfo info : processManager.getProcesses()) {
                    arr.put(info.toJSON());
                }
                JSONObject jo = new JSONObject();
                jo.put("list", arr);
                res = new JSONObjectResult(jo);
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
}
