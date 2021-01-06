package org.hps.online.recon;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Scanner;
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
import org.hps.online.recon.CommandResult.Error;
import org.hps.online.recon.CommandResult.LogStreamResult;
import org.hps.online.recon.handlers.CommandHandlerFactory;
import org.hps.online.recon.properties.Property;
import org.jlab.coda.et.EtConstants;
import org.jlab.coda.et.EtStation;
import org.jlab.coda.et.EtSystem;
import org.jlab.coda.et.EtSystemOpenConfig;
import org.jlab.coda.et.exception.EtException;
import org.jlab.coda.et.exception.EtTooManyException;
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
    final PlotAggregator agg = new PlotAggregator();

    CommandHandlerFactory handlers = null;

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

                // Get command handler
                CommandHandler handler = null;
                try {
                    // Find the handler for the command.
                    LOG.info("Getting handler for command: " + command);
                    handler = handlers.getCommandHandler(command);
                    LOG.info("Got command handler: " + handler.getClass().getCanonicalName());
                } catch (IllegalArgumentException e) {
                    // Command name is invalid. This shouldn't happen normally.
                    res = new Error("Unknown command: " + command);
                }

                // Execute command
                if (handler != null) {
                    try {
                        // Execute the command and get its result.
                        LOG.info("Executing command: " + command);
                        res = handler.execute(params);
                    } catch (Exception e) {
                        // Some kind of error occurred executing the command.
                        LOG.log(Level.SEVERE, "Error executing command: " + command, e);
                        res = new Error(e.getMessage());
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
            } finally {
                // Close the socket.
                try {
                    socket.close();
                } catch (IOException e) {
                    LOG.log(Level.SEVERE, "Error closing socket", e);
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
    }

    public Logger getLogger() {
        return LOG;
    }

    /**
     * Get the station properties (config settings)
     * @return The station properties
     */
    public StationProperties getStationProperties() {
        return this.stationProperties;
    }

    /**
     * Get the status and state of the ET system.
     * @param jo The JSON object to update with status
     */
    public void getEtStatus(JSONObject jo, boolean verbose) {
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
            server.startServer();
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
     * Create a new server instance.
     */
    private Server() {
        this.stationManager = new StationManager(this);

        handlers = new CommandHandlerFactory(this);

        try {
            // FIXME: Hard-coded connection settings on the aggregator
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

    public void save(File file) throws IOException {
        this.agg.save(file);
    }

    /**
     * Shutdown the server, attempting to cleanup all active
     * components, stations, and threads
     */
    void shutdown() {

        stationManager.stopStations(stationManager.getActiveStations());

        LOG.info("Shutting down monitoring threads...");
        //exec.shutdownNow();
        exec.shutdown();

        //LOG.info("Disconnecting aggregator...");
        agg.disconnect();

        LOG.info("Exiting application...");
        System.exit(0);
    }

    /**
     * Start the server.
     */
    private void startServer() throws Exception {

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
