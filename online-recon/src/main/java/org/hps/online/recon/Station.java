package org.hps.online.recon;

import java.io.File;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.job.DatabaseConditionsManagerSetup;
import org.hps.job.JobManager;
import org.hps.online.recon.eventbus.OnlineEventBus;
import org.hps.online.recon.properties.Property;
import org.hps.online.recon.properties.PropertyValidationException;
import org.hps.record.LCSimEventBuilder;
import org.hps.record.et.EtConnection;
import org.lcsim.conditions.ConditionsManager.ConditionsNotFoundException;
import org.lcsim.job.ConditionsSetup;

/**
 * Online reconstruction station which processes EVIO events from the ET system
 * by running LCIO reconstruction on them
 */
public class Station {

    /**
     * Class logger
     */
    private static Logger LOG = Logger.getLogger(Station.class.getPackage().getName());

    /**
     * The station properties
     */
    private StationProperties props = new StationProperties();

    /**
     * Reference to the lcsim job manager
     */
    private JobManager mgr;

    /**
     * Reference to the EVIO event builder
     */
    private LCSimEventBuilder builder;

    /**
     * The ET connection for managing the station
     */
    private EtConnection conn;

    /**
     * The name of the station in the ET system
     */
    private String stationName;

    /**
     * The conditions system setup
     */
    private DatabaseConditionsManagerSetup conditionsSetup;


    private OnlineEventBus eventbus;

    /**
     * Attribute for setting station name in the environment
     */
    public static final String STAT_NAME_KEY = "org.hps.online.recon.stationName";

    /**
     * Attribute for setting the remote tree bind in a Driver
     */
    public static final String RTB_KEY = "org.hps.online.recon.remoteTreeBind";

    /**
     * Create new online reconstruction station with given properties
     * @param config The station properties
     */
    Station(StationProperties props) {
        this.props = props;
    }

    /**
     * Get the configuration properties of the station
     * @return The configuration of the station
     */
    public StationProperties getProperties() {
        return this.props;
    }

    /**
     * Get the job manager
     * @return The job manager
     */
    public JobManager getJobManager() {
        return mgr;
    }

    /**
     * Get the event builder
     * @return The event builder
     */
    public LCSimEventBuilder getEventBuilder() {
        return builder;
    }

    /**
     * Get the conditions setup for the station
     * @return The conditions setup for the station
     */
    public ConditionsSetup getConditionsSetup() {
        return this.conditionsSetup;
    }

    /**
     * Get the name of the station in the ET system
     * @return The name of the station in the ET system
     */
    public String getStationName() {
        return stationName;
    }

    /**
     * Get the ET connection
     * @return The ET connection
     */
    public EtConnection getEtConnection() {
        return this.conn;
    }

    /**
     * Run from the command line
     * @param args The command line arguments
     */
    public static void main(String args[]) {
        if (args.length == 0) {
            throw new RuntimeException("Missing configuration properties file");
        }
        StationProperties props = new StationProperties();
        props.load(new File(args[0]));
        Station stat = new Station(props);
        stat.setup();
        stat.process();
    }

    /**
     * Perform setup of the station using the supplied {@link StationProperties}
     */
    void setup() {

        LOG.info("Started station setup: " + new Date().toString());

        this.stationName = props.get("et.stationName").value().toString();
        LOG.info("Initializing station: " + stationName);

        LOG.config("Station properties: " + props.toJSON().toString());

        try {
            LOG.config("Validating station properties...");
            props.validate();
        } catch (PropertyValidationException e) {
            // Station cannot be started with invalid properties.
            LOG.log(Level.SEVERE, "Station properties are not valid", e);
            throw new RuntimeException(e);
        }
        LOG.config("Station properties validated");

        // Get all the properties needed to configure the station
        Property<String> detector = props.get("lcsim.detector");
        Property<Integer> run = props.get("lcsim.run");
        Property<String> outputDir = props.get("station.outputDir");
        Property<String> outputName = props.get("station.outputName");
        Property<String> steering = props.get("lcsim.steering");
        Property<String> tag = props.get("lcsim.tag");
        Property<String> builderClass = props.get("lcsim.builder");
        Property<String> conditionsUrl = props.get("lcsim.conditions");

        // Conditions URL
        if (conditionsUrl.valid()) {
            System.setProperty("org.hps.conditions.url", conditionsUrl.value());
            LOG.config("Conditions URL: " + conditionsUrl.value());
        }

        // Setup the condition system from properties.
        conditionsSetup = new DatabaseConditionsManagerSetup();
        conditionsSetup.setDetectorName(detector.value());
        if (run.value() != null) {
            conditionsSetup.setRun(run.value());
            conditionsSetup.setFreeze(true);
        }

        // Condition tag
        if (tag.valid()) {
            Set<String> tags = new HashSet<String>();
            tags.add(tag.value());
            conditionsSetup.setTags(tags);
        }
        LOG.config("Conditions will be initialized: detector=" + detector.value()
                + ", run=" + run.value() + ", tag=" + tag.value());

        // Setup event builder and register with conditions system.
        LOG.config("Creating event builder: " + builderClass.value());
        this.builder = null;
        try {
            builder = LCSimEventBuilder.class.cast(Class.forName(builderClass.value()).getConstructor().newInstance());
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to create event builder: " + builderClass.value(), e);
        }
        conditionsSetup.addConditionsListener(builder);
        LOG.config("Done creating event builder");

        // Setup the lcsim job manager.
        LOG.config("Initializing job manager...");
        this.mgr = new JobManager();
        mgr.setDryRun(true);
        final String outputFilePath = outputDir.value() + File.separator + outputName.value();
        LOG.config("Output file path: " + outputFilePath);
        mgr.addVariableDefinition("outputFile", outputFilePath);

        // Setup job steering
        if (steering.value().startsWith("file://")) {
            String steeringPath = steering.value().replace("file://", "");
            LOG.config("Setting up steering file: " + steeringPath);
            mgr.setup(new File(steeringPath));
        } else {
            LOG.config("Setting up steering resource: " + steering.value());
            mgr.setup(steering.value());
        }

        // Activate the conditions system.
        LOG.config("Initializing conditions system...");
        conditionsSetup.configure();
        try {
            conditionsSetup.setup();
        } catch (ConditionsNotFoundException e) {
            throw new RuntimeException(e);
        }
        conditionsSetup.postInitialize();
        LOG.config("Conditions system initialized successfully");

        // Try to connect to the ET system.
        try {
            this.conn = new EtParallelStation(props);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create ET station", e);
        }

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                // Cleanup the ET connection on exit
                if (conn != null) {
                    conn.cleanup();
                }
            }
        });

        LOG.info("Finished station setup: " + new Date().toString());
    }

    /**
     * Run the online reconstruction station by streaming ET events
     * using the {@link org.hps.online.recon.eventbus.OnlineEventBus}
     */
    void process() {
        LOG.info("Started processing: " + new Date().toString());
        eventbus = new OnlineEventBus(this);
        eventbus.loop();
        LOG.info("Ended processing: " + new Date().toString());
    }
}
