package org.hps.conditions.cli;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.hps.conditions.api.ConditionsObject;
import org.hps.conditions.api.ConditionsObjectCollection;
import org.hps.conditions.api.ConditionsRecord.ConditionsRecordCollection;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.database.TableMetaData;

/**
 * This sub-command of the conditions CLI prints conditions conditions table data by run number to the console or
 * optionally writes it to an output file.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
class PrintCommand extends AbstractCommand {

    /**
     * Print stream for output.
     */
    private PrintStream ps = System.out;

    /**
     * Flag to print row IDs.
     */
    private boolean printIDs = false;

    /**
     * Flag to print out column headers.
     */
    private boolean printHeaders = true;

    /**
     * The field delimiter for print output.
     */
    private char fieldDelimiter = ' ';

    /**
     * Output file if printing to a file.
     */
    private File outputFile;
    
    /**
     * Defines command options.
     */
    static Options options = new Options();
    static {
        options.addOption(new Option("h", false, "Show help for print command"));
        options.addOption(new Option("t", true, "Set the table name"));
        options.addOption(new Option("i", false, "Print the ID for the records (off by default)"));
        options.addOption(new Option("f", true, "Write print output to a file (must be used with -t option)"));
        options.addOption(new Option("H", false, "Suppress printing of conditions record and table info"));
        options.addOption(new Option("d", false, "Use tabs for field delimiter instead of spaces"));
        options.addOption(new Option("T", true, "Specify a conditions tag to use for filtering records"));
    }

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
    final void execute(final String[] arguments) {

        final CommandLine commandLine = parse(arguments);

        final DatabaseConditionsManager conditionsManager = DatabaseConditionsManager.getInstance();

        if (!conditionsManager.isInitialized()) {
            throw new RuntimeException("conditions system is not initialized");
        }

        // User specified tag of conditions records.
        if (commandLine.hasOption("T")) {
            conditionsManager.setTag(commandLine.getOptionValue("T"));
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
            outputFile = new File(path);
            try {
                ps = new PrintStream(new BufferedOutputStream(new FileOutputStream(outputFile, false)));
            } catch (FileNotFoundException e) {
                throw new IllegalArgumentException(e);
            }
        }

        // Print IDs in the output.
        if (commandLine.hasOption("i")) {
            printIDs = true;
        }

        // Print header info. Option turns this off.
        if (commandLine.hasOption("h")) {
            printHeaders = false;
        }

        // Use tabs instead of spaces for field delimiter.
        if (commandLine.hasOption("d")) {
            fieldDelimiter = '\t';
        }

        // List of conditions records to print.
        final ConditionsRecordCollection conditionsRecords = new ConditionsRecordCollection();

        // Did the user specify a table to use?
        if (userConditionsKey == null) {
            System.out.println("printing all conditions");
            // Use all table names if there was not one specified.
            conditionsRecords.addAll(conditionsManager.getConditionsRecords());
        } else {
            System.out.println("printing conditions with name: " + userConditionsKey);
            // Get records only for the user specified table name.
            conditionsRecords.addAll(conditionsManager.findConditionsRecords(userConditionsKey));
        }

        // Sort the records by key (table name).
        conditionsRecords.sortByKey();

        // Get a unique list of keys from the returned conditions records.
        final Set<String> conditionsKeys = conditionsRecords.getConditionsKeys();

        // Print the records and the data.
        printConditionsRecords(conditionsKeys);
        ps.flush();
        ps.close();

        if (outputFile != null) {
            System.out.println("wrote collection data to file " + outputFile.getPath());
        }
    }

    /**
     * Print out the conditions records either to the console or a file (if that option is enabled).
     *
     * @param conditionsKeys the list of conditions keys (usually same as table names)
     */
    private void printConditionsRecords(final Set<String> conditionsKeys) {

        final DatabaseConditionsManager conditionsManager = DatabaseConditionsManager.getInstance();

        System.out.print("printing conditions sets: ");
        for (String conditionsKey : conditionsKeys) {
            System.out.print(conditionsKey + " ");
        }
        System.out.println();
        // Loop over the conditions keys from the conditions records.
        for (String conditionsKey : conditionsKeys) {

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
            printCollections(collectionList);
        }
    }

    /**
     * Print the list of collections.
     * @param collectionList the list of collections
     */
    private void printCollections(final List<ConditionsObjectCollection<?>> collectionList) {
        // Loop over all the collections and print them.
        for (ConditionsObjectCollection<?> collection : collectionList) {
            if (printHeaders) {
                printCollectionHeader(collection);
            }
            printColumnNames(collection.getTableMetaData());
            printCollection(collection);
            System.out.println();
        }
        ps.flush();
    }

    /**
     * Print a single collection.
     * @param collection the collection to print
     */
    private void printCollection(final ConditionsObjectCollection<?> collection) {
        final StringBuffer buffer = new StringBuffer();
        for (Object object : collection) {
            for (String columnName : collection.getTableMetaData().getFieldNames()) {
                buffer.append(((ConditionsObject) object).getFieldValue(columnName));
                buffer.append(fieldDelimiter);
            }
            buffer.setLength(buffer.length() - 1);
            buffer.append('\n');
        }
        buffer.setLength(buffer.length() - 1);
        ps.print(buffer.toString());
        ps.flush();
    }

    /**
     * Print the header for a collection.
     * @param collection the collection
     */
    private void printCollectionHeader(final ConditionsObjectCollection<?> collection) {
        System.out.println("--------------------------------------");
        System.out.print(collection.getConditionsRecord());
        System.out.println("--------------------------------------");
        System.out.println();
        System.out.flush();
    }

    /**
     * Print the column names for a table.
     * @param tableMetaData the table meta data
     */
    private void printColumnNames(final TableMetaData tableMetaData) {
        if (printIDs) {
            ps.print("id");
            ps.print(fieldDelimiter);
        }
        for (String columnName : tableMetaData.getFieldNames()) {
            ps.print(columnName);
            ps.print(fieldDelimiter);
        }
        ps.println();
    }
}
