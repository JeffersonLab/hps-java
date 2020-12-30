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
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListenerAdapter;
import org.hps.online.recon.StationManager.StationProcess;
import org.hps.online.recon.properties.Property;
import org.jlab.coda.et.EtConstants;
import org.jlab.coda.et.EtStation;
import org.jlab.coda.et.EtSystem;
import org.jlab.coda.et.EtSystemOpenConfig;
import org.jlab.coda.et.exception.EtException;
import org.jlab.coda.et.exception.EtTooManyException;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Server for managing reconstruction {@link Station} instances
 *
 * Accepts commands from the {@link Client} and sends back a JSON result.
 *
 * The server will fail to start if the ET system does not open, and it
 * will automatically shutdown if the ET connection goes down.
 */
public final class Server {

    /**
     * When streaming data, client sends to server to keep connection alive.
     */
    static final String KEEPALIVE_RESPONSE = "<KEEPALIVE>";

    /**
     * The default server port.
     */
    static final int DEFAULT_PORT = 22222;

    /**
     * The package logger.
     */
    static Logger LOG = Logger.getLogger(Server.class.getPackage().getName());

    /**
     * Max allowed server port number.
     */
    private static final int MAX_PORT = 65535;

    /**
     * Minimum allowed server port number.
     */
    private static final int MIN_PORT = 1024;

    /**
     * Error status string.
     */
    private static final String STATUS_ERROR = "ERROR";

    /**
     * Success status string.
     */
    private static final String STATUS_SUCCESS = "SUCCESS";

    /**
     * Warning status string.
     */
    private static final String STATUS_WARNING = "WARNING";

    /**
     * The pool of threads for processing client commands.
     */
    private final ExecutorService clientProcessingPool = Executors.newFixedThreadPool(10);

    /**
     * Default work dir path (current working dir by default).
     */
    private final String DEFAULT_WORK_PATH = System.getProperty("user.dir");

    /**
     * The server port number.
     */
    private int port = DEFAULT_PORT;

    /**
     * The station base name to which the ID will be appended.
     */
    private String stationBase = "HPS_RECON";

    /**
     * The station properties
     */
    private StationProperties stationProperties = new StationProperties();

    /**
     * The station manager.
     */
    private final StationManager stationManager;

    /**
     * The server work directory.
     */
    private File workDir;

    /**
     * Reference to active ET system
     */
    private EtSystem etSystem;

    /**
     * Reference to the current ET config
     */
    private EtSystemOpenConfig etConfig;

    /**
     * Executor for executing various tasks such as the ET and station monitors
     */
    ScheduledExecutorService exec = Executors.newScheduledThreadPool(3);

    /**
     * Remote AIDA plot aggregator
     */
    final InlineAggregator agg = new InlineAggregator();

    /**
     * Handles a single client request.
     */
    private class ClientTask implements Runnable {

        private final Socket socket;

        ClientTask(Socket socket) {
            this.socket = socket;
        }

        /**
         * Handle client request by dispatching to a command handler and
         * sending a response back to the client.
         */
        public void run() {
            try {

                LOG.fine("Got new connection from: " + socket.getInetAddress().getHostName());

                // Read input from client.
                Scanner in = new Scanner(socket.getInputStream());
                JSONObject jo = new JSONObject(in.nextLine());

                // Get the command and its parameters.
                String command = jo.getString("command");
                JSONObject params = jo.getJSONObject("parameters");
                LOG.info("Received client command <" + command + "> with parameters " + params);

                // Command result to send back to client.
                CommandResult res = null;

                // Find the handler for the command.
                CommandHandler handler = getCommandHandler(command);
                if (handler == null) {
                    // Command name is invalid. This shouldn't happen normally.
                    res = new CommandStatus(STATUS_ERROR, "Unknown command: " + command);
                } else {
                    try {
                        // Execute the command and get its result.
                        LOG.info("Executing command: " + command);
                        res = handler.execute(params);
                    } catch (Exception e) {
                        // Some kind of error occurred executing the command.
                        LOG.log(Level.SEVERE, "Error executing command: " + command, e);
                        res = new CommandStatus(STATUS_ERROR, e.getMessage());
                    }
                }

                // Setup writer to send data back to client.
                OutputStream os = socket.getOutputStream();
                final BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os));

