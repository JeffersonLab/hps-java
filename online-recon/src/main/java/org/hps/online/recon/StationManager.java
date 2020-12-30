package org.hps.online.recon;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListener;
import org.hps.online.recon.properties.Property;
import org.hps.online.recon.properties.PropertyValidationException;
import org.json.JSONObject;

/**
 * Manages online reconstruction stations by creating, starting, and stopping them,
 * as well as setting up the connection to their remote AIDA trees for the
 * {@link InlineAggregator}.
 */
public class StationManager {

    /**
     * Information for managing a single online reconstruction station
     */
    class StationProcess {

        String stationName;
        private int id;

        private boolean active = false;
        private Process process;
        private long pid = -1L;
        private int exitValue = -1;

        private List<String> command;
        File log;
        private File dir;
        private File configFile;

        private StationProperties props;

        /**
         * Convert station data to JSON.
         * @return The converted JSON data
         */
        JSONObject toJSON() {
            JSONObject jo = new JSONObject();
            jo.put("pid", pid);
            jo.put("active", active);
            jo.put("id", id);
            jo.put("station", stationName);
            jo.put("command", String.join(" ", command));
            jo.put("dir", dir.getPath());
            jo.put("log", log != null ? FilenameUtils.getBaseName(log.getPath()) : "");
            jo.put("props", props.toJSON());
            return jo;
        }

        synchronized void activate() throws IOException {

            LOG.info("Activating station: " + stationName);

            if (active) {
                LOG.warning("Station is already active: " + stationName);
                return;
            }

            // Make sure station directory exists.
            if (!dir.exists()) {
                LOG.info("Recreating missing station dir: " + dir.getPath());
                dir.mkdir();
            }

            // The config file disappeared, probably from the cleanup command.
            if (!configFile.exists()) {
                LOG.info("Rewriting station config file: " + configFile.getPath());
                writeStationProperties(props, dir, stationName);
            }

            ProcessBuilder pb = new ProcessBuilder(command);

            pb.directory(dir);
            log = new File(dir.getPath() + File.separator + "out." + Integer.valueOf(id).toString() + ".log");
            if (log.exists()) {
                if (log.delete()) {
                    LOG.info("Deleted old log file: " + log.getPath());
                } else {
                    LOG.warning("Failed to delete old log file " + log.getPath());
                }
            }
            pb.redirectErrorStream(true);
            pb.redirectOutput(Redirect.appendTo(log));

            LOG.info("Starting command: " + '\n' + String.join(" ", pb.command()) +'\n');

            // Can throw exception.
            process = pb.start();
            pid = getPid(process);

            if (this.props.get("lcsim.remoteTreeBind").valid()) {
                Property<String> remoteTreeBind = this.props.get("lcsim.remoteTreeBind");
                for (long i = 0; i < 10; i++) {
                    long attempt = i + 1;
                    try {
                        try {
                            Thread.sleep(attempt*3000L);
                        } catch (InterruptedException e) {
                            throw new RuntimeException("Interrupted", e);
                        }
                        LOG.info("Adding remote tree bind: " + remoteTreeBind.value());
                        server.agg.addRemote(remoteTreeBind.value());
                        LOG.info("Done adding remote tree bind: " + remoteTreeBind.value());
                        break;
                    } catch (Exception e) {
                        LOG.log(Level.WARNING, "Cannot connect to: " + remoteTreeBind.value(), e);
                    }
                }
            }

            LOG.info("Setting station to active");
            active = true;
        }

        // Set station info to indicate that it is inactive with no valid process
        synchronized void deactivate() {

            LOG.info("Deactivating station: " + stationName);

            if (!active) {
                LOG.warning("Station has already been deactivated: " + stationName);
                return;
            }

            active = false;

            // Dismount the station's AIDA tree
            if (props.get("lcsim.remoteTreeBind").valid()) {
                Property<String> rtb = props.get("lcsim.remoteTreeBind");
                server.agg.unmount(rtb.value());
            }

            // Destroy the station's system process
            if (process != null) {
                if (process.isAlive()) {
                    //process.destroy();
                    process.destroyForcibly();
                    try {
                        LOG.fine("Waiting for station to stop: " + stationName);
                        process.waitFor(30, TimeUnit.SECONDS);
                        LOG.fine("Done waiting for station to stop: " + stationName);
                        if (process.isAlive()) {
                            LOG.severe("station did not stop after 30 seconds!");
                            throw new RuntimeException("Station failed to stop");
                        }
                        exitValue = process.exitValue();
                        LOG.fine("Exit value: " + exitValue);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    exitValue = process.exitValue();
                }
                process = null;
            }

            pid = -1L;

            LOG.info("Done deactivating station: " + stationName);
        }
    }

