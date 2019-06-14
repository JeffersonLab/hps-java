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

    private static final Logger LOGGER = Logger.getLogger(StationManager.class.getPackageName());
    
    private static final String CONDITIONS_PROPERTY = "org.hps.conditions.url";

    private static final String LOGGING_PROPERTY = "java.util.logging.config.file";
       
    class StationInfo {
        
        Process process;
        long pid;
        int id;
        String stationName;
        boolean active;
        File dir;
        File log;
        List<String> command;
        
        JSONObject toJSON() {
            JSONObject jo = new JSONObject();
            jo.put("pid", pid);
            jo.put("alive", process.isAlive());
            jo.put("id", id);
            jo.put("station", stationName);
            jo.put("command", String.join(" ", command));
            jo.put("dir", dir.getPath());
            jo.put("log", log.getPath());
            return jo;
        }
    }
    
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
       
    private volatile List<StationInfo> stations = new ArrayList<StationInfo>();
    
    private volatile int stationID = 1;
    
    private final Server server;
    
    StationManager(Server server) {        
        this.server = server;
    }
        
    synchronized void add(StationInfo info) {
        stations.add(info);
    }
       
    synchronized int getNextStationID() {
        ++stationID;
        return stationID - 1;
    }
    
    void setStationID(int stationID) {
        this.stationID = stationID;
    }
                
    File createProcessDir(String name) {
        String path = server.getWorkDir().getPath() + File.separator + name;
        File dir = new File(path);
        dir.mkdir();
        if (!dir.isDirectory()) {
            throw new RuntimeException("Error creating dir <" + dir.getPath() + ">");
        }
        return dir;
    }
    
    synchronized void createStation(JSONObject parameters) throws IOException {
        
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
        command.add("-r");
        command.add(stationConfig.getRunNumber().toString());
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

        LOGGER.info("Starting command <" + String.join(" ", pb.command()) + ">");

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
        info.active = true;
        add(info);       
    }
    
    synchronized List<StationInfo> getStations() {
        return Collections.unmodifiableList(this.stations);
    }
    
    synchronized StationInfo findStation(int id) {
        for (StationInfo info : stations) {
            if (info.id == id) {
                return info;
            }
        }
        return null;
    }

    synchronized void stopStation(StationInfo info) {
        if (info != null) {
            Process p = info.process;
            LOGGER.info("Stopping process <" + info.id + ">");
            p.destroy();
            try {
                p.waitFor();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (!p.isAlive()) {
                LOGGER.info("Stopped process <" + info.id + ">");
            } else {
                LOGGER.severe("Failed to stop process <" + info.id + ">");
            }
            LOGGER.info("Removing process <" + info.id + ">");
            removeStation(info);
        }
    }
    
    synchronized void stopStation(int id) {
        StationInfo info = this.findStation(id);
        if (info != null) {
            stopStation(info);
        } else {
            throw new RuntimeException("Unknown process id <" + id + ">");
        }
    }
    
    synchronized void stopAll() {
        LOGGER.info("Stopping ALL stations");
        for (StationInfo info : this.stations) {
            stopStation(info);
        }
    }    
    
    private synchronized void removeStation(StationInfo info) {
        this.stations.remove(info);
    }
}
