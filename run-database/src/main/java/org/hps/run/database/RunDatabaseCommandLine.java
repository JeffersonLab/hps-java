package org.hps.run.database;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.hps.conditions.database.ConnectionParameters;
import org.hps.datacat.client.DatacatClient;
import org.hps.datacat.client.DatacatClientFactory;
import org.hps.datacat.client.Dataset;
import org.hps.datacat.client.DatasetMetadata;
import org.hps.record.evio.EvioFileUtilities;
import org.lcsim.util.log.DefaultLogFormatter;
import org.lcsim.util.log.LogUtil;

/**
 * Command line tool for updating the run database from EVIO files registered in the data catalog.
 *
 * @author Jeremy McCormick, SLAC
 */
public class RunDatabaseCommandLine {

    /**
     * Set of features supported by the tool.
     */
    static enum Feature {
        /**
         * Insert EPICS data.
         */
        EPICS,
        /**
         * Insert scaler data.
         */
        SCALERS,
        /**
         * Insert run summary.
         */
        SUMMARY,
        /**
         * Insert trigger config.
         */
        TRIGGER_CONFIG
    }

    /**
     * Setup the logger.
     */
    private static final Logger LOGGER = LogUtil.create(RunDatabaseCommandLine.class, new DefaultLogFormatter(),
            Level.ALL);

    /**
     * Command line options for the crawler.
     */
    private static final Options OPTIONS = new Options();

    /**
     * Statically define the command options.
     */
    static {
        OPTIONS.addOption("f", "feature", true, "enable a feature");
        OPTIONS.addOption("p", "connection-properties", true, "database connection properties file (required)");
        OPTIONS.addOption("h", "help", false, "print help and exit (overrides all other arguments)");
        OPTIONS.addOption("r", "run", true, "run to update");
        OPTIONS.addOption("u", "update", false, "allow updating existing run in the database");
    }

    /**
     * Run the program from the command line.
     *
     * @param args the command line arguments
     */
    public static void main(final String args[]) {
        new RunDatabaseCommandLine().parse(args).run();
    }

    /**
     * Allow updating of the database for existing runs.
     */
    private boolean allowUpdates = false;

    /**
     * The set of enabled features.
     */
    private final Set<Feature> features = new HashSet<Feature>();

    /**
     * The run manager for interacting with the run db.
     */
    private RunManager runManager;

    /**
     * Create a run processor from the current configuration.
     *
     * @return the run processor
     */
    private RunProcessor createEvioRunProcessor(final RunSummaryImpl runSummary, final List<File> files) {

        final RunProcessor runProcessor = new RunProcessor(runSummary, files);

        if (features.contains(Feature.EPICS)) {
            runProcessor.addEpicsProcessor();
        }
        if (features.contains(Feature.SCALERS)) {
            runProcessor.addScalerProcessor();
        }
        if (features.contains(Feature.TRIGGER_CONFIG)) {
            runProcessor.addTriggerTimeProcessor();
        }

        return runProcessor;
    }

    /**
     * Get the list of EVIO files for the run.
     *
     * @param run the run number
     * @return the list of EVIO files from the run
     */
    private Map<File, Dataset> getEvioFiles(final int run) {
        final DatacatClient datacatClient = new DatacatClientFactory().createClient();
        final Set<String> metadata = new HashSet<String>();
        final Map<File, Dataset> files = new HashMap<File, Dataset>();
        metadata.add("runMin");
        metadata.add("eventCount");
        metadata.add("fileNumber");
        metadata.add("endTimestamp");
        metadata.add("startTimestamp");
        metadata.add("hasEnd");
        metadata.add("hasPrestart");
        final List<Dataset> datasets = datacatClient.findDatasets("data/raw",
                "fileFormat eq 'EVIO' AND dataType eq 'RAW' AND runMin eq " + run, metadata);
        if (datasets.isEmpty()) {
            throw new IllegalStateException("No EVIO datasets for run " + run + " were found in the data catalog.");
        }
        for (final Dataset dataset : datasets) {
            files.put(new File(dataset.getLocations().get(0).getResource()), dataset);
        }
        return files;
    }

    /**
     * Insert information for a run into the database.
     *
     * @param runManager the run manager for interacting with the run db
     * @param runSummary the run summary with information about the run
     */
    private void insertRun(final RunManager runManager, final RunSummary runSummary) {

        final RunDatabaseDaoFactory runFactory = new RunDatabaseDaoFactory(runManager.getConnection());

        // Add the run summary record.
        if (this.features.contains(Feature.SUMMARY)) {
            LOGGER.info("inserting run summary");
            runFactory.createRunSummaryDao().insertRunSummary(runSummary);
        }

        if (this.features.contains(Feature.EPICS)) {
            LOGGER.info("inserting EPICS data");
            runFactory.createEpicsDataDao().insertEpicsData(runSummary.getEpicsData());
        }

        if (this.features.contains(Feature.SCALERS)) {
            LOGGER.info("inserting scaler data");
            runFactory.createScalerDataDao().insertScalerData(runSummary.getScalerData(), runManager.getRun());
        }

        if (this.features.contains(Feature.TRIGGER_CONFIG)) {
            LOGGER.info("inserting trigger config");
            runFactory.createTriggerConfigDao().insertTriggerConfig(runSummary.getTriggerConfig(), runManager.getRun());
        }
    }