    /**
     * Name of station properties file.
     */
    private static final String STATION_CONFIG_NAME = "station.properties";

    /**
     * The package logger.
     */
    private static final Logger LOG = Logger.getLogger(StationManager.class.getPackage().getName());

    /**
     * Get the PID of a system process
     * @param p The system process
     * @return The process's PID
     */
    private static Long getPid(Process p) {
        long pid = -1;
        if (p != null) {
            try {
                Field f = p.getClass().getDeclaredField("pid");
                f.setAccessible(true);
                pid = f.getLong(p);
                f.setAccessible(false);
            } catch (Exception e) {
                pid = -1;
            }
        }
        return pid;
    }

    /**
     * Reference to the server.
     */
    private final Server server;

    /**
     * Next station ID.
     */
    private volatile int stationID = 1;

    /**
     * Starting port number for remote AIDA access.
     */
    private int remoteAidaPortStart = 5000;

    private String hostName = null;

    /**
     * Define station list as synchronized because we do not want different
     * threads mucking with it at the same time.
     */
    private final List<StationProcess> stations = Collections.synchronizedList(
            new ArrayList<StationProcess>());

    StationMonitor stationMonitor = new StationMonitor();

    /**
     * Create a new instance of this class
     * @param server Reference to the containing {@link Server}
     */
    StationManager(Server server) {

        try {
            this.hostName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }

        this.server = server;

        // Schedule the station monitor to run every 500 milliseconds
        this.server.exec.scheduleAtFixedRate(stationMonitor, 0, 500, TimeUnit.MILLISECONDS);
    }

    /**
     * Add information about a station
     * @param station The station information
     */
    void add(StationProcess station) {
        LOG.info("StationManager.add - " + station.stationName);
        stations.add(station);
    }

    /**
     * Create the work directory for a station.
     * @param name The name of the station
     * @return The File of the new directory
     */
    File createStationDir(String name) {
        String path = server.getWorkDir().getPath() + File.separator + name;
        File dir = new File(path);
        dir.mkdir();
        if (!dir.isDirectory()) {
            throw new RuntimeException("Error creating station dir: " + dir.getPath());
        }
        return dir;
    }

    /**
     * Start an online reconstruction station's system process.
     *
     * @param station The station to start
     * @throws IOException If there is a problem starting the station's process
     */
    void start(final StationProcess station) throws IOException {

        LOG.info("Starting station: " + station.stationName);

        synchronized(station) {
            if (!station.active) {
                station.activate();
                LOG.info("Successfully started station: " + station.stationName);

            } else {
                LOG.warning("Station is already active: " + station.stationName);
            }
        }
    }

    /**
     * Write station configuration properties to a file.
     * @param sc The station configuration properties
     * @param dir The target directory
     * @param stationName The name of the station
     * @return The file with the station configuration
     */
    private static File writeStationProperties(StationProperties props, File dir, String stationName) {
        File scf = new File(dir.getPath() + File.separator + STATION_CONFIG_NAME);
        try {
            props.write(scf, "Configuration properties for online recon station: " + stationName);
        } catch (Exception e) {
            throw new RuntimeException("Error writing station config file", e);
        }
        return scf;
    }

