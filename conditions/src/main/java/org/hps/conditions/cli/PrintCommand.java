package org.hps.conditions.cli;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.hps.conditions.api.ConditionsObject;
import org.hps.conditions.api.ConditionsObjectCollection;
import org.hps.conditions.api.ConditionsRecord.ConditionsRecordCollection;
import org.hps.conditions.api.TableMetaData;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.lcsim.util.log.LogUtil;

/**
 * This sub-command of the conditions CLI prints conditions conditions table data by run number to the console or
 * optionally writes it to an output file.
 *
 * @author Jeremy McCormick, SLAC
 */
final class PrintCommand extends AbstractCommand {

    /**
     * Setup logger.
     */
    private static final Logger LOGGER = LogUtil.create(PrintCommand.class);

    /**
     * Defines command options.
     */
    static Options options = new Options();

    static {
        options.addOption(new Option("h", false, "print help for print command"));
        options.addOption(new Option("t", true, "table name"));
        options.addOption(new Option("i", false, "print the ID for the records (off by default)"));
        options.addOption(new Option("f", true, "write print output to a file (must be used with -t option)"));
        options.addOption(new Option("H", false, "suppress printing of conditions record and table info"));
        options.addOption(new Option("d", false, "use tabs for field delimiter instead of spaces"));
    }

    /**
     * The field delimiter for print output.
     */
    private char fieldDelimiter = ' ';

    /**
     * Output file if printing to a file.
     */
    private File outputFile;

    /**
     * Flag to print out column headers.
     */
    private boolean printHeaders = true;

    /**
     * Flag to print row IDs.
     */
    private boolean printIDs = false;

    /**
     * This is the <code>PrintStream</code> for printing the collections to the console or a file.
     */
    private PrintStream ps = System.out;

    /**
     * Class constructor.
     */
    PrintCommand() {
        super("print", "Print the table data for a conditions set", options);
    }

    /**
     * Print out the conditions sets selected by the user's command line arguments.
     *
     * @param arguments the command line arguments
     */
    @Override
    final void execute(final String[] arguments) {

        final CommandLine commandLine = this.parse(arguments);

        final DatabaseConditionsManager conditionsManager = DatabaseConditionsManager.getInstance();

        if (!conditionsManager.isInitialized()) {
            throw new RuntimeException("conditions system is not initialized");
        }

        // Print conditions sets matching a specific conditions key.
        String userConditionsKey = null;
        if (commandLine.hasOption("t")) {
            userConditionsKey = commandLine.getOptionValue("t");
        }

        // Setup an output file for the print out if requested.
        if (commandLine.hasOption("f")) {
            if (!commandLine.hasOption("t")) {
                throw new IllegalArgumentException("An output file may only be specified when using the -t option.");
            }
            final String path = commandLine.getOptionValue("f");
            if (new File(path).exists()) {
                throw new IllegalArgumentException("File already exists: " + path);
            }
            this.outputFile = new File(path);
            try {
                this.ps = new PrintStream(new BufferedOutputStream(new FileOutputStream(this.outputFile, false)));
            } catch (final FileNotFoundException e) {
                throw new IllegalArgumentException(e);
            }
        }

        // Print IDs in the output.
        if (commandLine.hasOption("i")) {
            this.printIDs = true;
        }

        // Print header info. Option turns this off.
        if (commandLine.hasOption("h")) {
            this.printHeaders = false;
        }

        // Use tabs instead of spaces for field delimiter.
        if (commandLine.hasOption("d")) {
            this.fieldDelimiter = '\t';
        }

        // List of conditions records to print.
        final ConditionsRecordCollection conditionsRecords = new ConditionsRecordCollection();

        // Did the user specify a table to use?
        if (userConditionsKey == null) {
            LOGGER.info("printing all conditions");
            // Use all table names if there was not one specified.
            conditionsRecords.addAll(conditionsManager.getConditionsRecords());
        } else {
            LOGGER.info("printing conditions with name: " + userConditionsKey);
            // Get records only for the user specified table name.
            conditionsRecords.addAll(conditionsManager.findConditionsRecords(userConditionsKey));
        }

        // Sort the records by key (table name).
        conditionsRecords.sortByKey();

        // Get a unique list of keys from the returned conditions records.
        final Set<String> conditionsKeys = conditionsRecords.getConditionsKeys();

        // Print the records and the data.
        this.printConditionsRecords(conditionsKeys);
        this.ps.flush();
        this.ps.close();

        if (this.outputFile != null) {
            LOGGER.info("wrote collection data to file " + this.outputFile.getPath());
        }
    }

