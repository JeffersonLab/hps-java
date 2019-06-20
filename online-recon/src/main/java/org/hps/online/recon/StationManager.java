package org.hps.online.recon;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.json.JSONObject;

/**
 * Manages online reconstruction stations.
 */
public class StationManager {

    /**
     * Information for managing a single online reconstruction station.
     */
    class StationInfo {
        
        volatile boolean active;
        List<String> command;
        File dir;
        int id;
        File log;
        long pid;
        Process process;
        String stationName;
        
        JSONObject toJSON() {
            JSONObject jo = new JSONObject();
            jo.put("pid", pid);
            jo.put("active", active);
            jo.put("id", id);
            jo.put("station", stationName);
            jo.put("command", String.join(" ", command));
            jo.put("dir", dir.getPath());
            jo.put("log", log != null ? log.getPath() : "");
            return jo;
        }
    }
    
    private static final String CONDITIONS_PROPERTY = "org.hps.conditions.url";

    private static final Logger LOGGER = Logger.getLogger(StationManager.class.getPackageName());
       
    private static final String LOGGING_PROPERTY = "java.util.logging.config.file";

    private static final String JAR_PATH = 
            OnlineReconStation.class.getProtectionDomain().getCodeSource().getLocation().getPath();
    
    private static final Properties SYSTEM_PROPERTIES = System.getProperties();
    
