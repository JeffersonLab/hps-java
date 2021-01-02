package org.hps.online.recon;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FilenameUtils;
import org.hps.online.recon.InlineAggregator.RemoteTreeBindThread;
import org.hps.online.recon.logging.LoggingConfig;
import org.hps.online.recon.properties.Property;
import org.json.JSONObject;

/**
 * Manages the system process of a {@link Station}
 */
public class StationProcess {

    static Logger LOG = Logger.getLogger(StationProcess.class.getPackage().getName());

    /**
     * Name of station properties file.
     */
    private static final String STATION_CONFIG_NAME = "station.properties";
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

    RemoteTreeBindThread rtbThread = null;

    StationProcess(Integer id, String stationName, File dir, StationProperties props) {
        this.id = id;
        this.stationName = stationName;
        this.dir = dir;
        this.props = props;
    }

    /**
     * Convert station data to JSON.
     * @return The converted JSON data
     */
    public JSONObject toJSON() {
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

    synchronized void activate(/*Server server*/) throws IOException {

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
        LOG.info("Started process: " + pid);

        setActive(true);

        LOG.info("Setting station to active (connection to remote tree might be delayed)");
    }

    synchronized void mountRemoteTree(InlineAggregator agg) {
        killRemoteTreeBindThread();
        if (this.props.get("lcsim.remoteTreeBind").valid()) {
            LOG.fine("Starting remoteTreeBind connection thread");
            rtbThread = agg.new RemoteTreeBindThread(this, null);
            rtbThread.start();
        }
    }

    synchronized void killRemoteTreeBindThread() {
        if (rtbThread != null && rtbThread.isAlive()) {
            rtbThread.interrupt();
            try {
                rtbThread.join();
            } catch (InterruptedException e) {
                LOG.log(Level.WARNING, "Interrupted", e);
            }
            rtbThread = null;
        }
    }

    // Set station info to indicate that it is inactive with no valid process
    synchronized void deactivate(/*Server server*/) {

        LOG.info("Deactivating station: " + stationName);

        if (!active) {
            LOG.warning("Station has already been deactivated: " + stationName);
            return;
        }

        setActive(false);

        // Dismount the station's AIDA tree
        //unmountRemoteTree(server.agg);

        destroyProcess();

        LOG.info("Done deactivating station: " + stationName);
    }

    synchronized void setActive(boolean active) {
        this.active = active;
    }

    boolean isActive() {
        return this.active;
    }

    private void destroyProcess() {
        // Destroy the station's system process
        try {
            if (process != null) {
                if (process.isAlive()) {
                    LOG.fine("Killing process: " + pid);
                    //process.destroy();
                    process.destroyForcibly();
                    try {
                        LOG.fine("Waiting for station to stop: " + stationName);
                        process.waitFor(30, TimeUnit.SECONDS);
                        LOG.fine("Done waiting for station to stop: " + stationName);
                        if (process.isAlive()) {
                            LOG.warning("Station did not stop after 30 seconds");
                        } else {
                            exitValue = process.exitValue();

                        }
                    } catch (InterruptedException e) {
                        LOG.log(Level.WARNING, "Interrupted", e);
                    }
                } else {
                    exitValue = process.exitValue();
                }
                process = null;
            }
            pid = -1L;
            LOG.fine("Exit value: " + exitValue);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Error killing station process", e);
        }
    }

    void unmountRemoteTree(InlineAggregator agg) {

        // Kill the RTB thread if active
        killRemoteTreeBindThread();

        // Unmount the remote tree for this station
        if (props.get("lcsim.remoteTreeBind").valid()) {
            Property<String> remoteTreeBind = props.get("lcsim.remoteTreeBind");
            try {
                agg.unmount(remoteTreeBind.value());
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Error unmounting AIDA tree", e);
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

    Integer getStationID() {
        return this.id;
    }

    File getDirectory() {
        return this.dir;
    }

    Process getProcess() {
        return process;
    }

    void setExitValue(int exitValue) {
        this.exitValue = exitValue;
    }

    int getExitValue() {
        return this.exitValue;
    }

    void setConfigFile(File file) {
        this.configFile = file;
    }

    List<String> getCommand() {
        return this.command;
    }

    public String getStationName() {
        return this.stationName;
    }

    public File getLogFile() {
        return this.log;
    }

    public StationProperties getProperties() {
        return this.props;
    }

    void buildCommand() {

        Property<String> logConfigFile = props.get("station.loggingConfig");

        command = new ArrayList<String>();

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
        command.add(configFile.getPath());
    }

}