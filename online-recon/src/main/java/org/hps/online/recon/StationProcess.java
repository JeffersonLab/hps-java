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

    /**
     * Name of the station
     */
    String stationName;

    /**
     * Server's ID of the station
     */
    private int id;

    /**
     * Whether the station has an active system process or not
     */
    private boolean active = false;

    /**
     * The station's system process
     */
    private Process process;

    /**
     * The PID of the station process
     */
    private long pid = -1L;

    /**
     * The exit value of the process
     */
    private int exitValue = -1;

    /**
     * The command for running the station
     */
    private List<String> command;

    /**
     * The log file for the station
     */
    private File log;

    /**
     * The run directory for the station
     */
    private File dir;

    /**
     * The input station properties file for the station
     */
    private File configFile;

    /**
     * The station's key-value properties
     */
    private StationProperties props;

    /**
     * Create a station process
     * @param id The ID of the station
     * @param stationName The name of the station
     * @param dir The station's directory
     * @param props The station's config properties
     */
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

    /**
     * Activate the reconstruction station, which includes starting its system process
     * and connecting its remote AIDA tree
     *
     * @throws IOException If there is a problem activating the station's process
     * @thows InterruptedException If station activation is interrupted
     */
    synchronized boolean activate(Server server) throws IOException, InterruptedException {

        LOG.info("Activating station: " + stationName);

        // Make sure station directory exists.
        if (!dir.exists()) {
            LOG.info("Recreating missing station dir: " + dir.getPath());
            dir.mkdir();
        }

        if (!configFile.exists()) {
            // The configuration file disappeared, so recreate it
            LOG.info("Rewriting station config file: " + configFile.getPath());
            writeStationProperties(props, dir, stationName);
        }

        // Setup the system process for the station
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(dir);
        log = new File(dir.getPath() + File.separator + "out." + Integer.valueOf(id).toString() + ".log");
        if (log.exists()) {
            if (log.delete()) {
                LOG.info("Deleted old station log file: " + log.getPath());
            } else {
                LOG.warning("Failed to delete old station log file " + log.getPath());
            }
        }
        pb.redirectErrorStream(true);
        pb.redirectOutput(Redirect.appendTo(log));
        LOG.info("Station command: " + '\n' + String.join(" ", pb.command()) +'\n');

        // This can throw an exception which is fine
        process = pb.start();
        pid = getPid(process);
        LOG.info("Started station process: " + pid);

        // Attempt to mount the station's remote tree which may fail with an exception
        server.getAggregator().addRemoteTree(getRemoteTreeBind());

        // Set active flag
        setActive(true);

        LOG.info("Activated station successfully: " + this.stationName);

        // This is returned for a result when using a Future
        return this.active;
    }

    /**
     * Deactivate a station by unmounting its remote AIDA tree and killing its system process.
     *
     * The ET station will be removed when the {@link Station} process actually exits.
     *
     * This is called by {@link StationManager#stopStation(StationProcess)}.
     */
    synchronized void deactivate(Server server) {

        LOG.log(Level.INFO, "Deactivating station: " + stationName);

        if (!active) {
            LOG.warning("Station was already deactivated: " + stationName);
            return;
        }

        // Set active flag to false
        setActive(false);

        // Unmount the remote AIDA tree
        try {
            server.getAggregator().unmount(getRemoteTreeBind());
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error unmounting remote tree: " + getRemoteTreeBind(), e);
        }

        // Wake up the ET station which signals the event loop to exit
        server.wakeUp(this);

        // Destroy the station's system process
        LOG.info("Destroying station's system process: " + this.getPid());
        destroyProcess();

        LOG.info("Done deactivating station: " + stationName);
    }

    /**
     * Set the station to active
     * @param active True to set station to active
     */
    void setActive(boolean active) {
        this.active = active;
    }

    /**
     * Get whether the station is active or not
     * @return True if station is active
     */
    boolean isActive() {
        return this.active;
    }

    /**
     * Destroy the station's system process
     */
    private void destroyProcess() {
        try {
            if (process != null) {
                // Wait a little bit, given that process is probably exiting already
                process.waitFor(10, TimeUnit.SECONDS);
                if (process.isAlive()) {
                    LOG.fine("Destroying process: " + pid);
                    // First try gentle exit
                    process.destroy();
                    try {
                        process.waitFor(30, TimeUnit.SECONDS);
                        if (process.isAlive()) {
                            LOG.fine("Destroying process forcibly: " + pid);
                            // Destroy forcibly e.g. using kill -9
                            process.destroyForcibly();
                            process.waitFor(10, TimeUnit.SECONDS);
                        }
                        if (!process.isAlive()) {
                            exitValue = process.exitValue();
                        } else {
                            // It really shouldn't ever get here
                            LOG.warning("Failed to stop process: " + pid);
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
            LOG.log(Level.WARNING, "Error destroying station system process", e);
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

    /**
     * Get the PID of the station's system process
     * @return The station's system process PID
     */
    Long getPid() {
        return getPid(this.process);
    }

    /**
     * Get the station ID
     * @return The station ID
     */
    Integer getStationID() {
        return this.id;
    }

    /**
     * Get the station directory
     * @return The station directory
     */
    File getDirectory() {
        return this.dir;
    }

    /**
     * Get the station's system process
     * @return The station's system process
     */
    Process getProcess() {
        return process;
    }

    /**
     * Set the exit value
     * @param exitValue The exit value
     */
    void setExitValue(int exitValue) {
        this.exitValue = exitValue;
    }

    /**
     * Get the exit value
     * @return The exit value
     */
    int getExitValue() {
        return this.exitValue;
    }

    /**
     * Set the config properties file
     * @param file The config properties file
     */
    void setConfigFile(File file) {
        this.configFile = file;
    }

    /**
     * Get the command for running the station's system process
     * @return The command for running the station's system process
     */
    List<String> getCommand() {
        return this.command;
    }

    /**
     * Get the name of the station
     * @return The name of the station
     */
    public String getStationName() {
        return this.stationName;
    }

    /**
     * Get the log file for the station
     * @return The log file for the station
     */
    public File getLogFile() {
        return this.log;
    }

    /**
     * Get the station's config properties
     * @return The station's config properties
     */
    public StationProperties getProperties() {
        return this.props;
    }

    /**
     * Get the AIDA remote tree bind URL for the station
     * @return The AIDA remote tree bind
     */
    String getRemoteTreeBind() {
        Property<String> prop = props.get("lcsim.remoteTreeBind");
        return prop.value();
    }

    /**
     * Build the command for running the station
     */
    void buildCommand() {

        Property<String> logConfigFile = props.get("station.loggingConfig");

        command = new ArrayList<String>();

        command.add("java");

        // Set JVM arguments from the station properties
        Property<String> jvmArgs = props.get("lcsim.jvm_args");
        command.add(jvmArgs.value());

        // Logging configuration
        if (logConfigFile.valid()) {
            command.add("-Djava.util.logging.config.file=" + logConfigFile.value());
        } else {
            command.add("-Djava.util.logging.config.class=" + LoggingConfig.class.getCanonicalName());
        }

        // Set the station name
        command.add("-D" + Station.STAT_NAME_KEY + "=" + this.stationName);

        // Set the AIDA remote tree binding
        command.add("-D" + Station.RTB_KEY + "=" + this.getRemoteTreeBind());

        command.add("-cp");
        if (props.get("lcsim.classpath").valid()) {
            // Set classpath from user setting
            Property<String> cp = props.get("lcsim.classpath");
            command.add(cp.value());
        } else {
            // Use default classpath from system
            command.add(System.getProperty("java.class.path"));
        }

        command.add(Station.class.getCanonicalName());
        command.add(configFile.getPath());
    }

}