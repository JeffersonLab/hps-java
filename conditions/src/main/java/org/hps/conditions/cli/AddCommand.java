package org.hps.conditions.cli;

import java.sql.SQLException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.hps.conditions.api.ConditionsRecord;
import org.hps.conditions.api.DatabaseObjectException;
import org.hps.conditions.api.FieldValuesMap;
import org.hps.conditions.api.TableRegistry;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.lcsim.util.log.LogUtil;

/**
 * This is a command for the conditions CLI that will add a conditions record, making a conditions set with a particular
 * collection ID available by run number via the {@link org.hps.conditions.database.DatabaseConditionsManager}.
 *
 * @author Jeremy McCormick, SLAC
 */
final class AddCommand extends AbstractCommand {

    /**
     * Setup logger.
     */
    private static final Logger LOGGER = LogUtil.create(AddCommand.class);

    /**
     * Define command line options.
     */
    private static final Options OPTIONS = new Options();
    static {
        OPTIONS.addOption(new Option("h", false, "print help for add command"));
        OPTIONS.addOption("r", true, "starting run number (required)");
        OPTIONS.getOption("r").setRequired(true);
        OPTIONS.addOption("e", true, "ending run number (default is starting run number)");
        OPTIONS.addOption("t", true, "table name (required)");
        OPTIONS.getOption("t").setRequired(true);
        OPTIONS.addOption("c", true, "collection ID (required)");
        OPTIONS.getOption("c").setRequired(true);
        OPTIONS.addOption("u", true, "user name (optional)");
        OPTIONS.addOption("m", true, "notes about this conditions set (optional)");
    }

    /**
     * Class constructor.
     */
    AddCommand() {
        super("add", "Add a conditions record to associate a collection to a run range", OPTIONS);
    }

    /**
     * Create a conditions record.
     *
     * @param runStart the run start
     * @param runEnd the run end
     * @param tableName the table name
     * @param name the key name
     * @param collectionId the collection ID
     * @param createdBy the user name
     * @param tag the conditions tag
     * @param notes the text notes about the collection
     * @return the new conditions record
     */
    private ConditionsRecord createConditionsRecord(final int runStart, final int runEnd, final String tableName,
            final String name, final int collectionId, final String createdBy, final String notes) {
        final ConditionsRecord conditionsRecord = new ConditionsRecord();
        final FieldValuesMap fieldValues = new FieldValuesMap();
        fieldValues.setValue("run_start", runStart);
        fieldValues.setValue("run_end", runEnd);
        fieldValues.setValue("table_name", tableName);
        fieldValues.setValue("name", name);
        fieldValues.setValue("collection_id", collectionId);
        fieldValues.setValue("created_by", createdBy);
        if (notes != null) {
            fieldValues.setValue("notes", notes);
        }
        conditionsRecord.setFieldValues(fieldValues);
        fieldValues.setValue("created", new Date());
        return conditionsRecord;
    }

    /**
     * Execute the command with the given arguments.
     *
     * @param arguments the command line arguments
     */
    @Override
    final void execute(final String[] arguments) {

        final CommandLine commandLine = this.parse(arguments);

        // Run start (required).
        final int runStart;
        if (commandLine.hasOption("r")) {
            runStart = Integer.parseInt(commandLine.getOptionValue("r"));
        } else {
            throw new RuntimeException("Missing required -r option with run number.");
        }

        // Run end.
        int runEnd = runStart;
        if (commandLine.hasOption("e")) {
            runEnd = Integer.parseInt(commandLine.getOptionValue("e"));
        }

        // Name of table (required).
        String tableName;
        if (commandLine.hasOption("t")) {
            tableName = commandLine.getOptionValue("t");
        } else {
            throw new RuntimeException("Missing required -t argument with table name");
        }
        final String name = tableName;

        // Collection ID (required).
        int collectionId;
        if (commandLine.hasOption("c")) {
            collectionId = Integer.parseInt(commandLine.getOptionValue("c"));
        } else {
            throw new RuntimeException("Missing required -c argument with collection ID");
        }

        // User name.
        String createdBy = System.getProperty("user.name");
        if (commandLine.hasOption("u")) {
            createdBy = commandLine.getOptionValue("u");
        }

        // Notes (optional).
        String notes = null;
        if (commandLine.hasOption("m")) {
            notes = commandLine.getOptionValue("m");
        }

        // Create the conditions record to insert.
        final ConditionsRecord conditionsRecord = this.createConditionsRecord(runStart, runEnd, tableName, name,
                collectionId, createdBy, notes);
        LOGGER.info("inserting conditions record ..." + '\n' + conditionsRecord);
        try {
            boolean createdConnection = false;
            final DatabaseConditionsManager manager = DatabaseConditionsManager.getInstance();
            if (!DatabaseConditionsManager.getInstance().isConnected()) {
                createdConnection = manager.openConnection();
            }
            conditionsRecord.setConnection(manager.getConnection());
            conditionsRecord.setTableMetaData(TableRegistry.getTableRegistry().findByTableName("conditions"));
            conditionsRecord.insert();
            manager.closeConnection(createdConnection);
        } catch (final SQLException | DatabaseObjectException e) {
            LOGGER.log(Level.SEVERE, "Error adding conditions record", e);
            throw new RuntimeException("An error occurred while adding a conditions record.", e);
        }
        LOGGER.info("successfully added conditions record ..." + '\n' + conditionsRecord);
    }
}