    /**
     * Print a single collection.
     *
     * @param collection the collection to print
     */
    private void printCollection(final ConditionsObjectCollection<?> collection) {
        final StringBuffer buffer = new StringBuffer();
        for (final Object object : collection) {
            for (final String columnName : collection.getTableMetaData().getFieldNames()) {
                if (!"collection_id".equals(columnName)) {
                    buffer.append(((ConditionsObject) object).getFieldValue(columnName));
                    buffer.append(this.fieldDelimiter);
                }
            }
            buffer.setLength(buffer.length() - 1);
            buffer.append('\n');
        }
        buffer.setLength(buffer.length() - 1);
        this.ps.print(buffer.toString());
        this.ps.flush();
    }

    /**
     * Print the header for a collection. This is printed to the log rather than the <code>PrintStream</code>.
     *
     * @param collection the collection
     */
    private void printCollectionHeader(final ConditionsObjectCollection<?> collection) {
        LOGGER.info('\n' + "--------------------------------------" + '\n' + "table: "
                + collection.getTableMetaData().getTableName() + '\n' + "collection ID: "
                + collection.getCollectionId() + '\n' + "--------------------------------------");
    }

    /**
     * Print the list of collections.
     *
     * @param collectionList the list of collections
     */
    private void printCollections(final List<ConditionsObjectCollection<?>> collectionList) {
        // Loop over all the collections and print them.
        for (final ConditionsObjectCollection<?> collection : collectionList) {
            if (this.printHeaders) {
                this.printCollectionHeader(collection);
            }
            this.printColumnNames(collection.getTableMetaData());
            this.printCollection(collection);
            this.ps.println();
        }
        this.ps.flush();
    }

    /**
     * Print the column names for a table.
     *
     * @param tableMetaData the table meta data
     */
    private void printColumnNames(final TableMetaData tableMetaData) {
        if (this.printIDs) {
            this.ps.print("id");
            this.ps.print(this.fieldDelimiter);
        }
        for (final String columnName : tableMetaData.getFieldNames()) {
            if (!"collection_id".equals(columnName)) {
                this.ps.print(columnName);
                this.ps.print(this.fieldDelimiter);
            }
        }
        this.ps.println();
    }

    /**
     * Print out the conditions records either to the console or a file (if that option is enabled).
     *
     * @param conditionsKeys the list of conditions keys (usually same as table names)
     */
    private void printConditionsRecords(final Set<String> conditionsKeys) {

        final DatabaseConditionsManager conditionsManager = DatabaseConditionsManager.getInstance();

        final StringBuffer sb = new StringBuffer();
        for (final String conditionsKey : conditionsKeys) {
            sb.append(conditionsKey + " ");
        }
        LOGGER.info("printing conditions sets: " + sb.toString());

        // Loop over the conditions keys from the conditions records.
        for (final String conditionsKey : conditionsKeys) {

            // The list of collections to print.
            final List<ConditionsObjectCollection<?>> collectionList = new ArrayList<ConditionsObjectCollection<?>>();

            // Get the table meta data for the conditions key.
            final TableMetaData tableMetaData = conditionsManager.findTableMetaData(conditionsKey);

            // This shouldn't ever happen but check anyways.
            if (tableMetaData == null) {
                throw new RuntimeException("The table meta data for " + conditionsKey
                        + " does not exist.  The key might be invalid.");
            }

            // Use only the single collection which would be seen by a user job for this run number and key.
            final ConditionsObjectCollection<?> collection = conditionsManager.getCachedConditions(
                    tableMetaData.getCollectionClass(), tableMetaData.getTableName()).getCachedData();

            collectionList.add(collection);

            // Print out all the collection data to console or file.
            this.printCollections(collectionList);
        }
    }
}
