package org.hps.online.recon;

import java.io.File;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.job.DatabaseConditionsManagerSetup;
import org.hps.job.JobManager;
import org.hps.online.recon.eventbus.OnlineEventBus;
import org.hps.online.recon.properties.Property;
import org.hps.online.recon.properties.PropertyValidationException;
import org.hps.record.LCSimEventBuilder;
import org.hps.record.composite.CompositeLoopConfiguration;
import org.hps.record.et.EtConnection;
import org.hps.record.evio.EvioDetectorConditionsProcessor;
import org.lcsim.conditions.ConditionsManager.ConditionsNotFoundException;
import org.lcsim.util.Driver;

/**
 * Online reconstruction station which processes events from the ET system
 * and writes intermediate plot files.
 */
// TODO: new main() using OnlineEventBus instead of composite loop
public class Station {

    /**
     * Class logger
     */
    private static Logger LOG = Logger.getLogger(Station.class.getPackage().getName());

    /**
     * The station properties
     */
    private StationProperties props = new StationProperties();

    JobManager mgr;
    LCSimEventBuilder builder;
    EtConnection conn;
    String stationName;

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
    StationProperties getProperties() {
        return this.props;
    }

    public JobManager getJobManager() {
        return mgr;
    }

    public LCSimEventBuilder getEventBuilder() {
        return builder;
    }

