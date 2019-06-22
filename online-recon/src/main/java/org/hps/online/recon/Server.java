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
import org.jlab.coda.et.EtConstants;
import org.jlab.coda.et.EtStation;
import org.jlab.coda.et.EtSystem;
import org.jlab.coda.et.EtSystemOpenConfig;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Server for managing instances of the online reconstruction.
 * 
 * Accepts commands from the Client class and sends back a JSON result.
 * 
 * @author jeremym
 */
public final class Server {        
       
    /**
     * Conversion of milliseconds to seconds.
     */
    private static final long MILLIS_TO_SECONDS = 1000L;

    /**
     * Handles a single client request.
     */
    private class ClientTask implements Runnable {
        
        private final Socket socket;
        
        ClientTask(Socket socket) {
            this.socket = socket;
        }

        /**
         * Handle client request by dispatching to a command handler.
         */
        public void run() {               
            try {
                
                LOGGER.fine("Got new connection from: " + socket.getInetAddress().getHostName());
                
                Scanner in = new Scanner(socket.getInputStream());                                            
                JSONObject jo = new JSONObject(in.nextLine());
                 
                String command = jo.getString("command");
                JSONObject params = jo.getJSONObject("parameters");
                
                LOGGER.info("Received client command <" + command + "> with parameters " + params);
                
                CommandResult res = null;

                // Find the handler for the client command.
                CommandHandler handler = getCommandHandler(command);
                if (handler == null) {
                    // Command name is invalid. This shouldn't happen normally.
                    res = new CommandStatus(STATUS_ERROR, "Unknown command: " + command);
                } else {
                    try {
                        // Execute the command and get its result.
                        LOGGER.info("Executing command: " + command);
                        res = handler.execute(params);
                    } catch (Exception e) {
                        // Some kind of error occurred executing the command.
                        LOGGER.log(Level.SEVERE, "Error executing command: " + command, e);
                        res = new CommandStatus(STATUS_ERROR, e.getMessage());
                    }
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
        
        CommandHandler getCommandHandler(String command) {
            CommandHandler handler = null;
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
            } else if (command.equals("cleanup")) {
                handler = new CleanupCommandHandler();
            } else if (command.equals("settings")) {
                handler = new SettingsCommandHandler();
            } else if (command.equals("status")) {
                handler = new StatusCommandHandler();
            }
            return handler;
        }
    }

    /**
     * Handler for a client command on the server.
     */
    abstract class CommandHandler {                   
               
        /**
         * Execute the command.
         * @param jo The JSON input parameters
         * @return The command result
         */
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
            
    /**
     * Handle the list command.
     */
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
                    stationManager.update(station);
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
                        stationManager.update(station);
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
    
    /**
     * Handle the create command.
     */
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
                        LOGGER.log(Level.SEVERE, "Station " + station.stationName + " failed to start.", e);
                    }
                }
            }
            LOGGER.info("Created " + created + " stations.");
            if (start) {
                LOGGER.info("Started " + started + " stations.");
                if (started < count) {
                    res = new CommandStatus(STATUS_ERROR, "Some stations failed to start.");
                } else {
                    res = new CommandStatus(STATUS_SUCCESS, "Created and started " + started + " stations successfully.");
                }
            } else {
                res = new CommandStatus(STATUS_SUCCESS, "Created " + created + " stations successfully.");
            }
            return res;
        }
    }
    
    /**
     * Handle the start command.
     */
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
                    res = new CommandStatus(STATUS_SUCCESS, "Started all stations successfull.");
                }
            } else {
                started = Server.this.getStationManager().start(ids);
                if (started < ids.size()) {
                    res = new CommandStatus(STATUS_ERROR, "Failed to start some stations.");
                } else {
                    res = new CommandStatus(STATUS_SUCCESS, "Started " + ids.size() + " stations successfully.");
                }
            }
           
            return res;
        }
    }
    
    /**
     * Handle the stop command.
     */
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
                int nactive = Server.this.getStationManager().getActiveCount();
                int nstopped = Server.this.getStationManager().stopAll();
                if (nstopped < nactive) {
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
                    res = new CommandStatus(STATUS_SUCCESS, "Stopped stations: " + ids.toString());
                }
            }            
            return res;
        }
    }
    
    /**
     * Handle the config command.
     */
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
    
    /**
     * Handle the remove command.
     */
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
    
    /**
     * Handle the cleanup command.
     */
    class CleanupCommandHandler extends CommandHandler {
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
                LOGGER.info("Cleaning up all inactive stations!");
                int inactive = Server.this.stationManager.getInactiveCount();
                int cleaned = Server.this.stationManager.cleanupAll();
                if (cleaned < inactive) {
                    res = new CommandStatus(STATUS_ERROR, "Failed to cleanup at least one station."); 
                } else {
                    res = new CommandStatus(STATUS_SUCCESS, "Cleaned up " + cleaned + " stations.");
                }
            } else {
                LOGGER.info("Cleaning up stations: " + ids.toString());
                int cleaned = Server.this.stationManager.cleanup(ids);
                if (cleaned < ids.size()) {
                    res = new CommandStatus(STATUS_ERROR, "Failed to cleanup at least one station.");
                } else {
                    res = new CommandStatus(STATUS_SUCCESS, "Cleaned up " + cleaned + " stations.");
                }                
            }            
            return res;
        }
    }
    
    /**
     * Handle the settings command.
     */
    class SettingsCommandHandler extends CommandHandler {
        CommandResult execute(JSONObject parameters) {
            CommandResult res = null;
            StationManager mgr = Server.this.getStationManager();
            boolean error = false;
            if (parameters.length() > 0) {
                if (parameters.has("start")) {
                    int startID = parameters.getInt("start");            
                    try {
                        Server.this.getStationManager().setStationID(startID); 
                        LOGGER.config("Set new station start ID: " + mgr.getCurrentStationID());
                    } catch (IllegalArgumentException e) {                        
                        LOGGER.log(Level.SEVERE, "Failed to set new station ID", e);
                        error = true;
                    }
                }
                if (parameters.has("workdir")) {
                    File newWorkDir = new File(parameters.getString("workdir"));
                    try {
                        Server.this.setWorkDir(newWorkDir);
                    } catch (Exception e) {
                        LOGGER.log(Level.SEVERE, "Failed to set new work dir: " + newWorkDir.getPath(), e);
                        error = true;
                    }
                }
                if (parameters.has("basename")) {
                    String stationBase = parameters.getString("basename");
                    try {
                        Server.this.setStationBaseName(stationBase);
                    } catch (IllegalArgumentException e) {
                        LOGGER.log(Level.SEVERE, "Failed to set station base name: " + stationBase, e);
                        error = true;
                    }
                }            
                if (error) {
                    res = new CommandStatus(STATUS_ERROR, "At least one setting failed to update (see server log).");
                } else {
                    res = new CommandStatus(STATUS_SUCCESS, "All settings updated successfully.");
                }
            } else {
                JSONObject jo = new JSONObject();
                jo.put("start", mgr.getCurrentStationID());
                jo.put("workdir", Server.this.getWorkDir());
                jo.put("basename", Server.this.getStationBaseName());
                res = new JSONResult(jo);
            }
                
            return res;
        }
    }
    
    /**
     * Handle status command. 
     */
    class StatusCommandHandler extends CommandHandler {

        CommandResult execute(JSONObject jo) {

            boolean verbose = false;
            if (jo.has("verbose")) {
                verbose = jo.getBoolean("verbose");
            }
            
            JSONObject res = new JSONObject();
            
            // Update active status of all stations.
            StationManager mgr = Server.this.getStationManager();
            mgr.updateAll();
            
            // Put station status counts.
            JSONObject stationRes = new JSONObject();
            stationRes.put("total", mgr.getStationCount());
            stationRes.put("active", mgr.getActiveCount());
            stationRes.put("inactive", mgr.getInactiveCount());
            res.put("stations", stationRes);
            
            // Put ET system status.
            JSONObject etRes = new JSONObject();
            getEtStatus(etRes, verbose);
            res.put("ET", etRes);
            
            return new JSONResult(res);
        }
    }
    
    /**
     * Get the status and state of the ET system.
     * @param jo The JSON object to update with status
     */
    synchronized private void getEtStatus(JSONObject jo, boolean verbose) {
        EtSystem sys = null;
        try {
            EtSystemOpenConfig etConfig = new EtSystemOpenConfig(this.stationConfig.getBufferName(),
                    this.stationConfig.getHost(), this.stationConfig.getPort());
            sys = new EtSystem(etConfig, EtConstants.debugWarn);
            sys.open();
            jo.put("alive", sys.alive());
            jo.put("host", etConfig.getHost());
            jo.put("port", etConfig.getTcpPort());
            if (sys.alive()) {
                jo.put("pid", sys.getPid());
                jo.put("num_stations", sys.getNumStations());
                jo.put("attachments", sys.getNumAttachments());
                jo.put("attachments_max", sys.getAttachmentsMax());
            }            
            if (verbose) {
                JSONObject joRes = new JSONObject();
                final List<StationInfo> stations = 
                        Server.this.getStationManager().getStations();
                for (StationInfo station : stations) {
                    if (sys.stationExists(station.stationName)) {
                        EtStation etStation = sys.stationNameToObject(station.stationName);                        
                        JSONObject joStat = new JSONObject();
                        joStat.put("usable", etStation.isUsable());
                        joStat.put("input_count", etStation.getInputCount());
                        joStat.put("status", getStatusString(etStation.getStatus()));
                        joStat.put("id", etStation.getId());
                        joRes.put(station.stationName, joStat);
                    }
                }
                jo.put("stations", joRes);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting ET status", e);
            jo.put("error", e.getMessage());
        } finally {
            if (sys != null) {                
                sys.close();
            }
        }
    }
    
    /**
     * Get ET station status from code.
     * @param status The status string from the code.
     * @return The station status string
     */
    static String getStatusString(int status) {
        String statusString = "unknown";
        if (status == EtConstants.stationUnused) {
            statusString = "unused";
        } else if (status == EtConstants.stationCreating) {
            statusString = "creating";
        } else if (status == EtConstants.stationIdle) {
            statusString = "idle";
        } else if (status == EtConstants.stationActive) {
            statusString = "active";
        }
        return statusString;
    }
            
    /**
     * The default server port.
     */
    private static final int DEFAULT_PORT = 22222;
        
    /**
     * The package logger.
     */
    static Logger LOGGER = Logger.getLogger(Server.class.getPackageName());    
    
    /**
     * Max allowed server port number.
     */
    private static final int MAX_PORT = 49152;
    
    /**
     * Minimum allowed server port number.
     */
    private static final int MIN_PORT = 1024;
    
    /**
     * Error status string.
     */
    public static final String STATUS_ERROR = "ERROR";
    
    /**
     * Success status string.
     */
    public static final String STATUS_SUCCESS = "SUCCESS";
        
    /**
     * Run from command line.
     * @param args The command line args
     */
    public static void main(String args[]) { 
        Server server = new Server();
        try {
            server.parse(args);
        } catch (ParseException e) {
            throw new RuntimeException("Error parsing command line", e);
        }        
        server.start();
    }
    
    /**
     * The pool of threads for processing client commands.
     */
    final ExecutorService clientProcessingPool = Executors.newFixedThreadPool(10);
            
    /**
     * Default work dir path (current working dir).
     */
    private final String DEFAULT_WORK_PATH = System.getProperty("user.dir");
    
    /**
     * Output plot file.
     */
    private File plotFile;
    
    /**
     * Default interval for running plot task.
     */
    private Long plotIntervalMillis = (long) (60 * 1000);
    
    /**
     * Timer for running plot task.
     */
    private Timer plotTimer = new Timer();
    
    /**
     * The server port number.
     */
    private int port = DEFAULT_PORT;
        
    /**
     * The station base name to which the ID will be appended.
     */
    private String stationBase = "HPS_RECON";
        
    /**
     * The station configuration.
     */
    private StationConfiguration stationConfig = new StationConfiguration();
        
    /**
     * The station manager.
     */
    private final StationManager stationManager;
    
    /**
     * The server work directory.
     */
    private File workDir;
      
    /**
     * Whether to enable dry run for plotting.
     */
    private boolean dryRun = false;
    
    /**
     * Create a new server instance.
     */
    Server() {
        this.stationManager = new StationManager(this);
    }
        
    /**
     * Create the work directory for a station.
     * @param name The name of the station
     * @return The File for the new directory
     */
    File createStationDir(String name) {
        String path = this.workDir.getPath() + File.separator + name;
        File dir = new File(path);
        dir.mkdir();
        if (!dir.isDirectory()) {
            throw new RuntimeException("Error creating dir: " + dir.getPath());
        }
        return dir;
    }

    /**
     * Get the string used for station base name.
     * @return The string used for station base name
     */
    String getStationBaseName() {
        return this.stationBase;
    }
    
    /**
     * Set the string for station base name.
     * @param stationBase The string for station base name
     */
    void setStationBaseName(String stationBase) {
        if (stationBase == null) {
            throw new IllegalArgumentException("The stationBase points to null.");
        }
        this.stationBase = stationBase;
    }
        
    /**
     * Get the current station configuration.
     * @return The current station configuration
     */
    StationConfiguration getStationConfig() {
        return this.stationConfig;
    }
    
    /**
     * Get the station manager.
     * @return The station manager
     */
    StationManager getStationManager() {
        return this.stationManager;
    }
    
    /**
     * Get the server's work directory.
     * @return The server's work directory
     */
    File getWorkDir() {
        return this.workDir;
    }
    
    /**
     * Check whether a work directory exists, is a directory, and is writable.
     * @param workDir The work directory to check
     */
    static void checkWorkDir(File workDir) {
        if (!workDir.exists()) {
            throw new RuntimeException("Work dir does not exist: " + workDir.getPath());
        }
        if (!workDir.isDirectory()) {
            throw new RuntimeException("Work dir is not a directory: " + workDir.getPath());
        }
        if (!workDir.canWrite()) {
            throw new RuntimeException("Work dir is not writable: " + workDir.getPath());
        }
    }
    
    /**
     * Set the server working directory.
     * @param workDir The new server working directory
     */
    void setWorkDir(File workDir) {
        checkWorkDir(workDir);
        this.workDir = workDir;
    }
    
    /**
     * Parse command line options.
     * @param args The command line arguments
     * @throws ParseException If there is a problem parsing the command line
     */
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
        String workPath = DEFAULT_WORK_PATH;
        if (cl.hasOption("w")) {
            workPath = cl.getOptionValue("w");
        }                  
        setWorkDir(new File(workPath));

        LOGGER.config("Server work dir: " + this.workDir.getPath());
        
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
        
    /**
     * Start the server.
     */
    void start() {
        
        // Schedule the plot task.
        if (this.plotFile != null) {
            LOGGER.info("Starting plot add task with output file " + this.plotFile + " and update interval of " + this.plotIntervalMillis + " ms.");
            this.plotTimer.schedule(new PlotAddTask(this, this.plotFile, dryRun), this.plotIntervalMillis, this.plotIntervalMillis);
        } else {
            LOGGER.info("Plot task was not enabled (no plot file was provided with -a option).");
        }
        
        // Start the server instance.
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
