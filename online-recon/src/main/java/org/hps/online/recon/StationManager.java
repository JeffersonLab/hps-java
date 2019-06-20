package org.hps.online.recon;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import org.json.JSONObject;

/**
 * Manages online reconstruction system processes.
 */
public class StationManager {

    class StationInfo {
        
        volatile boolean alive;
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
            jo.put("alive", alive);
            jo.put("id", id);
            jo.put("station", stationName);
            jo.put("command", String.join(" ", command));
            jo.put("dir", dir.getPath());
            jo.put("log", log.getPath());
            return jo;
        }
    }
    
    private static final String CONDITIONS_PROPERTY = "org.hps.conditions.url";

    private static final Logger LOGGER = Logger.getLogger(StationManager.class.getPackageName());
       
    private static final String LOGGING_PROPERTY = "java.util.logging.config.file";
    
    static Long getPid(Process p) {
        long pid = -1;       
        try {
            Field f = p.getClass().getDeclaredField("pid");
            f.setAccessible(true);
            pid = f.getLong(p);
            f.setAccessible(false);
        } catch (Exception e) {
            pid = -1;
        }
        return pid;
    }
       
    private final Server server;
    
    private volatile int stationID = 1;
    
    private final List<StationInfo> stations = Collections.synchronizedList(
            new ArrayList<StationInfo>());
    
    StationManager(Server server) {        
        this.server = server;
    }
        
    void add(StationInfo info) {
        stations.add(info);
    }
       
    synchronized File createProcessDir(String name) {
        String path = server.getWorkDir().getPath() + File.separator + name;
        File dir = new File(path);
        dir.mkdir();
        if (!dir.isDirectory()) {
            throw new RuntimeException("Error creating dir: " + dir.getPath());
        }
        return dir;
    }
    
    void createStation(JSONObject parameters) throws IOException {
        
        StationConfiguration stationConfig = server.getStationConfig();
        
        String jarPath = OnlineReconStation.class.getProtectionDomain().getCodeSource().getLocation().getPath();
          
        Integer processID = getNextStationID();
        String stationName = this.server.getStationBaseName() + "_" + String.format("%03d", processID);
        File dir = createProcessDir(stationName);
        
        Properties prop = System.getProperties();
                
        List<String> command = new ArrayList<String>();
        command.add("java");        
        if (prop.containsKey(LOGGING_PROPERTY)) {
            String logProp = prop.getProperty(LOGGING_PROPERTY);
            if (new File(logProp).isAbsolute()) {
                command.add("-D" + LOGGING_PROPERTY + "=" + logProp);
            }
        }
        if (prop.containsKey(CONDITIONS_PROPERTY)) {
            String condProp = prop.getProperty(CONDITIONS_PROPERTY);
            if (new File(condProp.replaceAll("jdbc:sqlite:", "")).isAbsolute()) {
                command.add("-D" + CONDITIONS_PROPERTY + "=" + condProp);
            }
        }
        command.add("-cp");
        command.add(jarPath);
        command.add(OnlineReconStation.class.getCanonicalName());
        
        // TODO: This command string could be created via a utility method in the configuration class.
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
                 
        ProcessBuilder pb = new ProcessBuilder(command);
               
        pb.directory(dir);
        File log = new File(dir.getPath() + File.separator + "out." + processID.toString() + ".log");
        pb.redirectErrorStream(true);
        pb.redirectOutput(Redirect.appendTo(log));

        LOGGER.info("Starting command: " + String.join(" ", pb.command()));

        // This will throw an exception if there is a problem starting the process
        // which is fine as the caller should catch and handle it.  The process 
        // won't be registered, which is also fine as it isn't valid.
        Process p = pb.start();
        
        // Add new station info.
        StationInfo info = new StationInfo();
        info.process = p;
        info.pid = getPid(p);
        info.id = processID;
        info.stationName = stationName;
        info.dir = dir;
        info.log = log;
        info.command = command;
        info.alive = true;
        add(info);       
    }
                
    StationInfo findStation(int id) {
        for (StationInfo info : stations) {
            if (info.id == id) {
                return info;
            }
        }
        return null;
    }
    
    synchronized int getNextStationID() {
        ++stationID;
        return stationID - 1;
    }
    
    List<StationInfo> getStations() {
        return Collections.unmodifiableList(this.stations);
    }
    
    synchronized void setStationID(int stationID) {
        this.stationID = stationID;
        LOGGER.config("Set station ID: " + stationID);
    }
    
    synchronized void stopAll() {
        LOGGER.info("Stopping ALL stations!");
        for (StationInfo info : this.stations) {
            stopStation(info);
        }
        LOGGER.info("Stopped ALL stations!");
    }
    
    void stopStation(int id) {
        LOGGER.info("Stopping station with id: " + id);
        StationInfo info = this.findStation(id);
        if (info != null) {
            stopStation(info);
        } else {
            throw new RuntimeException("Unknown process id: " + id);
        }
    }    
    
    /**
     * Stop a station.
     * 
     * This does not remove it from the station list.  The 'remove' command
     * must be used to do this separately.
     * 
     * @param info
     */
    synchronized void stopStation(StationInfo info) {
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
                info.alive = false;
                LOGGER.info("Stopped station: " + info.stationName);
            } else {
                LOGGER.severe("Failed to stop station: " + info.stationName);
                // FIXME: Should this throw an exception???
            }
        }
    }
    
    /**
     * Remove a station from the manager.
     * @param info
     * @return True if the station was successfully removed.
     */
    boolean removeStation(StationInfo info) {
        LOGGER.info("Removing station: " + info.stationName);
        updateStation(info);
        if (info.alive == false) {
            this.stations.remove(info);
            LOGGER.info("Done removing station: " + info.stationName);
            return true;
        } else {
            LOGGER.warning("Failed to remove station " + info.stationName + " because it is still alive!");
            return false;
        }
    }

    /**
     * Remove a list of stations by their IDs. 
     * @param ids
     * @return The number of stations successfully removed.
     */
    synchronized int removeStations(List<Integer> ids) {
        int n = 0;
        List<StationInfo> stations = findStations(ids);
        for (StationInfo station : stations) {
            if (removeStation(station)) {
                ++n;
            }
        }
        return n;
    }
    
    /**
     * Remove all stations, returning number of stations removed.
     * @return The number of stations removed.
     */
    synchronized int removeAllStations() {
        return removeStations(getStationIDs());
    }
        
    // FIXME: Should have some kind of exception/error if ID does not exist.
    private List<StationInfo> findStations(List<Integer> ids) {
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
    
    private void updateStation(StationInfo station) {
        if (!station.process.isAlive()) {
            station.alive = false;
        }
    }
    
    private boolean stationExists(Integer id) {
        boolean exists = false;
        for (StationInfo station : this.stations) {
            if (station.id == id) {
                exists = true;
                break;
            }
        }
        return exists;
    }
}
