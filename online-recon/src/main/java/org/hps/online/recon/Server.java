package org.hps.online.recon;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Timer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.hps.online.recon.StationManager.StationInfo;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Server for managing instances of the online reconstruction.
 * 
 * @author jeremym
 */
public final class Server {        
       
    private static final long MILLIS_TO_SECONDS = 1000L;

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
                    if (command.equals("create")) {
                        handler = new CreateCommandHandler();
                    } else if (command.equals("start")) {
                        handler = new StartCommandHandler();
                    } else if (command.equals("stop")) {
                        handler = new StopCommandHandler();
                    } else if (command.equals("list")) {
                        handler = new ListCommandHandler();
                    } else if (command.equals("config")) {
                        handler = new ConfigCommandHandler();
                    } else if (command.equals("remove")) {
                        handler = new RemoveCommandHandler();
                    }
                    
                    if (handler != null) {
                        res = handler.execute(params);
                    } else {
                        res = new CommandStatus(STATUS_ERROR, "Unknown command: " + command);
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

    /**
     * Handler for a client command on the server.
     */
    abstract class CommandHandler {                   
               
        abstract CommandResult execute(JSONObject jo);
    }

    /**
     * Generic class for returning command results.
     */
    abstract class CommandResult {
    }
       
    /**
     * Return a result which describes result of command execution
     * i.e. success or failure.
     */
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
    
    /**
     * Return a generic result with an object which can be converted to
     * a JSON string.
     */
    class GenericResult extends CommandResult {
        
        final Object o;
        
        GenericResult(Object o) {
            this.o = o;
        }
        
        public String toString() {
            return o.toString();
        }        
    }
    
    /**
     * Return a JSON object.
     */
    class JSONResult extends CommandResult {
        
        final JSONObject jo;
        
        JSONResult(JSONObject jo) {
            this.jo = jo;
        }
                
        public String toString() {
            return jo.toString();
        }        
    }
            
    class ListCommandHandler extends CommandHandler {

        CommandResult execute(JSONObject parameters) {
            CommandResult res = null;
            List<Integer> ids = new ArrayList<Integer>();
            if (parameters.has("ids")) {
                JSONArray arr = parameters.getJSONArray("ids");
                for (int i = 0; i < arr.length(); i++) {
                    ids.add(arr.getInt(i));
                }
            }
            JSONArray arr = new JSONArray();
            if (ids.size() == 0) {
                // Return info on all stations.
                for (StationInfo station : stationManager.getStations()) {
                    JSONObject jo = station.toJSON();
                    arr.put(jo);
                }                
            } else {
                // Return info on selected station IDs.
                for (Integer id : ids) {
                    StationInfo station = stationManager.find(id);
                    if (station == null) {                               
                        // One of the station IDs is invalid.  Just return message about the first bad one.
                        res = new CommandStatus(STATUS_ERROR, "Station with this ID does not exist: " + id);
                        break;
                    } else {
                        JSONObject jo = station.toJSON();
                        arr.put(jo);
                    }
                }
            }
            if (res == null) {
                res = new GenericResult(arr);
            }
            return res;
        }
        
    }
    
    class CreateCommandHandler extends CommandHandler {
        CommandResult execute(JSONObject parameters) {
            CommandStatus res = null;
            int count = 1;
            boolean start = false;
            if (parameters.has("count")) {
                count = parameters.getInt("count");
            }
            LOGGER.info("Creating stations: " + count);
            if (parameters.has("start")) {
                start = parameters.getBoolean("start");
                LOGGER.info("Stations will be automatically started: " + start);
            }
            int started = 0;
            int created = 0;
            for (int i = 0; i < count; i++) {
                LOGGER.info("Creating station number: " + i);
                StationInfo station = Server.this.stationManager.create(parameters);
                LOGGER.info("Created station: " + station.stationName);
                ++created;
                if (start) {
                    LOGGER.info("Starting station: " + station.stationName);
                    try {
                        Server.this.stationManager.start(station);
                        ++started;
                    } catch (IOException e) {
                        LOGGER.log(Level.SEVERE, "Station failed to start.", e);
                    }
                }
            }
            LOGGER.info("Created " + created + " stations.");
            if (start) {
                LOGGER.info("Started " + started + " stations.");
                if (started < count) {
                    res = new CommandStatus(STATUS_SUCCESS, "Some stations failed to start.");
                } else {
                    res = new CommandStatus(STATUS_SUCCESS, "Created and started " + started + " stations successfully.");
                }
            } else {
                res = new CommandStatus(STATUS_SUCCESS, "Created " + created + " stations successfully.");
            }
            return res;
        }
    }
    
    class StartCommandHandler extends CommandHandler {
        CommandResult execute(JSONObject parameters) {
            CommandResult res = null;
            List<Integer> ids = new ArrayList<Integer>();
            if (parameters.has("ids")) {
                JSONArray arr = parameters.getJSONArray("ids");
                for (int i = 0; i < arr.length(); i++) {
                    ids.add(arr.getInt(i));
                }
            }            
            int started = 0;            
            if (ids.size() == 0) {
                int inactive = Server.this.getStationManager().getInactiveCount();
                LOGGER.info("Attepting to start " + inactive + " inactive stations.");
                started = Server.this.getStationManager().startAll();
                if (started < inactive) {
                    res = new CommandStatus(STATUS_ERROR, "Failed to start some stations.");
                } else {
                    res = new CommandStatus(STATUS_ERROR, "Started all stations successfull.");
                }
            } else {
                started = Server.this.getStationManager().start(ids);
                if (started < ids.size()) {
                    res = new CommandStatus(STATUS_ERROR, "Failed to start some stations.");
                } else {
                    res = new CommandStatus(STATUS_ERROR, "Started " + ids.size() + " stations successfully.");
                }
            }
           
            return res;
        }
    }
    
    class StopCommandHandler extends CommandHandler {
        CommandResult execute(JSONObject parameters) {
            CommandResult res = null;
            List<Integer> ids = new ArrayList<Integer>();
            if (parameters.has("ids")) {
                JSONArray arr = parameters.getJSONArray("ids");
                for (int i = 0; i < arr.length(); i++) {
                    ids.add(arr.getInt(i));
                }
            }
            if (ids.size() == 0) {
                LOGGER.info("Stopping all stations!");
                int nalive = Server.this.getStationManager().getAliveCount();
                int nstopped = Server.this.getStationManager().stopAll();
                if (nstopped < nalive) {
                    res = new CommandStatus(STATUS_ERROR, "Failed to stop at least one station.");
                } else {
                    res = new CommandStatus(STATUS_SUCCESS, "Stopped all stations.");
                }
            } else {
                LOGGER.info("Stopping stations: " + ids.toString());
                int n = Server.this.getStationManager().stop(ids);
                if (n < ids.size()) {
                    res = new CommandStatus(STATUS_ERROR, "Failed to stop at least one station.");
                } else {
                    res = new CommandStatus(STATUS_SUCCESS, "Removed stations: " + ids.toString());
                }
            }            
            return res;
        }
    }
    
    class ConfigCommandHandler extends CommandHandler {
        CommandResult execute(JSONObject parameters) {
            CommandResult res = null;
            if (parameters.length() == 0) {
                LOGGER.info("Returning existing station config.");
                res = new JSONResult(Server.this.getStationConfig().toJSON());
            } else {
                LOGGER.config("Loading new station config: " + parameters.toString());
                Server.this.getStationConfig().fromJSON(parameters);
                LOGGER.info("New config loaded.");
                res = new CommandStatus(STATUS_SUCCESS, "Loaded new station config. Create a new station to use it.");
            }
            return res;
        }
    }
    
    class RemoveCommandHandler extends CommandHandler {
        CommandResult execute(JSONObject parameters) {
            CommandResult res = null;
            List<Integer> ids = new ArrayList<Integer>();
            if (parameters.has("ids")) {
                JSONArray arr = parameters.getJSONArray("ids");
                for (int i = 0; i < arr.length(); i++) {
                    ids.add(arr.getInt(i));
                }
            }
            if (ids.size() == 0) {
                LOGGER.info("Removing all stations!");               
                Server.this.getStationManager().removeAll();
                if (Server.this.getStationManager().getStationCount() > 0) {
                    res = new CommandStatus(STATUS_ERROR, "Failed to remove at least one station.");
                } else {
                    res = new CommandStatus(STATUS_SUCCESS, "Removed all stations.");
                }
            } else {
                LOGGER.info("Removing stations: " + ids.toString());
                int n = Server.this.getStationManager().remove(ids);
                if (n < ids.size()) {
                    res = new CommandStatus(STATUS_ERROR, "Failed to remove at least one station.");
                } else {
                    res = new CommandStatus(STATUS_SUCCESS, "Removed stations: " + ids.toString());
                }
            }            
            return res;
        }
    }
    
    private static final int DEFAULT_PORT = 22222;
        
    static Logger LOGGER = Logger.getLogger(Server.class.getPackageName());    
    private static final int MAX_PORT = 49152;
    
    private static final int MIN_PORT = 1024;
    
    public static final String STATUS_ERROR = "ERROR";
    public static final String STATUS_SUCCESS = "SUCCESS";
        
    public static void main(String args[]) { 
        Server server = new Server();
        try {
            server.parse(args);
        } catch (ParseException e) {
            throw new RuntimeException("Error parsing command line", e);
        }        
        server.start();
    }
    
    final ExecutorService clientProcessingPool = Executors.newFixedThreadPool(10);
            
    private final String DEFAULT_WORK_PATH = System.getProperty("user.dir");
    
    private File plotFile;
    private Long plotIntervalMillis = (long) (60 * 1000);
    private Timer plotTimer = new Timer();
    
    private int port = DEFAULT_PORT;
        
    private String stationBase = "HPS_RECON";
        
    private StationConfiguration stationConfig = new StationConfiguration();
        
    private final StationManager stationManager;
    
    private File workDir;
    
    private String workPath = DEFAULT_WORK_PATH;    
  
    private boolean dryRun = false;
    
    public Server() {        
        this.stationManager = new StationManager(this);
    }
        
    File createProcessDir(String name) {
        String path = this.workDir.getPath() + File.separator + name;
        File dir = new File(path);
        dir.mkdir();
        if (!dir.isDirectory()) {
            throw new RuntimeException("Error creating dir: " + dir.getPath());
        }
        return dir;
    }
            
    String getStationBaseName() {
        return this.stationBase;
    }
        
    StationConfiguration getStationConfig() {
        return this.stationConfig;
    }
    
    StationManager getStationManager() {
        return this.stationManager;
    }
    
    File getWorkDir() {
        return this.workDir;
    }
    
    void parse(String args[]) throws ParseException {
        Options options = new Options();
        options.addOption(new Option("h", "help", false, "print help"));
        options.addOption(new Option("p", "port", true, "server port"));
        options.addOption(new Option("s", "start", true, "starting station ID (default 1)"));
        options.addOption(new Option("w", "workdir", true, "work dir (default is current dir where server is started)"));
        options.addOption(new Option("b", "basename", true, "station base name"));
        options.addOption(new Option("c", "config", true, "config properties file"));
        options.addOption(new Option("a", "add-plots", true, "output file for adding plots"));
        options.addOption(new Option("i", "interval", true, "update interval in seconds for adding plots (default is every 1 minute)"));
        options.addOption(new Option("D", "dry-run", false, "run plot task in dry run mode (plots will not be added or deleted)"));
        
        final CommandLineParser parser = new DefaultParser();
        CommandLine cl = parser.parse(options, args);
        
        // Print help and exit!
        if (cl.hasOption("h")) {
            final HelpFormatter help = new HelpFormatter();
            help.printHelp("Server", 
                    "Start the online reconstruction server",
                    options, "");
            System.exit(0);
        }
        
        // Port number of ET server.
        if (cl.hasOption("p")) {
            this.port = Integer.parseInt(cl.getOptionValue("p"));
        }
        if (this.port < MIN_PORT || this.port >= MAX_PORT) {
            LOGGER.severe("Port number <" + this.port + "> is not between " + MIN_PORT + " and " + MAX_PORT);
            throw new RuntimeException("Port number is not allowed: " + this.port);
        }
        LOGGER.config("Server port: " + this.port);
        
        // Starting station ID.
        if (cl.hasOption("s")) {
            int processID = Integer.parseInt(cl.getOptionValue("s"));
            this.stationManager.setStationID(processID);
            LOGGER.config("Starting station ID: " + processID);
        }
        
        // Base work directory for creating station directories.
        if (cl.hasOption("w")) {
            this.workPath = cl.getOptionValue("w");
        }                
        this.workDir = new File(this.workPath);
        LOGGER.config("Server work dir: " + this.workDir);
        if (!this.workDir.exists()) {
            throw new RuntimeException("Work dir does not exist: " + this.workPath);
        }
        if (!this.workDir.isDirectory()) {
            throw new RuntimeException("Work path is not a directory: " + this.workPath);
        }
        if (!this.workDir.canWrite()) {
            throw new RuntimeException("Work dir is not writable: " + this.workPath);
        }
        
        // Base name for station to which will be appended the station ID.
        if (cl.hasOption("b")) {
            this.stationBase = cl.getOptionValue("b");
        }
        LOGGER.config("Station base name: " + this.stationBase);

        // Output file for combined plots.
        if (cl.hasOption("a")) {
            this.plotFile = new File(cl.getOptionValue("a"));
            LOGGER.config("Plot file set to: " + this.plotFile.toPath());
        }
        
        // Plot update interval in seconds.
        if (cl.hasOption("i")) {
            this.plotIntervalMillis = Long.valueOf(cl.getOptionValue("i")) * 1000L;
            LOGGER.config("Plot interval set to " + this.plotIntervalMillis + " ms (" + this.plotIntervalMillis / MILLIS_TO_SECONDS + " seconds).");
        }
        
        // Load station configuration properties.
        if (cl.hasOption("c")) {
            File configFile = new File(cl.getOptionValue("c"));
            if (!configFile.exists()) {
                throw new RuntimeException("Config file does not exist: " + configFile.getPath());
            }
            this.stationConfig.load(configFile);
        }
        
        if (cl.hasOption("D")) {
            this.dryRun = true;
        }
    }
        
    void start() {
        
        // Schedule the plot task.
        if (this.plotFile != null) {
            LOGGER.info("Starting plot add task with output file " + this.plotFile + " and update interval of " + this.plotIntervalMillis + " ms.");
            this.plotTimer.schedule(new PlotAddTask(this, this.plotFile, dryRun), this.plotIntervalMillis, this.plotIntervalMillis);
        } else {
            LOGGER.info("Plot task was not enabled (no plot file was provided with -a option).");
        }
        
        // Start the server.
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