    /**
     * Create a new station.
     * @param parameters The JSON parameters defining the station
     * @return The new station info
     */
    StationProcess create(JSONObject parameters) {

        LOG.info("StationManager.create");

        // Get next station ID
        Integer stationID = getNextStationID();
        if (exists(stationID)) {
            LOG.severe("Station ID " + stationID + " already exists.  Set a new station start ID to fix.");
            throw new RuntimeException("Station ID already exists: " + stationID);
        }

        LOG.info("New station ID: " + stationID);

        // Get unique name for this station
        String stationName = this.server.getStationBaseName() + "_" + String.format("%03d", stationID);
        LOG.info("New station name: " + stationName);

        // Create the directory for this station's files
        File dir = createStationDir(stationName);

        // Copy the server's properties and set some station-specific properties
        StationProperties props = new StationProperties(this.server.getStationProperties());
        props.get("et.stationName").set(stationName);
        props.get("station.outputName").set(stationName.toLowerCase());
        props.get("station.outputDir").set(dir.getPath());

        // Add new station info.
        StationProcess info = new StationProcess();
        info.id = stationID;
        info.stationName = stationName;
        info.dir = dir;
        info.props = props;

        final String remoteTreeBind = "//" + this.hostName
                + ":" + (this.remoteAidaPortStart + stationID) + "/"
                + stationName;

        LOG.info("remoteTreeBind: " + remoteTreeBind);
        props.get("lcsim.remoteTreeBind").from(remoteTreeBind);

        try {
            props.validate();
        } catch (PropertyValidationException pve) {
            LOG.warning(pve.getMessage());
        }

        // Write the properties file for the station to read in when running
        File scf = writeStationProperties(props, dir, stationName);
        info.configFile = scf;

        // Build the command to run the station
        info.command = buildCommand(info);

        LOG.config("Command: " + String.join(" ", info.command));

        // Register the station info
        add(info);

        LOG.info("StationManager.create - done");

        return info;
    }

    /**
     * Build the command for running the station.
     * @param configFile The station configuration properties file
     */
    private List<String> buildCommand(StationProcess info) {

        final StationProperties props = info.props;
        Property<String> logConfigFile = props.get("station.loggingConfig");

        List<String> command = new ArrayList<String>();

        command.add("java");
        command.add("-Xmx1g");

        // Logging configuration
        if (logConfigFile.valid()) {
            command.add("-Djava.util.logging.config.file=" + logConfigFile.value());
        } else {
            command.add("-Djava.util.logging.config.class=" + LoggingConfig.class.getCanonicalName());
        }

        command.add("-cp");
        command.add(System.getProperty("java.class.path"));
        command.add(Station.class.getCanonicalName());
        command.add(info.configFile.getPath());

        return command;
    }

    /**
     * Find a station by its ID.
     * @param id The station's ID
     * @return The station or null if not found
     */
    StationProcess find(int id) {
        StationProcess station = null;
        for (StationProcess info : stations) {
            if (info.id == id) {
                station = info;
                break;
            }
        }
        return station;
    }

    /**
     * Get the next station ID, which automatically increments the stationID value.
     * @return The next station ID
     */
    synchronized int getNextStationID() {
        ++stationID;
        return stationID - 1;
    }

    /**
     * Get an unmodifiable list of stations.
     * @return An unmodifiable list of stations
     */
    List<StationProcess> getStations() {
        return Collections.unmodifiableList(this.stations);
    }

    /**
     * Set the next station ID.
     * @param stationID The next station ID
     * @throws IllegalArgumentException If the new value is bad
     */
    synchronized void setStationID(int stationID) throws IllegalArgumentException {
        if (stationID >= 0) {
            this.stationID = stationID;
            LOG.config("Set station ID: " + stationID);
        } else {
            throw new IllegalArgumentException("Bad value for new station ID: " + stationID);
        }
    }

    /**
     * Stop all active stations.
     * @return The number of stations stopped
     */
    synchronized int stopAll() {
        LOG.info("Stopping all active stations!");
        int n = 0;
        for (StationProcess info : this.getActiveStations()) {
            LOG.info("Stopping station: " + info.stationName);
            if (stop(info)) {
                ++n;
            }
        }
        LOG.info("Stopped stations count: " + n);
        return n;
    }

    /**
     * Stop a station by its ID.
     * @param id The ID of the station to stop
     * @return True if the station was stopped successfully
     */
    boolean stop(int id) {
        LOG.info("Stopping station with id: " + id);
        StationProcess info = this.find(id);
        boolean success = false;
        if (info != null) {
            success = stop(info);
        } else {
            throw new RuntimeException("Unknown process id: " + id);
        }
        return success;
    }

    /**
     * Stop a station.
     *
     * This does not remove it from the station list.  The "remove" command
     * must be used to do this separately.
     *
     * @param info The station info
     * @return True if the station was stopped successfully
     */
    boolean stop(StationProcess info) {
        LOG.info("Stopping station: " + info.stationName);
        info.deactivate();
        return !info.active;
    }

