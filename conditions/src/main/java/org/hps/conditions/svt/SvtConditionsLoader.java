package org.hps.conditions.svt;

import java.io.File;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.DefaultParser;
import org.hps.conditions.api.ConditionsRecord;
import org.hps.conditions.api.TableMetaData;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.svt.SvtCalibration.SvtCalibrationCollection;
import org.hps.conditions.svt.SvtChannel.SvtChannelCollection;
import org.hps.conditions.svt.SvtDaqMapping.SvtDaqMappingCollection;
import org.lcsim.conditions.ConditionsManager.ConditionsNotFoundException;

/**
 * Command line tool used to load SVT conditions into the conditions database.
 *
 * @author Omar Moreno, UCSC
 */
public final class SvtConditionsLoader {

    /**
     * Calibrations table name.
     */
    public static final String CALIBRATIONS_TABLE_NAME = "svt_calibrations";

    /**
     * SVT DAQ map table name.
     */
    public static final String DAQ_MAP_TABLE_NAME = "svt_daq_map";

    /**
     * Default detector name.
     */
    public static final String DETECTOR = "HPS-Proposal2014-v9-2pt2";

    /**
     * Initialize the logger.
     */
    private static Logger LOGGER = Logger.getLogger(SvtConditionsLoader.class.getPackage().getName());

    /**
     * SVT channels table name.
     */
    public static final String SVT_CHANNELS_TABLE_NAME = "svt_channels";

