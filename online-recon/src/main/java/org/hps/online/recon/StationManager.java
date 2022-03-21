package org.hps.online.recon;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListener;
import org.hps.online.recon.properties.PropertyValidationException;
import org.json.JSONObject;

/**
 * Manages online reconstruction stations by creating, starting, and stopping
 * them using a {@link StationProcess}, as well as setting up the connection
 * to their remote AIDA trees for the {@link PlotAggregator}.
 */
public class StationManager {

    /**
     * Name of station properties file.
     */
    private static final String STATION_CONFIG_NAME = "station.properties";

    /**
     * The package logger.
     */
    private static final Logger LOG = Logger.getLogger(StationManager.class.getPackage().getName());

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

    /**
     * Host name for creating new stations.
     */
    private String hostName = null;

    /**
     * Define station list as synchronized because we do not want different threads
     * mucking with it at the same time.
     */
    private final List<StationProcess> stations = Collections.synchronizedList(new ArrayList<StationProcess>());

    /**
     * Monitoring thread to deactivate stations where the process has died.
     */
    StationMonitor stationMonitor = new StationMonitor();

    Object updatingStations = new Object();

    /**
     * Create a new instance of this class
     *
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
     *
     * @param station The station information
     */
    void add(StationProcess station) {
        stations.add(station);
    }

    /**
     * Create the work directory for a station.
     *
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
     * Write station configuration properties to a file.
     *
     * @param sc          The station configuration properties
     * @param dir         The target directory
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
     *
     * @param parameters The JSON parameters defining the station
     * @return The new station info
     */
    public StationProcess create(JSONObject parameters) {

        // Get next station ID
        Integer stationID = getNextStationID();
        if (exists(stationID)) {
            LOG.severe("Station ID " + stationID + " already exists. Remove existing station to fix.");
            throw new IllegalArgumentException("Station ID already exists: " + stationID);
        }

        // Get unique name for this station
        String stationName = this.server.getStationBaseName() + "_" + String.format("%03d", stationID);
        LOG.info("Creating new station: " + stationName);

        // Create the directory for this station's files
        File dir = createStationDir(stationName);

        // Copy the server's properties and set some additional station-specific properties
        StationProperties props = new StationProperties(this.server.getStationProperties());
        props.get("et.stationName").set(stationName);
        props.get("station.outputName").set(stationName.toLowerCase());
        props.get("station.outputDir").set(dir.getPath());

        // Add new station info.
        StationProcess info = new StationProcess(stationID, stationName, dir, props);

        // Set the AIDA remote tree bind information
        final String remoteTreeBind = "//" + this.hostName + ":" + (this.remoteAidaPortStart + stationID) + "/"
                + stationName;
        props.get("lcsim.remoteTreeBind").from(remoteTreeBind);

        try {
            props.validate();
        } catch (PropertyValidationException pve) {
            LOG.warning(pve.getMessage());
        }

        // Write the properties file for the station to read in when running
        File scf = writeStationProperties(props, dir, stationName);
        info.setConfigFile(scf);

        // Build the command to run the station
        info.buildCommand();

        LOG.info("Station properties: " + props.toString());

        // Register the station info
        add(info);

        return info;
    }

    /**
     * Find a station by its ID.
     *
     * @param id The station's ID
     * @return The station or null if not found
     */
    public StationProcess find(int id) {
        StationProcess station = null;
        for (StationProcess info : stations) {
            if (info.getStationID() == id) {
                station = info;
                break;
            }
        }
        return station;
    }

    /**
     * Get the next station ID, which automatically increments the stationID value.
     *
     * @return The next station ID
     */
    synchronized int getNextStationID() {
        ++stationID;
        return stationID - 1;
    }

    /**
     * Get an unmodifiable list of stations.
     *
     * @return An unmodifiable list of stations
     */
    public List<StationProcess> getStations() {
        return Collections.unmodifiableList(this.stations);
    }

