package org.hps.conditions.cli;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.hps.conditions.api.ConditionsObjectException;
import org.hps.conditions.api.ConditionsRecord;
import org.hps.conditions.api.ConditionsRecord.ConditionsRecordCollection;
import org.hps.conditions.api.ConditionsTag;
import org.hps.conditions.api.ConditionsTag.ConditionsTagCollection;
import org.hps.conditions.api.DatabaseObjectException;
import org.hps.conditions.api.TableMetaData;
import org.hps.conditions.api.TableRegistry;
import org.hps.conditions.database.MultipleCollectionsAction;

/**
 * Create a conditions system tag.
 * <p>
 * The tag groups together conditions records from the <i>conditions</i> database table with a run validity range that 
 * is between a specified starting and ending run.
 * <p>
 * Tagging will not disambiguate overlapping conditions, which is done at run-time based on the current run number.
 *
 * @author Jeremy McCormick, SLAC
 */
final class TagCommand extends AbstractCommand {

    /**
     * Initialize the logger.
     */
    private static final Logger LOGGER = Logger.getLogger(TagCommand.class.getPackage().getName());
    
    /**
     * Defines command options.
     */
    private static Options OPTIONS = new Options();

    /**
     * Define all command options.
     */
    static {
        OPTIONS.addOption(new Option("h", false, "Show help for tag command"));
        OPTIONS.addOption(new Option("t", true, "Conditions tag name"));
        OPTIONS.addOption(new Option("s", true, "Starting run number (required)"));
        OPTIONS.getOption("s").setRequired(true);
        OPTIONS.addOption(new Option("e", true, "Ending run number (default is unlimited)"));
        OPTIONS.getOption("t").setRequired(true);
        OPTIONS.addOption(new Option("m", true,
                "MultipleCollectionsAction to use for disambiguation (default is LAST_CREATED)"));
        OPTIONS.addOption(new Option("d", false, "Don't prompt before making tag (be careful!)"));
    }

    /**
     * Class constructor.
     */
    TagCommand() {
        super("tag", "Tag a set of conditions records to group them together", OPTIONS);
    }

    /**
     * Create the collection with the records for creating a new conditions "tag".
     *
     * @param tagConditionsRecordCollection the tag record collection
     * @param tagName the tag name
     * @return the tag record collection
     */
    private ConditionsTagCollection createConditionsTagCollection(
            final ConditionsRecordCollection tagConditionsRecordCollection, final String tagName) {
        final ConditionsTagCollection conditionsTagCollection = new ConditionsTagCollection();
        conditionsTagCollection.setConnection(this.getManager().getConnection());
        conditionsTagCollection.setTableMetaData(TableRegistry.getTableRegistry().findByTableName("conditions_tags"));
        for (final ConditionsRecord conditionsRecord : tagConditionsRecordCollection) {
            final ConditionsTag conditionsTag = new ConditionsTag(conditionsRecord.getRowId(), tagName);
            try {
                conditionsTagCollection.add(conditionsTag);
            } catch (final ConditionsObjectException e) {
                throw new RuntimeException(e);
            }
        }
        return conditionsTagCollection;
    }