    public String getStationName() {
        return stationName;
    }

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
        stat.run();
    }

    void setup() {

        // Print start messages.
        LOG.info("Started station init: " + new Date().toString());

        //Property<String> stationName = props.get("et.stationName");
        this.stationName = props.get("et.stationName").value().toString();
        LOG.config("Initializing station: " + stationName);

        try {
            LOG.config("Validating station properties...");
            props.validate();
        } catch (PropertyValidationException e) {
            LOG.severe("Properties failed to validate!");
            throw new RuntimeException(e);
        }
        LOG.config("Station properties validated!");

        Property<String> detector = props.get("lcsim.detector");
        Property<Integer> run = props.get("lcsim.run");
        Property<String> outputDir = props.get("station.outputDir");
        Property<String> outputName = props.get("station.outputName");
        Property<String> steering = props.get("lcsim.steering");
        //Property<Integer> queueSize = props.get("station.queueSize");
        //Property<Integer> maxEvents = props.get("lcsim.maxEvents");
        //Property<Boolean> stopOnErrors = props.get("station.stopOnErrors");
        //Property<Boolean> stopOnEndRun = props.get("station.stopOnEndRun");
        Property<Boolean> freeze = props.get("lcsim.freeze");
        Property<String> tag = props.get("lcsim.tag");
        Property<String> builderClass = props.get("lcsim.builder");
        //Property<Integer> printInterval = props.get("station.printInterval");
        Property<String> conditionsUrl = props.get("lcsim.conditions");
        Property<Integer> remoteAidaPort = props.get("lcsim.remoteAidaPort");

        LOG.config("Station properties: " + props.toJSON().toString());

        // Conditions URL
        if (conditionsUrl.valid()) {
            System.setProperty("org.hps.conditions.url", conditionsUrl.value());
            LOG.config("Conditions URL: " + conditionsUrl.value());
        }

        // Remote AIDA port
        if (remoteAidaPort.valid()) {
            System.setProperty("remoteAidaPort", remoteAidaPort.value().toString());
            LOG.config("Remote AIDA port: " + remoteAidaPort.value());
        }

        // Composite loop configuration.
        CompositeLoopConfiguration loopConfig = new CompositeLoopConfiguration();

        // Setup the condition system.
        DatabaseConditionsManager conditionsManager = DatabaseConditionsManager.getInstance();
        DatabaseConditionsManagerSetup conditionsSetup = new DatabaseConditionsManagerSetup();
        boolean activateConditions = true;
        if (run.value() != null) {
            conditionsSetup.setDetectorName(detector.value());
            conditionsSetup.setRun(run.value());
            conditionsSetup.setFreeze(freeze.value());
            if (tag.valid()) {
                Set<String> tags = new HashSet<String>();
                tags.add(tag.value());
                conditionsSetup.setTags(tags);
            }
            LOG.config("Conditions will be initialized: detector=" + detector.value()
                    + ", run=" + run.value() + ", freeze=" + freeze.value() + ", tag=" + tag.value());
        } else {
            // No run number in configuration so read from EVIO data.
            EvioDetectorConditionsProcessor evioConditions =
                    new EvioDetectorConditionsProcessor(detector.value());
            loopConfig.add(evioConditions);
            activateConditions = false;
            LOG.config("Conditions will be initialized from EVIO data.");
        }

        // Setup event builder and register with conditions system.
        this.builder = null;
        try {
            builder = LCSimEventBuilder.class.cast(Class.forName(builderClass.value()).getConstructor().newInstance());
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to create event builder: " + builderClass.value(), e);
        }
        conditionsManager.addConditionsListener(builder);
        loopConfig.setLCSimEventBuilder(builder);

        // Setup the lcsim job manager.
        this.mgr = new JobManager();
        mgr.setDryRun(true);
        final String outputFilePath = outputDir.value() + File.separator + outputName.value();
        LOG.config("Output file path: " + outputFilePath);
        mgr.addVariableDefinition("outputFile", outputFilePath);
        mgr.setConditionsSetup(conditionsSetup); // FIXME: Is this even needed since not calling the run() method?
        if (steering.value().startsWith("file://")) {
            String steeringPath = steering.value().replace("file://", "");
            LOG.config("Setting up steering file: " + steeringPath);
            mgr.setup(new File(steeringPath));
        } else {
            LOG.config("Setting up steering resource: " + steering.value());
            mgr.setup(steering.value());
        }
        LOG.config("Done setting up steering!");

        // Add drivers from the job manager to the loop.
        for (Driver driver : mgr.getDriverExecList()) {
            LOG.config("Adding driver " + driver.getClass().getCanonicalName());
            loopConfig.add(driver);
        }

        // Activate the conditions system, if possible.
        if (activateConditions) {
            LOG.config("Initializing conditions system...");
            conditionsSetup.configure();
            try {
                conditionsSetup.setup();
            } catch (ConditionsNotFoundException e) {
                throw new RuntimeException(e);
            }
            conditionsSetup.postInitialize();
            LOG.config("Conditions system initialized successfully!");
        }

        // Try to connect to the ET system, retrying up to the configured number of max attempts.
        LOG.config("Connecting to ET system...");
        try {
            this.conn = new EtParallelStation(props);
            LOG.config("Successfully connected to ET system!");
        } catch (Exception e) {
            LOG.severe("Failed to create ET station!");
            throw new RuntimeException(e);
        }

        // Cleanly shutdown the ET station on exit.
        final EtConnection shutdownConn = conn;
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                if (shutdownConn != null && shutdownConn.getEtStation() != null) {
                    LOG.info("Cleaning up ET station: " + shutdownConn.getEtStation().getName());
                    shutdownConn.cleanup();
                }
            }
        });
    }

    /**
     * Run the online reconstruction station.
     */
    void run() {

        LOG.info("Started event processing: " + new Date().toString());

        OnlineEventBus eventbus = new OnlineEventBus(this);
        eventbus.startProcessing();
        try {
            eventbus.getEventProcessingThread().join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // TODO: event bus needs to stop on fatal errors, end of run, max events, etc.

        // Configure more settings on the loop.
        /*
        loopConfig.setDataSourceType(DataSourceType.ET_SERVER);
        loopConfig.setEtConnection(conn);
        loopConfig.setMaxQueueSize(queueSize.value());
        loopConfig.setTimeout(-1L);
        loopConfig.setStopOnEndRun(stopOnEndRun.value());
        loopConfig.setStopOnErrors(stopOnErrors.value());

        // Create the record loop.
        CompositeLoop loop = new CompositeLoop(loopConfig);

        // Enable detailed event printing on the composite loop
        LOGGER.config("Event print interval: " + printInterval.value());
        if (printInterval.valid()) {
            CompositeEventPrintLoopAdapter eventPrinter = new CompositeEventPrintLoopAdapter();
            eventPrinter.setPrintInterval(printInterval.value());
            eventPrinter.setPrintEt(true);
            eventPrinter.setPrintEvio(true);
            eventPrinter.setPrintLcio(true);
            loop.addRecordListener(eventPrinter);
        }

        // Run the event loop.
        LOGGER.info("Starting record loop for station: " + stationName.value());
        try {
            loop.loop(maxEvents.value());
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Event processing error", e);
            e.printStackTrace();
        }
        */
        LOG.info("Ended event processing: " + new Date().toString());
    }
}