    /**
     * Set the next station ID.
     *
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
     * Stop a station.
     *
     * This does not remove it from the station list. The remove command must be
     * used to do this separately.
     *
     * @param info The station info
     * @return True if the station was stopped successfully
     */
    boolean stopStation(StationProcess station) {
        LOG.info("Stopping station: " + station.stationName);
        try {
            station.deactivate(this.server);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Error stopping station: " + station.stationName, e);
        }
        return station.isActive();
    }

    /**
     * Stop a list of stations
     * @param stations The list of stations
     * @return Number of stations stopped
     */
    public int stopStations(List<StationProcess> stations) {
        int stopped = 0;
        for (StationProcess station : stations) {
            try {
                stopStation(station);
                ++stopped;
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Error deactivating station: " + station.stationName, e);
            }
        }
        return stopped;
    }

    /**
     * Stop all active stations
     */
    synchronized void stopAll() {
        stopStations(this.getActiveStations());
    }

    /**
     * Remove all inactive stations
     */
    synchronized void removeAll() {
        remove(this.getInactiveStations());
    }

    /**
     * Remove an inactive station from the manager and delete its working directory.
     *
     * @param info
     * @return True if the station was successfully removed.
     */
    boolean remove(StationProcess info) {
        synchronized (info) {
            LOG.info("Removing recon station: " + info.stationName);
            if (!info.isActive()) {
                LOG.config("Deleting station work dir: " + info.getDirectory().getPath());
                try {
                    FileUtils.deleteDirectory(info.getDirectory());
                } catch (IOException e) {
                    LOG.log(Level.WARNING, "Error deleting station work dir: " + info.getDirectory().getPath(), e);
                }
                this.stations.remove(info);
                LOG.info("Removed recon station: " + info.stationName);
                return true;
            } else {
                LOG.warning("Failed to remove station because it is still active: " + info.stationName);
                return false;
            }
        }
    }

    /**
     * Remove a list of stations
     * @param stations
     * @return
     */

    public int remove(List<StationProcess> stations) {
        int tot = 0;
        for (StationProcess station : stations) {
            boolean removed = this.remove(station);
            if (removed) {
                ++tot;
            }
        }
        return tot;
    }

    /**
     * Find a list of stations by their IDs.
     *
     * @param ids The IDs of the stations to find
     * @return A list of stations
     */
    public List<StationProcess> find(List<Integer> ids) {
        List<StationProcess> stations = new ArrayList<StationProcess>();
        for (StationProcess station : this.stations) {
            if (ids.contains(station.getStationID())) {
                stations.add(station);
            }
        }
        return stations;
    }

    /**
     * Get the number of stations.
     *
     * @return The number of stations
     */
    public int getStationCount() {
        return this.stations.size();
    }

    /**
     * Start a station and return the results as a <code>Future</code>
     * @param exec The executor service
     * @param station The station to start
     * @return A <code>Future</code> with the results as a <code>Boolean</code>
     */
    private Future<Boolean> startStation(ExecutorService exec, StationProcess station) {
        return exec.submit(() -> {
            return station.activate(this.server);
        });
    }