    /**
     * Parse command line options and return reference to <code>this</code>.
     *
     * @param args the command line arguments
     * @return reference to this object
     */
    RunDatabaseCommandLine parse(final String args[]) {
        try {
            final CommandLine cl = new PosixParser().parse(OPTIONS, args);

            // Print help and exit.
            if (cl.hasOption("h") || args.length == 0) {
                final HelpFormatter help = new HelpFormatter();
                help.printHelp("RunDatabaseCommandLine [options]", "", OPTIONS, "");
                System.exit(0);
            }

            // Database connection properties file.
            if (cl.hasOption("p")) {
                final String dbPropPath = cl.getOptionValue("p");
                final File dbPropFile = new File(dbPropPath);
                if (!dbPropFile.exists()) {
                    throw new IllegalArgumentException("Connection properties file " + dbPropFile.getPath()
                            + " does not exist.");
                }
                final ConnectionParameters connectionParameters = ConnectionParameters.fromProperties(dbPropFile);
                LOGGER.config("using " + dbPropPath + " for db connection properties");

                runManager = new RunManager(connectionParameters.createConnection());

            } else {
                // Database connection properties file is required.
                throw new RuntimeException("Connection properties are required.");
            }

            Integer run = null;
            if (cl.hasOption("r")) {
                run = Integer.parseInt(cl.getOptionValue("r"));
            } else {
                throw new RuntimeException("The run number is required.");
            }
            runManager.setRun(run);

            if (cl.hasOption("f")) {
                // Enable individual features.
                for (final String arg : cl.getOptionValues("f")) {
                    features.add(Feature.valueOf(arg));
                }
            } else {
                // By default all features are enabled.
                features.addAll(Arrays.asList(Feature.values()));
            }
            for (final Feature feature : features) {
                LOGGER.config("feature " + feature.name() + " is enabled.");
            }

            // Allow updates to existing runs in the db.
            if (cl.hasOption("u")) {
                this.allowUpdates = true;
                LOGGER.config("updating or replacing existing run data is enabled");
            }

        } catch (final ParseException e) {
            throw new RuntimeException(e);
        }

        return this;
    }

    /**
     * Run the job to update the information in the run database.
     */
    private void run() {

        LOGGER.info("starting");

        final boolean runExists = runManager.runExists();

        // Fail if run exists and updates are not allowed.
        if (runExists && !allowUpdates) {
            throw new IllegalStateException("The run " + runManager.getRun()
                    + " already exists and updates are not allowed.");
        }

        // Get the run number configured from command line.
        final int run = runManager.getRun();

        // Get the list of EVIO files for the run using a data catalog query.
        final Map<File, Dataset> fileDatasets = this.getEvioFiles(run);
        final List<File> files = new ArrayList<File>(fileDatasets.keySet());
        EvioFileUtilities.sortBySequence(files);

        // Process the run's files to get information.
        final RunSummaryImpl runSummary = new RunSummaryImpl(run);
        final RunProcessor runProcessor = this.createEvioRunProcessor(runSummary, files);
        try {
            runProcessor.processRun();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }

        // Set number of files from datacat query.
        runSummary.setTotalFiles(files.size());

        // Set run start date.
        this.setStartDate(fileDatasets, files, runSummary);

        // Set run end date.
        this.setEndDate(fileDatasets, files, runSummary);

        // Delete existing run.
        if (runExists) {
            runManager.deleteRun();
        }

        // Insert run into database.
        this.insertRun(runManager, runSummary);

        // Close the database connection.
        runManager.closeConnection();

        LOGGER.info("done");
    }

    /**
     * Set the run end date.
     *
     * @param fileDatasets the run's datasets
     * @param files the run's EVIO files
     * @param runSummary the run summary
     */
    private void setEndDate(final Map<File, Dataset> fileDatasets, final List<File> files,
            final RunSummaryImpl runSummary) {
        final Dataset lastDataset = fileDatasets.get(files.get(files.size() - 1));
        final DatasetMetadata metadata = lastDataset.getMetadata();
        // System.out.println("endTimestamp: " + metadata.getLong("endTimestamp"));
        final Date endDate = new Date(metadata.getLong("endTimestamp"));
        // System.out.println("endDate: " + startDate);
        runSummary.setEndDate(endDate);
        runSummary.setEndOkay(metadata.getLong("hasEnd") == 0 ? false : true);
    }

    /**
     * Set the run start date.
     *
     * @param fileDatasets the run's datasets
     * @param files the run's EVIO files
     * @param runSummary the run summary
     */
    private void setStartDate(final Map<File, Dataset> fileDatasets, final List<File> files,
            final RunSummaryImpl runSummary) {
        final Dataset firstDataset = fileDatasets.get(files.get(0));
        final DatasetMetadata metadata = firstDataset.getMetadata();
        final Date startDate = new Date(metadata.getLong("startTimestamp"));
        runSummary.setStartDate(startDate);
    }
}
