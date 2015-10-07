package org.hps.conditions.cli;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.hps.conditions.api.ConditionsObjectCollection;
import org.hps.conditions.api.ConditionsRecord;
import org.hps.conditions.api.ConditionsRecord.ConditionsRecordCollection;
import org.hps.conditions.api.TableMetaData;
import org.hps.conditions.database.DatabaseConditionsManager;

/**
 * This is a sub-command to print out collection meta data for the current conditions configuration of tag, detector
 * model and run number, which are given as arguments to the conditions command line front-end. It does not print out
 * any conditions objects, only the collection information. By default it will print information about the single
 * collection found for a given type, which is by convention the last one updated. The <code>-a</code> option can be
 * used to print out all collection information.
 *
 * @author Jeremy McCormick, SLAC
 */
final class RunSummaryCommand extends AbstractCommand {

    /**
     * Initialize the logger.
     */
    private static final Logger LOGGER = Logger.getLogger(RunSummaryCommand.class.getPackage().getName());

    /**
     * Define command options.
     */
    static Options options = new Options();
    static {
        options.addOption(new Option("h", false, "Show help for run-summary command"));
        options.addOption(new Option("a", false, "Print all collections found for the run"));
    }

    /**
     * Class constructor.
     */
    RunSummaryCommand() {
        super("run-summary", "Print the run summary", options);
    }

    /**
     * Print out the run summary information.
     *
     * @param arguments the command line arguments
     */
    @Override
    final void execute(final String[] arguments) {

        final CommandLine commandLine = this.parse(arguments);

        final DatabaseConditionsManager conditionsManager = DatabaseConditionsManager.getInstance();

        if (!conditionsManager.isInitialized()) {
            throw new RuntimeException("The conditions system is not initialized.");
        }

        final int run = conditionsManager.getRun();

        final boolean printAll = commandLine.hasOption("a");
        if (printAll) {
            LOGGER.info("All collections will be printed.");
        }

        // Get all the conditions records from the manager including those that overlap in time validity.
        final ConditionsRecordCollection conditionsRecords = conditionsManager.getConditionsRecords();
        LOGGER.info('\n' + "Run " + run + " has " + conditionsRecords.size() + " conditions records.");

        // Get the list of unique conditions keys and sort them.
        final List<String> conditionsKeys = new ArrayList<String>(conditionsRecords.getConditionsKeys());
        Collections.sort(conditionsKeys);
        LOGGER.info('\n' + "Found these unique conditions keys for the run ...");
        for (final String key : conditionsKeys) {
            LOGGER.info(key);
        }
        LOGGER.info("");

        // Loop over all the conditions keys that apply to this run.
        for (final String key : conditionsKeys) {

            // Get the table meta data for the key.
            final TableMetaData tableMetaData = conditionsManager.findTableMetaData(key);

            // Get all the conditions records that match this key.
            final ConditionsRecordCollection collectionRecords = conditionsRecords.findByKey(key);

            // Get the table name.
            final String tableName = tableMetaData.getTableName();

            if (!printAll) {
                // Print out the single collection that will be used if retrieved through the converter.
                final ConditionsObjectCollection<?> collection = conditionsManager.getCachedConditions(
                        tableMetaData.getCollectionClass(), key).getCachedData();
                LOGGER.info(tableMetaData.getObjectClass().getSimpleName() + " collection "
                        + collection.getCollectionId() + " in " + tableName + " with " + collection.size() + " rows.");
            } else {
                // Print out information about all applicable collections for this key, without figuring out which would
                // be used.
                LOGGER.info(tableMetaData.getObjectClass().getSimpleName() + " has " + collectionRecords.size()
                        + " collection(s) in " + tableName + " for run.");
                for (final ConditionsRecord record : collectionRecords) {
                    LOGGER.info("  collection " + record.getCollectionId().toString() + " created on "
                            + record.getCreated().toString());
                }
            }
        }
        LOGGER.info('\n' + "Done printing run summary!");
    }
}
