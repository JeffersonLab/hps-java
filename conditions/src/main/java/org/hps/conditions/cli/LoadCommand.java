package org.hps.conditions.cli;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.hps.conditions.api.BaseConditionsObjectCollection;
import org.hps.conditions.api.ConditionsObject;
import org.hps.conditions.api.ConditionsObjectCollection;
import org.hps.conditions.api.ConditionsObjectException;
import org.hps.conditions.api.DatabaseObjectException;
import org.hps.conditions.api.TableMetaData;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.lcsim.util.log.LogUtil;
import org.lcsim.util.log.MessageOnlyLogFormatter;

/**
 * This is a sub-command to add conditions data using an input text file. The file should be ASCII text that is tab or
 * space delimited and includes headers with the names of the database columns. (These must match exactly!) The user
 * must supply a table name as the target for the SQL insert. An optional collection ID can be supplied, which may not
 * exist already in the table. Otherwise, the command will fail. By default, the next collection ID will be found by the
 * conditions manager.
 * <p>
 *
 * <pre>
 * java -cp hps-distribution-bin.jar org.hps.conditions.cli.CommandLineTool \
 *     -p conditions_dev_local.properties load -t scratch_svt_gains -f ./scratch_svt_gains.txt -c 1
 * </pre>
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
final class LoadCommand extends AbstractCommand {

    /**
     * The default separator for making tokens from input data.
     */
    private static final String DEFAULT_FIELD_SEPARATOR = " \t";

    /**
     * Setup the logger.
     */
    private static final Logger LOGGER = LogUtil.create(LoadCommand.class, new MessageOnlyLogFormatter(), Level.ALL);

    /**
     * Define command options.
     */
    private static final Options OPTIONS = new Options();
    static {
        OPTIONS.addOption(new Option("h", false, "print help for load command"));
        OPTIONS.addOption(new Option("t", true, "name of the target table (required)"));
        OPTIONS.getOption("t").setRequired(true);
        OPTIONS.addOption(new Option("f", true, "input data file path (required)"));
        OPTIONS.getOption("f").setRequired(true);
        OPTIONS.addOption(new Option("d", true, "description of the collection for log"));
        OPTIONS.addOption(new Option("s", true, "field seperator string (default is tabs or spaces)"));
    }

    /**
     * Class constructor.
     */
    LoadCommand() {
        super("load", "Create a new conditions collection in the database from an input text file", OPTIONS);
    }

    /**
     * Convert from a raw string into a specific type.
     *
     * @param type the target type
     * @param value the raw value
     * @return the value converter to the given type
     */
    Object convertValue(final Class<?> type, final String value) {
        if (Integer.class.equals(type)) {
            return Integer.parseInt(value);
        } else if (Double.class.equals(type)) {
            return Double.parseDouble(value);
        } else if (Float.class.equals(type)) {
            return Float.parseFloat(value);
        } else if (Boolean.class.equals(type)) {
            return Boolean.parseBoolean(value);
        } else {
            return value;
        }
    }

    /**
     * Execute the <i>load</i> command with the given arguments.
     *
     * @param arguments the command arguments
     */
    @Override
    public void execute(final String[] arguments) {

        final CommandLine commandLine = this.parse(arguments);

        final String fileName = commandLine.getOptionValue("f");
        if (fileName == null) {
            throw new IllegalArgumentException("Missing file argument.");
        }
        if (!new File(fileName).exists()) {
            throw new IllegalArgumentException("Input file does not exist: " + fileName);
        }

        final String tableName = commandLine.getOptionValue("t");
        if (tableName == null) {
            throw new IllegalArgumentException("Missing table name.");
        }

        final DatabaseConditionsManager conditionsManager = DatabaseConditionsManager.getInstance();

        boolean openedConnection = false;
        if (!conditionsManager.isConnected()) {
            openedConnection = conditionsManager.openConnection();
        }

        String description = null;
        if (commandLine.hasOption("d")) {
            description = commandLine.getOptionValue("d");
        }

        String separator = DEFAULT_FIELD_SEPARATOR;
        if (commandLine.hasOption("s")) {
            separator = commandLine.getOptionValue("s");
            LOGGER.info("using separator character <" + separator + ">");
        }

        final TableMetaData tableMetaData = conditionsManager.findTableMetaData(tableName);
        BaseConditionsObjectCollection<ConditionsObject> newCollection = null;
        try {
            // Create a new collection. We don't use the specific type here because that won't work later when adding
            // objects to it.
            newCollection = new BaseConditionsObjectCollection<ConditionsObject>(conditionsManager.getConnection(),
                    tableMetaData);
        } catch (SQLException | DatabaseObjectException e) {
            throw new RuntimeException("Error creating new collection.", e);
        }

        LOGGER.info("getting new collection ID ...");

        try {
            conditionsManager.getCollectionId(newCollection, description);
        } catch (final SQLException e) {
            throw new RuntimeException("Error getting collection ID.", e);
        }

        LOGGER.info("collection was assigned ID " + newCollection.getCollectionId());

        LOGGER.info("parsing input file " + fileName + " ...");
        this.parseFile(fileName, newCollection, separator);
        LOGGER.info("Done parsing input file!");

        try {
            LOGGER.info("Inserting collection ...");
            newCollection.insert();
            LOGGER.info("Done inserting collection!");
        } catch (SQLException | DatabaseObjectException e) {
            throw new RuntimeException("Error getting collection ID.", e);
        }

        conditionsManager.closeConnection(openedConnection);
    }

    /**
     * Parse an input text file and create conditions objects from its row data.
     *
     * @param fileName the name of the text file
     * @param collection the collection into which objects will be inserted
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private final void parseFile(final String fileName, final ConditionsObjectCollection collection,
            final String seperator) {

        BufferedReader reader = null;

        try {
            final File inputFile = new File(fileName);
            reader = new BufferedReader(new FileReader(inputFile));

            LOGGER.info("reading in header line ...");

            // Read in the header line with column names.
            final String headerLine = reader.readLine();
            LOGGER.info("got header line: " + headerLine);
            if (headerLine == null) {
                throw new IllegalArgumentException("The file is empty.");
            }
            StringTokenizer tokenizer = new StringTokenizer(headerLine, seperator);
            final List<String> columnNames = new ArrayList<String>();
            while (tokenizer.hasMoreTokens()) {
                final String columnName = tokenizer.nextToken().trim();
                LOGGER.info("read column name: " + columnName);
                columnNames.add(columnName);
            }
            if (columnNames.isEmpty()) {
                throw new RuntimeException("No column names found in file.");
            }

            // Get table info.
            final TableMetaData tableMetaData = collection.getTableMetaData();
            final Class<? extends ConditionsObject> objectClass = tableMetaData.getObjectClass();

            // Get the field names from the table info.
            final List<String> fieldNames = new ArrayList<String>(Arrays.asList(tableMetaData.getFieldNames()));
            fieldNames.remove("collection_id");

            // Check that the column names which were read in from the header row are valid.
            for (final String columnName : columnNames) {
                LOGGER.info("checking column: " + columnName);
                if (!fieldNames.contains(columnName)) {
                    throw new RuntimeException("Unknown column name: " + columnName);
                }
            }

            // Read lines from the file.
            String line = null;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {

                LOGGER.info("reading line " + lineNumber);

                // Create a new conditions object for the row.
                ConditionsObject object;
                try {
                    object = objectClass.newInstance();
                } catch (InstantiationException | IllegalAccessException e) {
                    throw new RuntimeException("Error creating new object.", e);
                }

                // Parse the line.
                tokenizer = new StringTokenizer(line, " \t");
                final int tokens = tokenizer.countTokens();

                // Check that the number of data items is correct.
                if (tokens != columnNames.size()) {
                    throw new RuntimeException("Row " + lineNumber + " has wrong number of data items.");
                }

                // Iterate over the tokens.
                for (int i = 0; i < tokens; i++) {

                    LOGGER.info("proc token " + i);

                    // Get the column name.
                    final String columnName = columnNames.get(i);

                    // Get the column type.
                    final Class<?> columnType = tableMetaData.getFieldType(columnName);

                    // Get the value of the cell.
                    final String value = tokenizer.nextToken();

                    LOGGER.info("columnName: " + columnName);
                    LOGGER.info("columnType: " + columnType.getName());
                    LOGGER.info("value: " + value);

                    // Convert the value to a specific type and set the value on the object.
                    object.setFieldValue(columnNames.get(i), this.convertValue(columnType, value));

                    // Add the object to the collection.
                    LOGGER.info("adding conditions object: " + object);
                    collection.add(object);
                }
                ++lineNumber;
            }
        } catch (final FileNotFoundException e) {
            throw new RuntimeException("The input file does not exist.", e);
        } catch (final IOException e) {
            throw new RuntimeException("Error reading from the file.", e);
        } catch (final ConditionsObjectException e) {
            throw new RuntimeException("Error adding object to collection.", e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
