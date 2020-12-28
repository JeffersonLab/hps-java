package org.hps.online.recon;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ProcessBuilder.Redirect;
import java.lang.reflect.Field;
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
import org.jlab.coda.et.EtStation;
import org.jlab.coda.et.EtSystem;
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
    class StationInfo {

        volatile boolean active;
        List<String> command;
        File dir;
        int id;
        File log;
        long pid = -1L;
        Process process;
        String stationName;
        File configFile;
        StationProperties props;
        int exitValue = -1;
        String remoteTreeBind = null;

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
    private int remoteAidaPortStart = 2000;

    /**
     * Define station list as synchronized because we do not want different
     * threads mucking with it at the same time.
     */
    private final List<StationInfo> stations = Collections.synchronizedList(
            new ArrayList<StationInfo>());

    StationMonitor stationMonitor = new StationMonitor();

    /**
     * Create a new instance of this class
     * @param server Reference to the containing {@link Server}
     */
    StationManager(Server server) {
        this.server = server;

        // Schedule the station monitor to run every 500 milliseconds
        this.server.exec.scheduleAtFixedRate(stationMonitor, 0, 500, TimeUnit.MILLISECONDS);
    }

    /**
     * Add information about a station
     * @param info The station information
     */
    void add(StationInfo info) {
        stations.add(info);
    }

    /**
     * Create the work directory for a station.
     * @param name The name of the station
     * @return The File of the new directory
     */
    synchronized File createStationDir(String name) {
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
     * @param station The station to start
     * @throws IOException If there is a problem starting the station's process
     */
    synchronized void start(final StationInfo station) throws IOException {

        LOG.info("Starting station: " + station.stationName);

        if (!station.active) {

            // Make sure station dir exists.
            File dir = station.dir;
            if (!dir.exists()) {
                LOG.info("Recreating missing station dir: " + dir.getPath());
                dir.mkdir();
            }

            // The config file disappeared, probably from the cleanup command.
            if (!station.configFile.exists()) {
                LOG.info("Rewriting station config file: " + station.configFile.getPath());
                this.writeStationProperties(station.props, dir, station.stationName);
            }

            List<String> command = station.command;
            ProcessBuilder pb = new ProcessBuilder(command);

            Integer stationID = station.id;

            pb.directory(dir);
            File log = new File(dir.getPath() + File.separator + "out." + stationID.toString() + ".log");
            if (log.exists()) {
                if (log.delete()) {
                    LOG.info("Deleted old log file: " + log.getPath());
                } else {
                    LOG.warning("Failed to delete old log file " + log.getPath());
                }
            }
            pb.redirectErrorStream(true);
            pb.redirectOutput(Redirect.appendTo(log));
            station.log = log;

            LOG.info("Starting command: " + String.join(" ", pb.command()));

            // Can throw exception.
            Process p = pb.start();

            // These won't be set if exception is thrown but that's fine because
            // the process failed to start.
            station.process = p;
            station.pid = getPid(p);
            station.active = true;

            // Add remote tree bind by reading a file written by the remote AIDA driver
            // which contains the URL for connecting to the station.
            final String dirPath = dir.getCanonicalPath();
            new Thread() {
                public void run() {
                    File remoteTreeFile = new File(dirPath + File.separator + "remoteTreeBind");
                    while (true) {
                        // Check for file with remote tree bind information written by the driver
                        if (remoteTreeFile.exists()) {
                            try {
                                InputStream in = new FileInputStream(remoteTreeFile);
                                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                                String remoteTreeBind = reader.readLine().trim();
                                station.remoteTreeBind = remoteTreeBind;
                                reader.close();
                                LOG.info("Adding station remote tree bind: " + remoteTreeBind);
                                server.agg.addRemote(station.remoteTreeBind);
                                LOG.info("Done adding station remote tree bind");
                                break;
                            } catch (Exception e) {
                                LOG.log(Level.WARNING, "Failed to add station remote tree bind", e);
                                break;
                            }
                        } else {
                            LOG.warning("Remote tree file does not exist: " + remoteTreeFile);
                        }
                        // Wait awhile before checking for the file again
                        try {
                            Thread.sleep(5000L);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }.start();

            LOG.info("Successfully started station: " + station.stationName);
        } else {
            LOG.warning("Station is already active: " + station.stationName);
        }
    }

    /**
     * Write station configuration properties to a file.
     * @param sc The station configuration properties
     * @param dir The target directory
     * @param stationName The name of the station
     * @return The file with the station configuration
     */
    private File writeStationProperties(StationProperties props, File dir, String stationName) {
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
    StationInfo create(JSONObject parameters) {

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
        StationInfo info = new StationInfo();
        info.id = stationID;
        info.stationName = stationName;
        info.dir = dir;
        info.props = props;

        // Remote AIDA port
        int remoteAidaPort = this.remoteAidaPortStart + info.id;
        Property<Integer> remoteAidaPortProp = props.get("lcsim.remoteAidaPort");
        remoteAidaPortProp.from(remoteAidaPort);

        LOG.config("New station properties: " + props.toString());

        // TODO: Validate properties here

        // Write the properties file for the station to read in when running
        File scf = writeStationProperties(props, dir, stationName);
        info.configFile = scf;

        // Build the command to run the station
        info.command = buildCommand(info);

        LOG.config("Command: " + String.join(" ", info.command));

        // Register the station info
        add(info);

        return info;
    }

    /**
     * Build the command for running the station.
     * @param configFile The station configuration properties file
     */
    private List<String> buildCommand(StationInfo info) {

        final StationProperties props = info.props;
        Property<String> logConfigFile = props.get("station.loggingConfig");

        List<String> command = new ArrayList<String>();

        command.add("java");

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
    StationInfo find(int id) {
        StationInfo station = null;
        for (StationInfo info : stations) {
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
    List<StationInfo> getStations() {
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
        for (StationInfo info : this.stations) {
            if (info.active) {
                if (stop(info)) {
                    ++n;
                }
            }
        }
        LOG.info("Stopped all active stations!");
        return n;
    }

    /**
     * Stop a station by its ID.
     * @param id The ID of the station to stop
     * @return True if the station was stopped successfully
     */
    synchronized boolean stop(int id) {
        LOG.info("Stopping station with id: " + id);
        StationInfo info = this.find(id);
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
    // TODO: Throw an exception instead of returning false if an error occurs
    synchronized boolean stop(StationInfo info) {
        boolean success = false;
        if (info != null) {
            LOG.info("Stopping station: " + info.stationName);
            Process p = info.process;
            if (!info.active) {

                // Dismount the station's AIDA tree
                if (info.remoteTreeBind != null) {
                    server.agg.unmount(info.remoteTreeBind);
                    info.remoteTreeBind = null;
                }

                // Wake up the station
                EtSystem etSystem = server.getEtSystem();
                try {
                    EtStation etStation =
                            server.getEtSystem().stationNameToObject(info.stationName);
                    etSystem.wakeUpAll(etStation);
                } catch (Exception e) {
                    LOG.log(Level.WARNING, "Error waking up station", e);
                }

                // Destroy the station's system process
                p.destroy();
                try {
                    LOG.fine("Waiting for station to stop: " + info.stationName);
                    p.waitFor();
                    LOG.fine("Done waiting for station to stop: " + info.stationName);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                // Set station info to indicate that it is inactive with no valid process
                info.active = false;
                info.pid = -1L;
                info.process = null;
                LOG.info("Stopped station: " + info.stationName);

                success = true;

            } else {
                LOG.warning("Station is not active so stop command is ignored: " + info.stationName);
            }
        }
        return success;
    }

    /**
     * Remove a station from the manager.
     * @param info
     * @return True if the station was successfully removed.
     */
    synchronized boolean remove(StationInfo info) {
        LOG.info("Removing station: " + info.stationName);
        if (info.active == false) {
            this.stations.remove(info);
            LOG.info("Removed station: " + info.stationName);
            return true;
        } else {
            LOG.warning("Failed to remove station because it is still active: " + info.stationName);
            return false;
        }
    }

    /**
     * Remove a list of stations by their IDs.
     * @param ids The list of station IDs to remove
     * @return The number of stations successfully removed
     */
    synchronized int remove(List<Integer> ids) {
        int n = 0;
        List<StationInfo> stations = find(ids);
        for (StationInfo station : stations) {
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
    synchronized int stop(List<Integer> ids) {
        int n = 0;
        List<StationInfo> stations = find(ids);
        for (StationInfo station : stations) {
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
    private List<StationInfo> find(List<Integer> ids) {
        List<StationInfo> stations = new ArrayList<StationInfo>();
        for (StationInfo station : this.stations) {
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
        for (StationInfo station : this.stations) {
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
    synchronized int getActiveCount() {
        int n = 0;
        for (StationInfo station : this.stations) {
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
    synchronized int getInactiveCount() {
        int n = 0;
        for (StationInfo station : this.stations) {
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
        for (StationInfo station : this.stations) {
            if (!station.active) {
                try {
                    this.start(station);
                    ++started;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return started;
    }

    /**
     * Start a list of stations by their IDs
     * @param ids A list of station IDs to start
     * @return The number of stations started
     */
    synchronized int start(List<Integer> ids) {
        int started = 0;
        List<StationInfo> stations = this.find(ids);
        for (StationInfo station : stations) {
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
    synchronized boolean cleanup(StationInfo station) {
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
    synchronized int cleanup(List<Integer> ids) {
        int cleaned = 0;
        List<StationInfo> stations = this.find(ids);
        for (StationInfo station : stations) {
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
        for (StationInfo station : this.stations) {
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
        for (StationInfo station : this.stations) {
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
        List<StationInfo> stations = this.find(ids);
        List<File> dirs = new ArrayList<File>();
        for (StationInfo station : stations) {
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
        StationInfo station = this.find(id);
        if (!station.active) {
            throw new RuntimeException("Station is not active: " + station.stationName);
        }
        File logFile = station.log;
        if (!logFile.exists()) {
            throw new RuntimeException("Station log file does not exist: " + logFile.getPath());
        }
        return new Tailer(logFile, listener, delayMillis, true);
    }

    /**
     * Check the status of all station's periodically (run on a scheduled executor)
     */
    class StationMonitor implements Runnable {

        StationManager mgr = StationManager.this;

        public void run() {
            LOG.finest("StationMonitor is running...");
            for (StationInfo station : mgr.getStations()) {
                if (station.process != null) {
                    station.active = station.process.isAlive();
                    if (!station.active) {
                        station.exitValue = station.process.exitValue();
                        station.pid = -1L;
                        station.process = null;
                        LOG.info("Station " + station.stationName + " is now inactive with exit value: "
                                + station.exitValue);
                    }
                }
            }
            LOG.finest("StationMonitor is done running");
        }
    }
}