    /**
     * Start a list of stations using a thread execution pool
     *
     * @param stations The list of stations to start
     * @return How many stations were actually started
     */
    public int startStations(List<StationProcess> stations) {

        // Activate stations simultaneously using a thread execution pool
        final ExecutorService exec = Executors.newFixedThreadPool(stations.size());
        final Map<StationProcess, Future<Boolean>> results =
                new HashMap<StationProcess, Future<Boolean>>();
        for (StationProcess station : stations) {
            if (!station.isActive()) {
                LOG.info("Starting station: " + station.getStationName());
                results.put(station, startStation(exec, station));
            } else {
                LOG.log(Level.WARNING, "Station is already active: " + station.getStationName());
            }
        }

        // Wait for all stations to activate
        try {
            LOG.info("Waiting for station activations...");
            double start = (double) System.currentTimeMillis();
            exec.shutdown();
            // Allow up to 5 minutes for all stations to activate (normally < 1 minute wait here)
            exec.awaitTermination(5, TimeUnit.MINUTES);
            double elapsed = ((double) System.currentTimeMillis()) - start;
            LOG.info("Station activations completed in " + elapsed/1000. + " sec");
        } catch (InterruptedException e) {
            LOG.log(Level.SEVERE, "Station activation execution was interrupted", e);
        }

        // Determine how many stations were activated successfully
        int activated = 0;
        for (Entry<StationProcess, Future<Boolean>> result : results.entrySet()) {
            try {
                if (result.getValue().get()) {
                    activated++;
                }
            } catch (InterruptedException e) {
                LOG.log(Level.SEVERE, e.getMessage(), e);
            } catch (ExecutionException e) {
                // Some error occurred when activating the station
                LOG.log(Level.SEVERE, "Station "
                        + result.getKey().getStationName()
                        + " had error during activation: " + e.getMessage(),
                        e);
            }
        }
        LOG.info("Station activations: " + activated + "/" + stations.size());
        return activated;
    }

    /**
     * Check if a station exists with given ID.
     *
     * @param id The ID of the station
     * @return True if a station with this ID exists
     */
    private boolean exists(Integer id) {
        boolean exists = false;
        for (StationProcess station : this.stations) {
            if (station.getStationID() == id) {
                exists = true;
                break;
            }
        }
        return exists;
    }

    /**
     * Get the current stationID to be used for the next station assignment, without
     * incrementing it.
     *
     * @return The current station ID
     */
    int getCurrentStationID() {
        return this.stationID;
    }

    /**
     * Create a <code>Tailer</code> for tailing the log file of a station.
     *
     * @param id          The ID of the station
     * @param listener    The <code>TailerListener</code> to be attached to the
     *                    <code>Tailer</code>
     * @param delayMillis The delay in milliseconds between reading the
     *                    <code>Tailer</code>
     * @return The <code>Tailer</code> for the station's log file
     */
    public Tailer getLogTailer(Integer id, TailerListener listener, long delayMillis) {
        if (!this.exists(id)) {
            throw new IllegalArgumentException("Station ID does not exist: " + id);
        }
        StationProcess station = this.find(id);
        if (!station.isActive()) {
            throw new RuntimeException("Station is not active: " + station.stationName);
        }
        File logFile = station.getLogFile();
        if (!logFile.exists()) {
            throw new RuntimeException("Station log file does not exist: " + logFile.getPath());
        }
        return new Tailer(logFile, listener, delayMillis, true);
    }

    /**
     * Get a list of inactive stations
     * @return A list of inactive stations
     */
    public List<StationProcess> getInactiveStations() {
        List<StationProcess> stats = new ArrayList<StationProcess>();
        for (StationProcess sp : this.getStations()) {
            if (!sp.isActive()) {
                stats.add(sp);
            }
        }
        return stats;
    }

    /**
     * Get a list of active stations
     * @return A list of active stations
     */
    public List<StationProcess> getActiveStations() {
        List<StationProcess> stats = new ArrayList<StationProcess>();
        for (StationProcess sp : this.getStations()) {
            if (sp.isActive()) {
                stats.add(sp);
            }
        }
        return stats;
    }

    /**
     * Check the status of all station's periodically (run on a scheduled executor)
     */
    class StationMonitor implements Runnable {

        public void run() {
            for (StationProcess station : StationManager.this.getStations()) {
                // Set inactive state on stations whose processes have stopped
                Process process = station.getProcess();
                if (station.isActive() && process != null && !process.isAlive()) {
                    LOG.info("Station monitor deactivating station: " + station.stationName);
                    station.deactivate(StationManager.this.server);
                    LOG.info("Station monitoring deactivated station " + station.stationName
                            + " with exit value: "
                            + station.getExitValue());
                }
            }
        }
    }
}


