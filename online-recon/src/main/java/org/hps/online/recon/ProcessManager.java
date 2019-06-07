package org.hps.online.recon;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

public class ProcessManager {

    class ProcessInfo {
        Process process;
        int processNumber;
        long pid;
        String stationName;
        boolean active;
        File dir;
        File log;
    }
       
    private List<ProcessInfo> processes = new ArrayList<ProcessInfo>();
    
    private int processNumber = 0;
    
    private final Server server;
    
    ProcessManager(Server server) {        
        this.server = server;
    }
        
    void add(ProcessInfo info) {
        processes.add(info);
    }
       
    synchronized int getNextProcessNumber() {
        ++processNumber;
        return processNumber;
    }
    
    public static synchronized long getPidOfProcess(Process p) {
        long pid = -1;
        try {
            if (p.getClass().getName().equals("java.lang.UNIXProcess")) {
                Field f = p.getClass().getDeclaredField("pid");
                f.setAccessible(true);
                pid = f.getLong(p);
                f.setAccessible(false);
            }
        } catch (Exception e) {
            pid = -1;
        }
        return pid;
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
          
        Integer processNumber = getNextProcessNumber();
        String stationName = this.server.getStationBaseName() + "_" + String.format("%02d", processNumber);
        File dir = createProcessDir(stationName);
        
        // TODO: local conditions DB needs to be configurable/optional via properties and not hard-coded here
        //"-Djava.util.logging.config.file=logging.properties",
        //"-Dorg.hps.conditions.url=jdbc:sqlite:hps_conditions.db",
        
        ProcessBuilder pb = new ProcessBuilder(
                "java", 
                "-cp", jarPath, OnlineRecon.class.getCanonicalName(),
                "-s", stationName,
                propFile);
        pb.directory(dir);
        File log = new File(dir.getPath() + File.separator + "out." + processNumber.toString() + ".log");
        pb.redirectErrorStream(true);
        pb.redirectOutput(Redirect.appendTo(log));
        
        System.out.println("starting process: " + pb.command().toString());

        // This will throw an exception if there is a problem starting the process
        // which is fine as the caller should catch and handle it.  The process 
        // won't be registered, which is also fine.
        Process p = pb.start();
        
        /*
        assert pb.redirectInput() == Redirect.PIPE;
        assert pb.redirectOutput().file() == log;
        assert p.getInputStream().read() == -1;
        */
   
        // get the PID
        long pid = ProcessManager.getPidOfProcess(p);
        System.out.println("process started with pid <" + pid + ">");
        
        ProcessInfo info = new ProcessInfo();
        info.process = p;
        info.processNumber = processNumber;
        info.pid = pid;
        info.stationName = stationName;
        info.dir = dir;
        info.log = log;
        info.active = true;
        add(info);       
    }
       
    /*
    synchronized void update() {
        for (ProcessInfo info : this.processes) {
            if (!info.process.isAlive()) {
                info.active = false;
            }
        }
    }
    
    synchronized void killAll() {
        List<ProcessInfo> toRemove = new ArrayList<ProcessInfo>();
        for (ProcessInfo info : this.processes) {
            if (!info.process.isAlive()) {
                toRemove.add(info);
            }
        }
        for (ProcessInfo info : toRemove) {
            this.processes.remove(info);
        }
    }  
    
        Process find(int id) {
        Process process = null;
        for (ProcessInfo info : processes) {
            if (info.id == id) {
                process = info.process;
                break;
            }
        }
        return process;
    }
    
    Process find(String stationName) {
        Process process = null;
        for (ProcessInfo info : processes) {
            if (info.stationName.equals(stationName)) {
                process = info.process;
                break;
            }
        }
        return process;
    }
    */ 
}