                try {
                    if (res instanceof LogStreamResult) {
                        // Stream log file back to client.
                        runTailer(res, bw, in);
                    } else {
                        // Send single line command result back to client.
                        bw.write(res.toString());
                        bw.flush();
                    }
                } catch (Exception e) {
                    LOG.log(Level.SEVERE, "Error sending data to client", e);
                    e.printStackTrace();
                } finally {
                    // Close the writer.
                    try {
                        bw.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    // Close the input reader.
                    try {
                        in.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Error sending or receiving data", e);
                e.printStackTrace();
            } finally {
                // Close the socket.
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            LOG.fine("Done running client thread");
        }

        /**
         * Stream the tail of a log file back to the client.
         * @param res The CommandResult with the <code>Tailer</code> setup
         * @param bw The writer for output to client
         * @param in The reader for input from client
         * @throws IOException
         * @throws InterruptedException
         */
        // FIXME: Clean this up (can it be made less complicated?)
        private void runTailer(CommandResult res, final BufferedWriter bw, final Scanner in)
                throws IOException, InterruptedException {

            LOG.fine("Sending log tail back to client");

            LogStreamResult logStreamResult = (LogStreamResult) res;
            SimpleLogListener listener = logStreamResult.listener;
            listener.setBufferedWriter(bw);

            bw.write("--- " + logStreamResult.log.getPath() + " ---" + '\n');

            final Tailer tailer = logStreamResult.tailer;

            // Create a thread to stop the tailer if the client closes the connection.
            Thread clientCheckThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        while (true) {
                            // If the client stops sending keep alive responses, then
                            // this will fail eventually and an exception will be thrown.
                            in.nextLine();
                        }
                    } catch (Exception e) {
                        //e.printStackTrace();
                        tailer.stop();
                    }
                }
            });
            clientCheckThread.start();

            // Main thread will block here until client closes the connection.
            LOG.info("Running tailer");
            tailer.run();

            // Kill the thread checking the client, in case it is still alive.
            if (clientCheckThread.isAlive()) {
                LOG.fine("Killing client check thread");
                clientCheckThread.interrupt();
                clientCheckThread.join();
            }

