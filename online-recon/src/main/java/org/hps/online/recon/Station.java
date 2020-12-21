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
import org.hps.online.recon.properties.Property;
import org.hps.online.recon.properties.PropertyStore.PropertyValidationException;
import org.hps.record.LCSimEventBuilder;
import org.hps.record.composite.CompositeEventPrintLoopAdapter;
import org.hps.record.composite.CompositeLoop;
import org.hps.record.composite.CompositeLoopConfiguration;
import org.hps.record.enums.DataSourceType;
import org.hps.record.et.EtConnection;
import org.hps.record.evio.EvioDetectorConditionsProcessor;
import org.lcsim.conditions.ConditionsManager.ConditionsNotFoundException;
import org.lcsim.util.Driver;

/**
 * Online reconstruction station which processes events from the ET system
 * and writes intermediate plot files.
 */
// TODO: Support usage of an lcsim XML steering file, not just a resource
public class Station {

    /**
     * Class logger
     */
    private static Logger LOGGER = Logger.getLogger(Station.class.getPackage().getName());

    /**
     * The station properties
     */
    private StationProperties props = new StationProperties();

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
        stat.run();
    }

    /**
     * Run the online reconstruction station.
     */
    void run() {

        // Print start messages.
        LOGGER.info("Started: " + new Date().toString());

        Property<String> stationName = props.get("et.stationName");
        LOGGER.config("Initializing station: " + stationName.value());

        LOGGER.config("Validating station properties...");
        try {
            props.validate();
        } catch (PropertyValidationException e) {
            LOGGER.severe("Properties failed to validate");
            throw new RuntimeException(e);
        }
        LOGGER.config("Station properties validated!");

        Property<String> detector = props.get("lcsim.detector");
        Property<Integer> run = props.get("lcsim.run");
        Property<String> outputDir = props.get("station.outputDir");
        Property<String> outputName = props.get("station.outputName");
        Property<String> steering = props.get("lcsim.steering");
        Property<Integer> queueSize = props.get("station.queueSize");
        Property<Integer> interval = props.get("lcsim.printInterval");
        Property<Integer> maxEvents = props.get("lcsim.maxEvents");
        Property<Boolean> stopOnErrors = props.get("station.stopOnErrors");
        Property<Boolean> stopOnEndRun = props.get("station.stopOnEndRun");
        Property<Boolean> freeze = props.get("lcsim.freeze");
        Property<String> tag = props.get("lcsim.tag");
        Property<String> builderClass = props.get("lcsim.builder");
        Property<Boolean> printEvents = props.get("station.printEvents");

        LOGGER.config("Station properties: " + props.toJSON().toString());

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
            LOGGER.config("Conditions will be initialized with: detector=" + detector.value()
                + ", run=" + ", freeze=" + freeze.value() + ", tag=" + tag.value());
        } else {
            // No run number in configuration so read from EVIO data.
            EvioDetectorConditionsProcessor evioConditions =
                    new EvioDetectorConditionsProcessor(detector.value());
            loopConfig.add(evioConditions);
            activateConditions = false;
            LOGGER.config("No run number provided so conditions will be initialized from the EVIO data.");
        }

        // Setup event builder and register with conditions system.
        LCSimEventBuilder builder = null;
        try {
            builder = LCSimEventBuilder.class.cast(Class.forName(builderClass.value()).getConstructor().newInstance());
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to create event builder: " + builderClass.value(), e);
        }
        conditionsManager.addConditionsListener(builder);
        loopConfig.setLCSimEventBuilder(builder);

        // Setup the lcsim job manager.
        JobManager mgr = new JobManager();
        mgr.setDryRun(true);
        if (interval.valid()) {
            mgr.setEventPrintInterval(interval.value());
            LOGGER.config("lcsim event print interval: " + interval.value());
        } else {
            LOGGER.config("lcsim event printing disabled");
        }
        final String outputFilePath = outputDir.value() + File.separator + outputName.value();
        LOGGER.config("Output file path: " + outputFilePath);
        mgr.addVariableDefinition("outputFile", outputFilePath);
        mgr.setConditionsSetup(conditionsSetup); // FIXME: Is this even needed since not calling the run() method?
        LOGGER.config("Setting up steering resource: " + steering.value());
        mgr.setup(steering.value());

        // Add drivers from the job manager to the loop.
        for (Driver driver : mgr.getDriverExecList()) {
            LOGGER.config("Adding driver " + driver.getClass().getCanonicalName());
            loopConfig.add(driver);
        }

        // Activate the conditions system, if possible.
        if (activateConditions) {
            LOGGER.config("Activating conditions system");
            conditionsSetup.configure();
            try {
                conditionsSetup.setup();
            } catch (ConditionsNotFoundException e) {
                throw new RuntimeException(e);
            }
            conditionsSetup.postInitialize();
        }

        // Try to connect to the ET system, retrying up to the configured number of max attempts.
        LOGGER.config("Connecting to ET system...");
        EtConnection conn = null;
        try {
            conn = new EtParallelStation(props);
            LOGGER.config("Successfully connected to ET system!");
        } catch (Exception e) {
            LOGGER.severe("Failed to create ET station!");
            throw new RuntimeException(e);
        }

        // Cleanly shutdown the ET station on exit.
        final EtConnection shutdownConn = conn;
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                if (shutdownConn != null && shutdownConn.getEtStation() != null) {
                    LOGGER.info("Cleaning up ET station: " + shutdownConn.getEtStation().getName());
                    shutdownConn.cleanup();
                }
            }
        });

        // Configure more settings on the loop.
        loopConfig.setDataSourceType(DataSourceType.ET_SERVER);
        loopConfig.setEtConnection(conn);
        loopConfig.setMaxQueueSize(queueSize.value());
        loopConfig.setTimeout(-1L);
        loopConfig.setStopOnEndRun(stopOnEndRun.value());
        loopConfig.setStopOnErrors(stopOnErrors.value());

        // Create the record loop.
        CompositeLoop loop = new CompositeLoop(loopConfig);

        // Enable detailed event printing on the composite loop
        LOGGER.config("Station event printing: " + printEvents.value());
        if (printEvents.value()) {
            CompositeEventPrintLoopAdapter eventPrinter = new CompositeEventPrintLoopAdapter();
            eventPrinter.setPrintInterval(1L);
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
        LOGGER.info("Ended: " + new Date().toString());
    }
}