    /**
     * Execute the tag command.
     */
    @Override
    void execute(final String[] arguments) {

        final CommandLine commandLine = this.parse(arguments);

        // New tag name.
        final String tagName;
        if (commandLine.hasOption("t")) {
            tagName = commandLine.getOptionValue("t");
            LOGGER.info("tag name set to " + tagName);
        } else {
            throw new RuntimeException("Missing required -t argument with the tag name.");
        }

        // Starting run number (required).
        int runStart = -1;
        if (commandLine.hasOption("s")) {
            runStart = Integer.parseInt(commandLine.getOptionValue("s"));
            LOGGER.config("run start set to " + runStart);
        } else {
            throw new RuntimeException("missing require -s argument with starting run number");
        }

        // Ending run number (max integer is default).
        int runEnd = Integer.MAX_VALUE;
        if (commandLine.hasOption("e")) {
            runEnd = Integer.parseInt(commandLine.getOptionValue("e"));
            LOGGER.config("run end set to " + runEnd);
        }

        // Run end must be greater than or equal to run start.
        if (runEnd < runStart) {
            throw new IllegalArgumentException("runEnd < runStart");
        }

        // Action for disambiguating overlapping collections (default is to use the most recent creation date).
        MultipleCollectionsAction multipleCollectionsAction = MultipleCollectionsAction.LAST_CREATED;
        if (commandLine.hasOption("m")) {
            multipleCollectionsAction = MultipleCollectionsAction
                    .valueOf(commandLine.getOptionValue("m").toUpperCase());
        }
        LOGGER.config("multiple collections action set tco " + multipleCollectionsAction);

        // Whether to prompt before tagging (default is yes).
        boolean promptBeforeTagging = true;
        if (commandLine.hasOption("d")) {
            promptBeforeTagging = false;
        }
        LOGGER.config("prompt before tagging: " + promptBeforeTagging);

        // Conditions system configuration.
        this.getManager().setXmlConfig("/org/hps/conditions/config/conditions_database_no_svt.xml");

        // Find all the applicable conditions records by their run number ranges.
        ConditionsRecordCollection tagConditionsRecordCollection = this.findConditionsRecords(runStart, runEnd);

        LOGGER.info("found " + tagConditionsRecordCollection.size() + " conditions records for the tag");

        // Build the collection of tag records to insert into the database.
        final ConditionsTagCollection conditionsTagCollection = this.createConditionsTagCollection(
                tagConditionsRecordCollection, tagName);

        LOGGER.info("created " + conditionsTagCollection.size() + " tag records ..." + '\n' + conditionsTagCollection);

        // Prompt user to verify tag creation.
        boolean createTag = true;
        if (promptBeforeTagging) {
            System.out.println("Create conditions tag '" + tagName + "' in the database?  (Y/N)");
            final String line = System.console().readLine();
            if (!line.equals("Y")) {
                createTag = false;
            }
        }

        // Create the tag.
        if (createTag) {
            try {
                LOGGER.info("creating tag " + tagName + " in the database ...");
                conditionsTagCollection.insert();
            } catch (DatabaseObjectException | SQLException e) {
                throw new RuntimeException(e);
            }
        } else {
            LOGGER.warning("user aborted tag operation!");
        }

        LOGGER.info("done!");
    }
   
    /**
     * Find all the conditions records that are applicable for the given run range.
     * <p>
     * Overlapping run numbers in conditions with the same key are not disambiguated.
     * This must be done in the user's job at runtime; usually the most recently created 
     * conditions record will be used if multiple one's are applicable to the current run.
     *
     * @param runStart the start run
     * @param runEnd the end run (must be greater than or equal to <code>runStart</code>)
     * @return the conditions records that fall in the run range
     */
    private ConditionsRecordCollection findConditionsRecords(final int runStart, final int runEnd) {
        if (runStart > runEnd) {
            throw new IllegalArgumentException("runStart > runEnd");
        }
        if (runStart < 0) {
            throw new IllegalArgumentException("invalid runStart: " + runStart);
        }
        if (runEnd < 0) {
            throw new IllegalArgumentException("invalid runEnd: " + runEnd);
        }
        final Connection connection = this.getManager().getConnection();
        final ConditionsRecordCollection conditionsRecordCollection = new ConditionsRecordCollection();
        final TableMetaData tableMetaData = TableRegistry.getTableRegistry().findByTableName("conditions");
        PreparedStatement statement = null;
        try {
            /*
             * SQL statement handles 3 cases: 
             * 1) condition's run_start in range 
             * 2) condition's run_end in range 
             * 3) condition's run_start and run_end enclose the range
             */
            statement = connection
                    .prepareStatement("SELECT id FROM conditions WHERE (run_start >= ? and run_start <= ?) or (run_end >= ? and run_end <= ?)"
                            + " or (run_start <= ? and run_end >= ?)");
            statement.setInt(1, runStart);
            statement.setInt(2, runEnd);
            statement.setInt(3, runStart);
            statement.setInt(4, runEnd);
            statement.setInt(5, runStart);
            statement.setInt(6, runEnd);

            final ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                final ConditionsRecord record = new ConditionsRecord();
                record.setConnection(connection);
                record.setTableMetaData(tableMetaData);
                record.select(resultSet.getInt(1));
                conditionsRecordCollection.add(record);
            }
        } catch (DatabaseObjectException | ConditionsObjectException | SQLException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (statement != null) {
                    statement.close();
                }
            } catch (final SQLException e) {
                e.printStackTrace();
            }
        }
        return conditionsRecordCollection;
    }
}