    /**
     * Remove a station from the manager.
     * @param info
     * @return True if the station was successfully removed.
     */
    boolean remove(StationProcess info) {
        synchronized(info) {
            LOG.info("Removing station: " + info.stationName);
            if (!info.active) {
                this.stations.remove(info);
                LOG.info("Removed station: " + info.stationName);
                return true;
            } else {
                LOG.warning("Failed to remove station because it is still active: " + info.stationName);
                return false;
            }
        }
    }

    /**
     * Remove a list of stations by their IDs.
     * @param ids The list of station IDs to remove
     * @return The number of stations successfully removed
     */
    int remove(List<Integer> ids) {
        int n = 0;
        List<StationProcess> stations = find(ids);
        for (StationProcess station : stations) {
            if (remove(station)) {
                ++n;
            }
        }
        return n;
    }

    /**
     * Stop a list of stations by their IDs.
     * @param ids The list of stations to stop
     * @return The number of stations stopped
     */
    int stop(List<Integer> ids) {
        int n = 0;
        List<StationProcess> stations = find(ids);
        for (StationProcess station : stations) {
            if (stop(station)) {
                ++n;
            }
        }
        return n;
    }

    /**
     * Remove all stations, returning number of stations removed.
     * @return The number of stations removed
     */
    synchronized int removeAll() {
        return remove(getStationIDs());
    }

    /**
     * Find a list of stations by their IDs.
     * @param ids The IDs of the stations to find
     * @return A list of stations
     */
    private List<StationProcess> find(List<Integer> ids) {
        List<StationProcess> stations = new ArrayList<StationProcess>();
        for (StationProcess station : this.stations) {
            if (ids.contains(station.id)) {
                stations.add(station);
            }
        }
        return stations;
    }

    /**
     * Get a list of all station IDs
     * @return A list of all station IDs
     */
    private List<Integer> getStationIDs() {
        List<Integer> ids = new ArrayList<Integer>();
        for (StationProcess station : this.stations) {
            ids.add(station.id);
        }
        return ids;
    }

    /**
     * Get the number of stations.
     * @return The number of stations
     */
    int getStationCount() {
        return this.stations.size();
    }

    /**
     * Get the number of active stations.
     * @return The number of active stations
     */
    int getActiveCount() {
        int n = 0;
        for (StationProcess station : this.stations) {
            if (station.active) {
                ++n;
            }
        }
        return n;
    }

    /**
     * Get the number of inactive stations
     * @return The number of inactive stations
     */
    int getInactiveCount() {
        int n = 0;
        for (StationProcess station : this.stations) {
            if (!station.active) {
                ++n;
            }
        }
        return n;
    }