    /**
     * Run this class from the command line.
     *
     * @param args The command line arguments.
     */
    public static void main(final String[] args) {

        // Set up the command line options
        final Options options = setupCommandLineOptions();

        // Parse the command line arguments
        final CommandLineParser parser = new DefaultParser();
        final CommandLine commandLine;
        try {
            commandLine = parser.parse(options, args);
        } catch (final ParseException e) {
            throw new RuntimeException("Unable to parse command line arguments.", e);
        }

        // Get the run number. If a run number hasn't been set, warn the user and exit.
        if (!commandLine.hasOption("r")) {
            System.out.println("\nPlease specify a run number to associate with the conditions set.\n");
            return;
        }
        final int runNumber = Integer.valueOf(commandLine.getOptionValue("r"));
        LOGGER.info("Run number set to " + runNumber);

        // Initialize the conditions system and load the conditions onto the
        // detector object
        try {

            // If a user has specified the connection properties, set them, otherwise use the default values.
            if (commandLine.hasOption("p")) {
                DatabaseConditionsManager.getInstance().setConnectionProperties(
                        new File(commandLine.getOptionValue("p")));
            }
            DatabaseConditionsManager.getInstance().setDetector(SvtConditionsLoader.DETECTOR, runNumber);
        } catch (final ConditionsNotFoundException e) {
            throw new RuntimeException("Could not initialize the conditions system.", e);
        }

        // Instantiate the SVT conditions reader
        final SvtConditionsReader reader;
        try {
            reader = new SvtConditionsReader();
        } catch (final Exception e) {
            throw new RuntimeException("Couldn't open SvtConditionsReader.", e);
        }

        // If a calibrations file has been specified, parse it and load them
        // to the conditions database.
        if (commandLine.hasOption("c")) {
            final File calibrationFile = new File(commandLine.getOptionValue("c"));
            LOGGER.info("Loading calibrations from file " + calibrationFile.getAbsolutePath());
            try {

                // Parse the calibration file and retrieve the calibrations collection.
                reader.parseCalibrations(calibrationFile);
                final SvtCalibrationCollection calibrations = reader.getSvtCalibrationCollection();

                // Set the table meta data
                final TableMetaData tableMetaData = DatabaseConditionsManager.getInstance().findTableMetaData(
                        SvtConditionsLoader.CALIBRATIONS_TABLE_NAME);
                calibrations.setTableMetaData(tableMetaData);

                // Set the collection ID.
                final int collectionID = DatabaseConditionsManager.getInstance().getCollectionId(calibrations, null);
                calibrations.setCollectionId(collectionID);
                LOGGER.info("Using collection ID " + collectionID);

                // Load the calibrations
                calibrations.insert();
                LOGGER.info("A total of " + calibrations.size()
                        + " SvtCalibrations were loaded successfully into the database.");

                // Create a conditions record associated with the set of conditions that were just loaded.
                final ConditionsRecord conditionsRecord = new ConditionsRecord(calibrations.getCollectionId(),
                        runNumber, 99999, SvtConditionsLoader.CALIBRATIONS_TABLE_NAME,
                        SvtConditionsLoader.CALIBRATIONS_TABLE_NAME,
                        "Pedestals and noise. Loaded using SvtConditionsLoader.", "eng_run");
                conditionsRecord.insert();

            } catch (final Exception e) {
                throw new RuntimeException("Couldn't parse calibration file.", e);
            }
        }

        // If a DAQ map file has been specified, parse it and load them to the
        // conditions database.
        if (commandLine.hasOption("d")) {
            final File daqMapFile = new File(commandLine.getOptionValue("d"));
            LOGGER.info("Loading DAQ map from file " + daqMapFile.getAbsolutePath());
            try {

                // Parse the DAQ map file
                reader.parseDaqMap(daqMapFile);
                final SvtDaqMappingCollection daqMapping = reader.getDaqMapCollection();

                // Set the table meta data
                TableMetaData tableMetaData = DatabaseConditionsManager.getInstance().findTableMetaData(
                        SvtConditionsLoader.DAQ_MAP_TABLE_NAME);
                daqMapping.setTableMetaData(tableMetaData);

                // Set the collection ID
                int collectionID = DatabaseConditionsManager.getInstance().getCollectionId(daqMapping, null);
                daqMapping.setCollectionId(collectionID);
                LOGGER.info("Using collection ID " + collectionID);

                // Load the DAQ map
                daqMapping.insert();
                LOGGER.info("DAQ map has been loaded successfully");
                LOGGER.fine(daqMapping.toString());

                // Create a conditions record associated with the set of
                // conditions that were just loaded.
                ConditionsRecord conditionsRecord = new ConditionsRecord(daqMapping.getCollectionId(), runNumber,
                        99999, SvtConditionsLoader.DAQ_MAP_TABLE_NAME, SvtConditionsLoader.DAQ_MAP_TABLE_NAME,
                        "Engineering run DAQ map. Loaded using SvtConditionsLoader.", "eng_run");
                conditionsRecord.insert();

                LOGGER.info("Loading the collection of SvtChannel's");
                final SvtChannelCollection svtChannels = reader.getSvtChannelCollection();

                // Set the table meta data
                tableMetaData = DatabaseConditionsManager.getInstance().findTableMetaData(
                        SvtConditionsLoader.SVT_CHANNELS_TABLE_NAME);
                svtChannels.setTableMetaData(tableMetaData);

                // Set the collection ID
                collectionID = DatabaseConditionsManager.getInstance().getCollectionId(svtChannels, null);
                svtChannels.setCollectionId(collectionID);
                LOGGER.info("Using collection ID " + collectionID);

                svtChannels.insert();
                LOGGER.info("A total of " + svtChannels.size()
                        + " SvtChannels were successfully loaded into the database.");

                // Create a conditions record associated with the set of
                // conditions that were just loaded.
                conditionsRecord = new ConditionsRecord(svtChannels.getCollectionId(), runNumber, 99999,
                        SvtConditionsLoader.SVT_CHANNELS_TABLE_NAME, SvtConditionsLoader.SVT_CHANNELS_TABLE_NAME,
                        "Engineering run SVT channel IDs. Loaded using SvtConditionsLoader.", "eng_run");
                conditionsRecord.insert();
            } catch (final Exception e) {
                throw new RuntimeException("Couldn't parse DAQ map file.", e);
            }
        }
    }

    /**
     * Method used to setup all command line options.
     *
     * @return a set of options
     */
    private static Options setupCommandLineOptions() {
        final Options options = new Options();
        options.addOption(new Option("r", true, "Run number"));
        options.addOption(new Option("p", true, "Path to properties file"));
        options.addOption(new Option("c", true, "Calibration file"));
        options.addOption(new Option("d", true, "DAQ map file"));
        return options;
    }

    /**
     * Do not allow instantiation.
     */
    private SvtConditionsLoader() {
        throw new UnsupportedOperationException("Do not instantiate this class.");
    }
}