            LOG.info("Done running tailer");
        }

        /**
         * Find a <code>CommandHandler</code> for the given command name.
         * @param command The command name
         * @return The <code>CommandHandler</code> or null if does not exist
         */
        CommandHandler getCommandHandler(String command) {
            // TODO: Setup a map for this command dispatch
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
            } else if (command.equals("log")) {
                handler = new LogCommandHandler();
            } else if (command.equals("prop-set")) {
                handler = new PropSetCommandHandler();
            }
            return handler;
        }
    }

    /**
     * Get a list of station IDs from JSON parameters.
     * @param parameters The JSON parameters
     * @return A list of station IDs
     */
    static List<Integer> getStationIDs(JSONObject parameters) {
        List<Integer> ids = new ArrayList<Integer>();
        if (parameters.has("ids")) {
            JSONArray arr = parameters.getJSONArray("ids");
            for (int i = 0; i < arr.length(); i++) {
                ids.add(arr.getInt(i));
            }
        }
        return ids;
    }

    /**
     * Server-side handler for a client command
     */
    abstract class CommandHandler {

        Server server = null;

        CommandHandler() {
        }

        CommandHandler(Server server) {
            this.server = server;
        }

        /**
         * Execute the command.
         * @param jo The JSON input parameters
         * @return The command result
         */
        abstract CommandResult execute(JSONObject jo) throws CommandException;

    }

    /**
     * Generic class for returning command results.
     */
    abstract class CommandResult {
    }

    /**
     * Connection that handlers can throw if there is a command
     * processing error
     */
    @SuppressWarnings("serial")
    class CommandException extends Exception {

        public CommandException(String msg) {
            super(msg);
        }

        public CommandException(String msg, Exception e) {
            super(msg, e);
        }
    }

    /**
     * Return a result which describes result of command execution
     * i.e. success or failure (JSON format).
     */
    public class CommandStatus extends CommandResult {

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
            List<Integer> ids = getStationIDs(parameters);
            JSONArray arr = new JSONArray();
            if (ids.size() == 0) {
                // Return info on all stations.
                for (StationProcess station : stationManager.getStations()) {
                    JSONObject jo = station.toJSON();
                    arr.put(jo);
                }
            } else {
                // Return info on selected station IDs.
                for (Integer id : ids) {
                    StationProcess station = stationManager.find(id);
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

    /**
     * Handle the create command.
     */
    class CreateCommandHandler extends CommandHandler {
        CommandResult execute(JSONObject parameters) throws CommandException {
            CommandStatus res = null;
            int count = 1;

            if (parameters.has("count")) {
                count = parameters.getInt("count");
            }
            LOG.info("Creating stations: " + count);

            boolean start = false;
            if (parameters.has("start")) {
                start = parameters.getBoolean("start");
                LOG.info("Stations will be automatically started: " + start);
            }

            // Create the stations
            List<StationProcess> stats = new ArrayList<StationProcess>();
            for (int i = 0; i < count; i++) {
                try {
                    LOG.info("Creating station " + (i + 1) + " of " + count);
                    StationProcess station = Server.this.stationManager.create(parameters);
                    stats.add(station);
                    LOG.info("Created station: " + station.stationName);
                } catch (Exception e) {
                    LOG.log(Level.WARNING, "Error creating station: " + i, e);
                }
            }
            LOG.info("Number of stations created: " + stats.size());

            // Start the stations
            int started = 0;
            if (start) {
                for (StationProcess sp : stats) {
                    LOG.info("Starting station: " + sp.stationName);
                    try {
                        Server.this.stationManager.start(sp);
                        ++started;
                    } catch (IOException e) {
                        LOG.log(Level.SEVERE, "Station " + sp.stationName + " failed to start.", e);
                    }
                }
                LOG.info("Started " + started + " stations.");
                if (started < count) {
                    res = new CommandStatus(STATUS_ERROR, "Some stations failed to start.");
                } else {
                    res = new CommandStatus(STATUS_SUCCESS, "Created and started all stations successfully.");
                }
            } else {
                if (stats.size() == count) {
                    res = new CommandStatus(STATUS_SUCCESS, "Created all stations successfully");
                } else {
                    res = new CommandStatus(STATUS_ERROR, "Could not create some stations.");
                }
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
            List<Integer> ids = getStationIDs(parameters);
            int started = 0;
            if (ids.size() == 0) {
                int inactive = Server.this.getStationManager().getInactiveCount();
                LOG.info("Attepting to start " + inactive + " inactive stations.");
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
            if (started == 0) {
                res = new CommandStatus(STATUS_WARNING, "No stations were started.");
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
            List<Integer> ids = getStationIDs(parameters);
            int stopped = 0;
            if (ids.size() == 0) {
                int nactive = Server.this.getStationManager().getActiveCount();
                stopped = Server.this.getStationManager().stopAll();
                if (stopped < nactive) {
                    res = new CommandStatus(STATUS_ERROR, "Failed to stop at least one station.");
                } else {
                    res = new CommandStatus(STATUS_SUCCESS, "Stopped all stations.");
                }
            } else {
                LOG.info("Stopping stations: " + ids.toString());
                stopped = Server.this.getStationManager().stop(ids);
                if (stopped < ids.size()) {
                    res = new CommandStatus(STATUS_ERROR, "Failed to stop at least one station.");
                } else {
                    res = new CommandStatus(STATUS_SUCCESS, "Stopped stations: " + ids.toString());
                }
            }
            if (stopped == 0) {
                res = new CommandStatus(STATUS_WARNING, "No stations were stopped.");
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
                LOG.info("Returning existing station config.");
                res = new JSONResult(Server.this.getStationProperties().toJSON());
            } else {
                LOG.config("Loading new station config: " + parameters.toString());
                Server.this.getStationProperties().fromJSON(parameters);
                LOG.info("New config loaded.");
                res = new CommandStatus(STATUS_SUCCESS, "Loaded new station config. Create a new station to use it.");
            }
            return res;
        }
    }

    /**
     * Handle the <code>prop-set</code> command.
     *
     */
    class PropSetCommandHandler extends CommandHandler {
        CommandResult execute(JSONObject parameters) throws CommandException {
            String name = parameters.getString("name");
            String value = parameters.getString("value");
            StationProperties statProp = getStationProperties();
            if (statProp.has(name)) {
                statProp.get(name).set(value);
                LOG.info("Set prop: " + name + "=" + value);
            } else {
                throw new CommandException("Property does not exist: " + name);
            }
            return new CommandStatus(STATUS_SUCCESS, "Set prop: " + name + "=" + value);
        }
    }

    StationProperties getStationProperties() {
        return this.stationProperties;
    }

    /**
     * Handle the remove command.
     */
    class RemoveCommandHandler extends CommandHandler {
        CommandResult execute(JSONObject parameters) {
            CommandResult res = null;
            List<Integer> ids = getStationIDs(parameters);
            int removed = 0;
            if (ids.size() == 0) {
                LOG.info("Removing all stations!");
                removed = Server.this.getStationManager().removeAll();
                if (Server.this.getStationManager().getStationCount() > 0) {
                    res = new CommandStatus(STATUS_ERROR, "Failed to remove at least one station.");
                } else {
                    res = new CommandStatus(STATUS_SUCCESS, "Removed all stations.");
                }
            } else {
                LOG.info("Removing stations: " + ids.toString());
                removed = Server.this.getStationManager().remove(ids);
                if (removed < ids.size()) {
                    res = new CommandStatus(STATUS_ERROR, "Failed to remove at least one station.");
                } else {
                    res = new CommandStatus(STATUS_SUCCESS, "Removed stations: " + ids.toString());
                }
            }
            if (removed == 0) {
                res = new CommandStatus(STATUS_ERROR, "No stations were removed.");
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
            List<Integer> ids = getStationIDs(parameters);
            int cleaned = 0;
            if (ids.size() == 0) {
                LOG.info("Cleaning up all inactive stations!");
                int inactive = Server.this.stationManager.getInactiveCount();
                cleaned = Server.this.stationManager.cleanupAll();
                if (cleaned < inactive) {
                    res = new CommandStatus(STATUS_ERROR, "Failed to cleanup at least one station.");
                } else {
                    res = new CommandStatus(STATUS_SUCCESS, "Cleaned up " + cleaned + " stations.");
                }
            } else {
                LOG.info("Cleaning up stations: " + ids.toString());
                cleaned = Server.this.stationManager.cleanup(ids);
                if (cleaned < ids.size()) {
                    res = new CommandStatus(STATUS_ERROR, "Failed to cleanup at least one station.");
                } else {
                    res = new CommandStatus(STATUS_SUCCESS, "Cleaned up " + cleaned + " stations.");
                }
            }
            if (cleaned == 0) {
                res = new CommandStatus(STATUS_ERROR, "No stations were cleaned up.");
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
                        LOG.config("Set new station start ID: " + mgr.getCurrentStationID());
                    } catch (IllegalArgumentException e) {
                        LOG.log(Level.SEVERE, "Failed to set new station ID", e);
                        error = true;
                    }
                }
                if (parameters.has("workdir")) {
                    File newWorkDir = new File(parameters.getString("workdir"));
                    try {
                        Server.this.setWorkDir(newWorkDir);
                    } catch (Exception e) {
                        LOG.log(Level.SEVERE, "Failed to set new work dir: " + newWorkDir.getPath(), e);
                        error = true;
                    }
                }
                if (parameters.has("basename")) {
                    String stationBase = parameters.getString("basename");
                    try {
                        Server.this.setStationBaseName(stationBase);
                    } catch (IllegalArgumentException e) {
                        LOG.log(Level.SEVERE, "Failed to set station base name: " + stationBase, e);
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
     * Writes lines from <code>Tailer</code> to the client socket.
     */
    class SimpleLogListener extends TailerListenerAdapter {

        BufferedWriter bw;

        void setBufferedWriter(BufferedWriter bw) {
            this.bw = bw;
        }

        public void handle(String line) {
            try {
                bw.write(line + '\n');
                bw.flush();
            } catch (IOException e) {
                throw new RuntimeException("Error writing log line", e);
            }
        }
    }

    /**
     * Encapsulates information needed for streaming log files
     * back to the client.
     */
    class LogStreamResult extends CommandResult {

        SimpleLogListener listener;
        Tailer tailer;
        File log;

        LogStreamResult(Tailer tailer, SimpleLogListener listener, File log) {
            this.listener = listener;
            this.tailer = tailer;
            this.log = log;
        }
    }

    /**
     * Tail a station's log file.
     */
    class LogCommandHandler extends CommandHandler {
        CommandResult execute(JSONObject jo) {
            CommandResult res = null;
            List<Integer> ids = getStationIDs(jo);
            if (ids.size() == 1) {
                int id = ids.get(0);
                Long delayMillis = 1000L;
                if (jo.has("delayMillis")) {
                    delayMillis = jo.getLong("delayMillis");
                }
                SimpleLogListener listener = new SimpleLogListener();
                Tailer tailer = Server.this.getStationManager().getLogTailer(id, listener, delayMillis);
                File logFile = Server.this.getStationManager().find(id).log;
                res = new LogStreamResult(tailer, listener, logFile);
            } else if (ids.size() > 1) {
                res = new CommandStatus(STATUS_ERROR, "Multiple station IDs not supported for log command.");
            } else if (ids.size() == 0) {
                res = new CommandStatus(STATUS_ERROR, "No station IDs were given in parameters.");
            }
            return res;
        }
    }

    /**
     * Get the status and state of the ET system.
     * @param jo The JSON object to update with status
     */
    private void getEtStatus(JSONObject jo, boolean verbose) {
        try {
            jo.put("alive", etSystem.alive());
            jo.put("host", etConfig.getHost());
            jo.put("port", etConfig.getTcpPort());
            if (etSystem.alive()) {
                jo.put("pid", etSystem.getPid());
                jo.put("num_stations", etSystem.getNumStations());
                jo.put("attachments", etSystem.getNumAttachments());
                jo.put("attachments_max", etSystem.getAttachmentsMax());
            }
            if (verbose) {
                JSONObject joRes = new JSONObject();
                final List<StationProcess> stations =
                        Server.this.getStationManager().getStations();
                for (StationProcess station : stations) {
                    if (etSystem.stationExists(station.stationName)) {
                        EtStation etStation = etSystem.stationNameToObject(station.stationName);
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
            LOG.log(Level.SEVERE, "Error getting ET status", e);
            jo.put("error", e.getMessage());
        }
    }

    /**
     * Get whether the ET system is alive or not.
     * @return True if ET system is alive; false if fail to connect
     */
    boolean isEtAlive() {
        return etSystem.alive();
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
        try {
            server.openEtConnection();
            server.start();
        } catch (EtException|IOException|EtTooManyException etEx) {
            System.err.println("Failed to open ET system");
            etEx.printStackTrace();
        } catch (Exception e) {
            System.err.println("Error starting server");
            e.printStackTrace();
        } finally {
            server.shutdown();
        }
    }

    /**
     * Interrupt all active threads in the server process and stop them, if necessary
     *
     * FIXME: This is very hacky and maybe not advisable!
     */
    /*
    private void cleanUpThreads() {
        LOG.fine("Cleaning up threads...");
        Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
        for (Thread thread : threadSet) {
            LOG.fine("Cleaning up thread: " + thread.getName());
            thread.interrupt();
            try {
                thread.join(1000L);
            } catch (InterruptedException e) {
            }
            if (thread.isAlive()) {
                thread.stop();
            }
            LOG.fine("Done cleaning up thread");
        }
        LOG.fine("Done cleaning up threads");
    }
    */

    /**
     * Create a new server instance.
     */
    Server() {
        this.stationManager = new StationManager(this);

        try {
            // FIXME: Hard-coded settings on the aggregator
            agg.connect();
        } catch (Exception e) {
            throw new RuntimeException("Failed to connect aggregator", e);
        }
        // Run the plot aggregator every 5 seconds
        exec.scheduleAtFixedRate(agg, 0, agg.getUpdateInterval(), TimeUnit.MILLISECONDS);
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
            throw new IllegalArgumentException("The stationBase argument points to null.");
        }
        this.stationBase = stationBase;
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
            LOG.config("Creating work dir: " + workDir);
            workDir.mkdirs();
            if (!workDir.exists()) {
                throw new RuntimeException("Failed to create work dir: " + workDir.getPath());
            }
        }
        if (!workDir.isDirectory()) {
            throw new RuntimeException("Work dir is not a directory: " + workDir.getPath());
        }
        if (!workDir.canWrite()) {
            throw new RuntimeException("Work dir is not writable: " + workDir.getPath());
        }
    }

    /**
     * Set the server base working directory.
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
    // TODO: Read all settings from a config file instead using PropertySet class
    void parse(String args[]) throws ParseException {
        Options options = new Options();
        options.addOption(new Option("h", "help", false, "print help"));
        options.addOption(new Option("p", "port", true, "server port"));
        options.addOption(new Option("s", "start", true, "starting station ID (default 1)"));
        options.addOption(new Option("w", "workdir", true, "work dir (default is current dir where server is started)"));
        options.addOption(new Option("b", "basename", true, "station base name"));
        options.addOption(new Option("c", "config", true, "config properties file"));

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
            LOG.severe("Bad port number: " + this.port);
            throw new RuntimeException("Bad port number: " + this.port);
        }
        LOG.config("Server port: " + this.port);

        // Starting station ID.
        if (cl.hasOption("s")) {
            int processID = Integer.parseInt(cl.getOptionValue("s"));
            this.stationManager.setStationID(processID);
            LOG.config("Starting station ID: " + processID);
        }

        // Base work directory for creating station directories.
        String workPath = DEFAULT_WORK_PATH;
        if (cl.hasOption("w")) {
            workPath = cl.getOptionValue("w");
        }
        setWorkDir(new File(workPath));

        LOG.config("Server work dir: " + this.workDir.getPath());

        // Base name for station to which will be appended the station ID.
        if (cl.hasOption("b")) {
            this.stationBase = cl.getOptionValue("b");
        }
        LOG.config("Station base name: " + this.stationBase);

        // Load station configuration properties.
        if (cl.hasOption("c")) {
            File configFile = new File(cl.getOptionValue("c"));
            if (!configFile.exists()) {
                throw new RuntimeException("Config file does not exist: " + configFile.getPath());
            }
            this.stationProperties.load(configFile);
        }
    }

    /**
     * Open a connection to the ET system
     * @throws EtException If there is an error connecting
     * @throws IOException If there is an IO error
     * @throws EtTooManyException If there are too many ET connections already
     */
    void openEtConnection() throws EtException, IOException, EtTooManyException {

        LOG.config("Opening ET system...");
        Property<String> buffer = stationProperties.get("et.buffer");
        Property<String> host = stationProperties.get("et.host");
        Property<Integer> port = stationProperties.get("et.port");
        Property<Integer> maxAttempts = stationProperties.get("et.connectionAttempts");
        etConfig = new EtSystemOpenConfig(buffer.value(), host.value(), port.value());
        etSystem = new EtSystem(etConfig, EtConstants.debugWarn);

        for (int i = 0; i < maxAttempts.value(); i++) {
            try {
                LOG.config("ET connection attempt: " + (i + 1));
                etSystem.open();
                break;
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Failed to connect to ET system", e);
            }
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        if (!etSystem.alive()) {
            throw new EtException("Failed to connect to ET system after " + maxAttempts.value() + " attempts");
        }

        // Shutdown hook for ET cleanup
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    if (etSystem != null) {
                        if (etSystem.alive()) {
                            LOG.info("Closing ET system...");
                            etSystem.close();
                            etSystem = null;
                            etConfig = null;
                            LOG.info("ET system closed!");
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        // Run a monitoring thread to shutdown the server if ET system closes
        exec.scheduleAtFixedRate(new EtMonitor(), 0, 5, TimeUnit.SECONDS);

        LOG.config("Done opening ET system!");
    }

    EtSystem getEtSystem() {
        return etSystem;
    }

    /**
     * Shutdown the server, attempting to cleanup all active
     * components, stations, and threads
     */
    void shutdown() {

        stationManager.stopAll();

        LOG.info("Shutting down monitoring threads...");
        //exec.shutdownNow();
        exec.shutdown();

        //LOG.info("Disconnecting aggregator...");
        agg.disconnect();

        //LOG.info("Active threads...");
        //this.debugPrintThreads();

        // TODO: Check if this is necessary or should be moved to shutdown hook
        //cleanUpThreads();

        LOG.info("Exiting application...");
        System.exit(0);
    }

    void debugPrintThreads() {
        Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
        for (Thread thread : threadSet) {
            LOG.info(thread.getName());
        }
    }

    /**
     * Start the server.
     */
    void start() throws Exception {

        // Client connection loop
        LOG.info("Starting server on port: " + this.port);
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                clientProcessingPool.submit(new ClientTask(clientSocket));
            }
        } catch (IOException e) {
            throw new RuntimeException("Server exception", e);
        }
    }

    /**
     * Continuously monitor the ET system and exit the application
     * if it goes down (run on a scheduled thread executor)
     */
    class EtMonitor implements Runnable {

        @Override
        public void run() {
            //LOG.finest("EtMonitor is running...");
            if (!Server.this.etSystem.alive()) {
                LOG.severe("ET connection went down!");
                Server.this.etSystem = null;
                Server.this.shutdown();
            }
            //LOG.finest("EtMonitor is done running");
        }

    }
}
