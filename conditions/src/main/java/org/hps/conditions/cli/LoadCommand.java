package org.hps.conditions.cli;

import java.io.File;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.hps.conditions.api.BaseConditionsObjectCollection;
import org.hps.conditions.api.ConditionsObject;
import org.hps.conditions.api.DatabaseObjectException;
import org.hps.conditions.api.TableMetaData;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.lcsim.util.log.LogUtil;
import org.lcsim.util.log.MessageOnlyLogFormatter;

/**
 * This is a sub-command to add conditions data using an input text file. The file should be ASCII text that is comma
 * delimited with the first row containing headers that exactly match the table column names. The user must supply a
 * table name as the target for the SQL insert. An optional collection ID can be supplied, which is not allowed to exist
 * already in the table. Otherwise, the command will fail. By default, the next collection ID will be found by the
 * conditions manager.
 * <p>
 *
 * <pre>
 * java -cp hps-distribution-bin.jar org.hps.conditions.cli.CommandLineTool \
 *     -p conditions_dev_local.properties load -t scratch_svt_gains -f ./scratch_svt_gains.txt -c 1
 * </pre>
 *
 * @author Jeremy McCormick, SLAC
 */
final class LoadCommand extends AbstractCommand {

    /**
     * The default separator for making tokens from input data (tab-delimited).
     */
    private static final String DEFAULT_FIELD_SEPARATOR = "\t";

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
        OPTIONS.addOption(new Option("c", true, "field delimiter (default is tabs)"));
    }

    /**
     * Class constructor.
     */
    LoadCommand() {
        super("load", "Create a new conditions collection in the database from an input text file", OPTIONS);
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
        if (commandLine.hasOption("c")) {
            separator = commandLine.getOptionValue("c");
            if (separator.length() > 1) {
                throw new IllegalArgumentException("Separator must be a single character.");
            }
            LOGGER.info("using separator character <" + separator + ">");
        }

        final TableMetaData tableMetaData = conditionsManager.findTableMetaData(tableName);
        final BaseConditionsObjectCollection<ConditionsObject> newCollection = new BaseConditionsObjectCollection<ConditionsObject>(
                conditionsManager.getConnection(), tableMetaData);

        LOGGER.info("getting new collection ID ...");

        try {
            conditionsManager.getCollectionId(newCollection, description);
        } catch (final SQLException e) {
            throw new RuntimeException("Error getting collection ID.", e);
        }

        LOGGER.info("collection was assigned ID " + newCollection.getCollectionId());

        LOGGER.info("parsing input file " + fileName + " ...");
        try {
            newCollection.load(new File(fileName), separator.charAt(0));
        } catch (final Exception e) {
            throw new RuntimeException("Error loading CSV file.", e);
        }
        LOGGER.info("Done parsing input file!");

        try {
            LOGGER.info("Inserting collection ...");
            newCollection.insert();
            LOGGER.info("Done inserting collection!");
        } catch (SQLException | DatabaseObjectException e) {
            throw new RuntimeException("Error getting collection ID.", e);
        }

        conditionsManager.closeConnection(openedConnection);

        LOGGER.info("Collection was loaded successfully!");
    }
}