    /**
     * Start all inactive stations.
     * @return The number of stations started
     */
    synchronized int startAll() {
        int started = 0;
        final List<StationProcess> inactiveStats = this.getInactiveStations();
        for (StationProcess station : inactiveStats) {
            try {
                this.start(station);
                ++started;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return started;
    }

    /**
     * Start a list of stations by their IDs
     * @param ids A list of station IDs to start
     * @return The number of stations started
     */
    int start(List<Integer> ids) {
        int started = 0;
        List<StationProcess> stations = this.find(ids);
        for (StationProcess station : stations) {
            try {
                start(station);
            } catch (IOException e) {
                LOG.log(Level.SEVERE, "Failed to start station: " + station.stationName, e);
            }
            ++started;
        }
        return started;
    }

    /**
     * Cleanup a station by deleting its working directory.
     * @param station The station to cleanup
     * @return True if the station was successfully cleaned up
     */
    boolean cleanup(StationProcess station) {
        LOG.info("Cleaning up station: " + station.stationName);
        boolean deleted = false;
        if (!station.active) {
            try {
                LOG.info("Deleting station work dir: " + station.dir.getPath());
                FileUtils.deleteDirectory(station.dir);
                deleted = true;
            } catch (IOException e) {
                LOG.log(Level.SEVERE, "Failed to cleanup station: " + station.stationName, e);
            }
        } else {
            LOG.warning("Cannot cleanup station which is still active: " + station.stationName);
        }
        LOG.info("Done cleaning up station: " + station.stationName);
        return deleted;
    }

    /**
     * Cleanup a list of stations by their IDs.
     * @param ids A list of station IDs to cleanup
     * @return The number of stations cleaned up
     */
    int cleanup(List<Integer> ids) {
        int cleaned = 0;
        List<StationProcess> stations = this.find(ids);
        for (StationProcess station : stations) {
            if (cleanup(station)) {
                cleaned++;
            }
        }
        return cleaned;
    }

    /**
     * Cleanup all inactive stations.
     * @return The number of stations cleanup up
     */
    synchronized int cleanupAll() {
        int n = 0;
        for (StationProcess station : this.stations) {
            if (!station.active) {
                if (cleanup(station)) {
                    n++;
                }
            }
        }
        return n;
    }

    /**
     * Check if a station exists with given ID.
     * @param id The ID of the station
     * @return True if a station with this ID exists
     */
    private boolean exists(Integer id) {
        boolean exists = false;
        for (StationProcess station : this.stations) {
            if (station.id == id) {
                exists = true;
                break;
            }
        }
        return exists;
    }

    /**
     * Get the current stationID to be used for the next station assignment,
     * without incrementing it.
     * @return The current station ID
     */
    int getCurrentStationID() {
        return this.stationID;
    }

    /**
     * Get a list of station directories from a list of IDs.
     * @param ids The list of station IDs
     * @return The list of station directories
     */
    List<File> getStationDirectories(List<Integer> ids) {
        List<StationProcess> stations = this.find(ids);
        List<File> dirs = new ArrayList<File>();
        for (StationProcess station : stations) {
            dirs.add(station.dir);
        }
        return dirs;
    }

    /**
     * Get the list of all station directories.
     * @return The list of all station directories
     */
    List<File> getStationDirectories() {
        return getStationDirectories(this.getStationIDs());
    }

    /**
     * Create a <code>Tailer</code> for tailing the log file of a station.
     * @param id The ID of the station
     * @param listener The <code>TailerListener</code> to be attached to the <code>Tailer</code>
     * @param delayMillis The delay in milliseconds between reading the <code>Tailer</code>
     * @return The <code>Tailer</code> for the station's log file
     */
    Tailer getLogTailer(Integer id, TailerListener listener, long delayMillis) {
        if (!this.exists(id)) {
            throw new IllegalArgumentException("Station ID does not exist: " + id);
        }
        StationProcess station = this.find(id);
        if (!station.active) {
            throw new RuntimeException("Station is not active: " + station.stationName);
        }
        File logFile = station.log;
        if (!logFile.exists()) {
            throw new RuntimeException("Station log file does not exist: " + logFile.getPath());
        }
        return new Tailer(logFile, listener, delayMillis, true);
    }

    List<StationProcess> getInactiveStations() {
        List<StationProcess> stats = new ArrayList<StationProcess>();
        for (StationProcess sp : this.getStations()) {
            if (!sp.active) {
                stats.add(sp);
            }
        }
        return stats;
    }

    List<StationProcess> getActiveStations() {
        List<StationProcess> stats = new ArrayList<StationProcess>();
        for (StationProcess sp : this.getStations()) {
            if (sp.active) {
                stats.add(sp);
            }
        }
        return stats;
    }

    /**
     * Check the status of all station's periodically (run on a scheduled executor)
     */
    class StationMonitor implements Runnable {

        StationManager mgr = StationManager.this;

        public void run() {
            LOG.finest("StationMonitor is running...");
            for (StationProcess station : mgr.getStations()) {
                // Set inactive state on stations that have stopped (possibly due to errors)
                synchronized (station) {
                    if (station.active && station.process != null && !station.process.isAlive()) {
                        LOG.info("Deactivating station: " + station.stationName);
                        station.exitValue = station.process.exitValue();
                        station.deactivate();
                        LOG.info("StationMonitor set station " + station.stationName
                            + " to inactive with exit value: " + station.exitValue);
                    }
                }
                //}
            }
            LOG.finest("StationMonitor is done running");
        }
    }
}

// Wake up the station
/*
EtSystem etSystem = server.getEtSystem();
try {
    EtStation etStation =
            server.getEtSystem().stationNameToObject(stationName);
    etSystem.wakeUpAll(etStation);
} catch (Exception e) {
    LOG.log(Level.WARNING, "Error waking up stat)ion", e);
}
*/
