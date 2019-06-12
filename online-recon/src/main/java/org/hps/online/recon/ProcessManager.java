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
public class ProcessManager {

    private static final Logger LOGGER = Logger.getLogger(ProcessManager.class.getPackageName());
    
    private static final String CONDITIONS_PROPERTY = "org.hps.conditions.url";

    private static final String LOGGING_PROPERTY = "java.util.logging.config.file";
       
    class ProcessInfo {
        
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
       
    private List<ProcessInfo> processes = new ArrayList<ProcessInfo>();
    
    private int processID = 1;
    
    private final Server server;
    
    ProcessManager(Server server) {        
        this.server = server;
    }
        
    void add(ProcessInfo info) {
        processes.add(info);
    }
       
    synchronized int getNextProcessID() {
        ++processID;
        return processID - 1;
    }
    
    void setProcessID(int processID) {
        this.processID = processID;
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
    
    void createProcess(JSONObject parameters) throws IOException {
        
        if (!parameters.has("properties")) {
            throw new RuntimeException("No properties file found in parameters.");
        }
        String propFile = parameters.getString("properties");
        
        String jarPath = OnlineRecon.class.getProtectionDomain().getCodeSource().getLocation().getPath();
          
        Integer processID = getNextProcessID();
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
        command.add(OnlineRecon.class.getCanonicalName());
        command.add("-s");
        command.add(stationName);
        command.add("-o");
        command.add(stationName.toLowerCase()); // Should output filename be set separately???
        command.add("-d");
        command.add(dir.getPath());
        command.add(propFile);
        ProcessBuilder pb = new ProcessBuilder(command);
               
        pb.directory(dir);
        File log = new File(dir.getPath() + File.separator + "out." + processID.toString() + ".log");
        pb.redirectErrorStream(true);
        pb.redirectOutput(Redirect.appendTo(log));

        LOGGER.info("Starting command " + pb.command().toString());

        // This will throw an exception if there is a problem starting the process
        // which is fine as the caller should catch and handle it.  The process 
        // won't be registered, which is also fine as it isn't valid.
        Process p = pb.start();
        
        // Add new process record.
        ProcessInfo info = new ProcessInfo();
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
    
    List<ProcessInfo> getProcesses() {
        return Collections.unmodifiableList(this.processes);
    }
    
    ProcessInfo find(int id) {
        for (ProcessInfo info : processes) {
            if (info.id == id) {
                return info;
            }
        }
        return null;
    }

    void stopProcess(ProcessInfo info) {
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
            removeProcess(info);
        }
    }
    
    void stopProcess(int id) {
        ProcessInfo info = this.find(id);
        if (info != null) {
            stopProcess(info);
        } else {
            throw new RuntimeException("Unknown process id <" + id + ">");
        }
    }
    
    synchronized void stopAll() {
        LOGGER.info("Stopping ALL processes");
        for (ProcessInfo info : this.processes) {
            stopProcess(info);
        }
    }    
    
    void removeProcess(ProcessInfo info) {
        this.processes.remove(info);
    }
}