    static Long getPid(Process p) {
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
       
    private final Server server;
    
    private volatile int stationID = 1;

    /**
     * Define station list as synchronized because we do not want different
     * threads mucking with it at the same time.
     */
    private final List<StationInfo> stations = Collections.synchronizedList(
            new ArrayList<StationInfo>());
    
    StationManager(Server server) {        
        this.server = server;
    }
        
    void add(StationInfo info) {
        stations.add(info);
    }
       
    synchronized File createStationDir(String name) {
        String path = server.getWorkDir().getPath() + File.separator + name;
        File dir = new File(path);
        dir.mkdir();
        if (!dir.isDirectory()) {
            throw new RuntimeException("Error creating dir: " + dir.getPath());
        }
        return dir;
    }
    
    synchronized void start(StationInfo station) throws IOException {
        
        LOGGER.info("Starting station: " + station.stationName);
        
        update(station);
                
        if (!station.active) {
                
            List<String> command = station.command;
            File dir = station.dir;
            Integer stationID = station.id;

            ProcessBuilder pb = new ProcessBuilder(command);

            pb.directory(dir);
            File log = new File(dir.getPath() + File.separator + "out." + stationID.toString() + ".log");
            pb.redirectErrorStream(true);
            pb.redirectOutput(Redirect.appendTo(log));

            station.log = log;

            LOGGER.info("Starting command: " + String.join(" ", pb.command()));        

            // Can throw exception.
            Process p = pb.start();

            station.process = p;
            station.pid = getPid(p);
            station.active = true;
        
            LOGGER.info("Successfully started station: " + station.stationName);
        } else {
            LOGGER.warning("Station is already active: " + station.stationName);
        }            
    }
    
    StationInfo create(JSONObject parameters) {
        
        StationConfiguration stationConfig = server.getStationConfig();
                  
        Integer stationID = getNextStationID();
        String stationName = this.server.getStationBaseName() + "_" + String.format("%03d", stationID);
        File dir = createStationDir(stationName);
                       
        List<String> command = buildCommand(stationConfig, stationName, dir);
                        
        // Add new station info.
        StationInfo info = new StationInfo();
        info.id = stationID;
        info.stationName = stationName;
        info.dir = dir;
        info.command = command;        
        add(info);
        
        return info;
    }
    
    /**
     * Build the command for running the station.
     * @param stationConfig The station configuration
     * @param stationName The unique name of the station
     * @param dir The station's output directory
     * @return A command list to be sent to the ProcessBuilder
     */
    private List<String> buildCommand(StationConfiguration stationConfig,
            String stationName, File dir) {
        List<String> command = new ArrayList<String>();
        command.add("java");        
        if (SYSTEM_PROPERTIES.containsKey(LOGGING_PROPERTY)) {
            String logProp = SYSTEM_PROPERTIES.getProperty(LOGGING_PROPERTY);
            if (new File(logProp).isAbsolute()) {
                command.add("-D" + LOGGING_PROPERTY + "=" + logProp);
            }
        }
        if (SYSTEM_PROPERTIES.containsKey(CONDITIONS_PROPERTY)) {
            String condProp = SYSTEM_PROPERTIES.getProperty(CONDITIONS_PROPERTY);
            if (new File(condProp.replaceAll("jdbc:sqlite:", "")).isAbsolute()) {
                command.add("-D" + CONDITIONS_PROPERTY + "=" + condProp);
            }
        }
        command.add("-cp");
        command.add(JAR_PATH);
        command.add(OnlineReconStation.class.getCanonicalName());
        
        command.add("-d");
        command.add(stationConfig.getDetectorName());
        command.add("-s");
        command.add(stationConfig.getSteeringResource());        
        if (stationConfig.getRunNumber() != null) {
            command.add("-r");
            command.add(stationConfig.getRunNumber().toString());
        }
        command.add("-p");
        command.add(stationConfig.getPort().toString());
        command.add("-h");
        command.add(stationConfig.getHost());
        command.add("-n");
        command.add(stationName);
        command.add("-o");
        command.add(stationName.toLowerCase());
        command.add("-l");
        command.add(dir.getPath());
        command.add("-P");
        command.add(stationConfig.getEventPrintInterval().toString());
        command.add("-e");
        command.add(stationConfig.getEventSaveInterval().toString());
        
        return command;
    }
                
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
    
    synchronized int getNextStationID() {
        ++stationID;
        return stationID - 1;
    }
    
    /**
     * Get an unmodifiable list of stations.
     * @return
     */
    List<StationInfo> getStations() {
        return Collections.unmodifiableList(this.stations);
    }
    
    synchronized boolean setStationID(int stationID) {
        if (stationID >= 0) {
            this.stationID = stationID;
            LOGGER.config("Set station ID: " + stationID);
            return true;
        } else {
            LOGGER.warning("Ignored bad station ID arg: " + stationID);
            return false;
        }
    }
    
    /**
     * Stop all stations.
     * @return The number of stations stopped
     */
    synchronized int stopAll() {
        LOGGER.info("Stopping ALL stations!");
        int n = 0;
        for (StationInfo info : this.stations) {
            if (stop(info)) {
                ++n;
            }
        }
        LOGGER.info("Stopped ALL stations!");
        return n;
    }
    
    /**
     * Stop a station by its ID.
     * @param id
     * @return
     */
    boolean stop(int id) {
        LOGGER.info("Stopping station with id: " + id);
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
     * @param info
     */
    synchronized boolean stop(StationInfo info) {
        boolean success = false;
        if (info != null) {
            Process p = info.process;
            LOGGER.info("Stopping station: " + info.stationName);
            p.destroy();
            try {
                LOGGER.fine("Waiting for station " + info.stationName + " to stop");
                p.waitFor();
                LOGGER.fine("Done waiting for station " + info.stationName + " to stop!");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (!p.isAlive()) {
                info.active = false;
                LOGGER.info("Stopped station: " + info.stationName);
                success = true;
            } else {
                LOGGER.severe("Failed to stop station: " + info.stationName);
                // FIXME: Should this throw an exception???
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
        LOGGER.info("Removing station: " + info.stationName);
        update(info);
        if (info.active == false) {
            this.stations.remove(info);
            LOGGER.info("Done removing station: " + info.stationName);
            return true;
        } else {
            LOGGER.warning("Failed to remove station " + info.stationName + " because it is still active!");
            return false;
        }
    }

    /**
     * Remove a list of stations by their IDs. 
     * @param ids
     * @return The number of stations successfully removed.
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
     * @param ids
     * @return
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
     * @return The number of stations removed.
     */
    synchronized int removeAll() {
        return remove(getStationIDs());
    }
        
    // FIXME: Should have some kind of exception/error if ID does not exist.
    private List<StationInfo> find(List<Integer> ids) {
        List<StationInfo> stations = new ArrayList<StationInfo>();
        for (StationInfo station : this.stations) {
            if (ids.contains(station.id)) {
                stations.add(station);
            }
        }        
        return stations;
    }
    
    private List<Integer> getStationIDs() {
        List<Integer> ids = new ArrayList<Integer>();
        for (StationInfo station : this.stations) {
            ids.add(station.id);
        }
        return ids;
    }
    
    synchronized private void update(StationInfo station) {
        if (station.process != null) {
            if (!station.process.isAlive()) {
                station.active = false;
            }
        }
    }
    
    int getStationCount() {
        return this.stations.size();
    }
    
    synchronized int getActiveCount() {
        int n = 0;
        for (StationInfo station : this.stations) {
            update(station);
            if (station.active) {
                ++n;
            }
        }
        return n;
    }
    
    synchronized int getInactiveCount() {
        int n = 0;
        for (StationInfo station : this.stations) {
            update(station);
            if (!station.active) {
                ++n;
            }
        }
        return n;
    }
    
    synchronized int startAll() {
        int started = 0;
        for (StationInfo station : this.stations) {
            update(station);
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
    
    synchronized int start(List<Integer> ids) {
        int started = 0;
        List<StationInfo> stations = this.find(ids);
        for (StationInfo station : stations) {
            try {
                start(station);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Failed to start station: " + station.stationName, e);
            }
            ++started;
        }
        return started;
    }
    
    synchronized boolean cleanup(StationInfo station) {
        LOGGER.info("Cleaning up station: " + station.stationName);
        update(station);
        boolean deleted = false;
        if (!station.active) {
            try {
                LOGGER.info("Deleting station work dir: " + station.dir.getPath());
                FileUtils.deleteDirectory(station.dir);
                deleted = true;
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Failed to cleanup station: " + station.stationName, e);
            }
        } else {
            LOGGER.warning("Cannot cleanup station " + station.stationName + " which is still active.");
        }
        LOGGER.info("Done cleaning up station: " + station.stationName);
        return deleted;
    }
    
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
    
    /*
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
    */
}
