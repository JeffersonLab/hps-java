package org.hps.conditions.cli;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
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
import org.hps.conditions.api.TableRegistry;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.database.MultipleCollectionsAction;
import org.lcsim.conditions.ConditionsManager.ConditionsNotFoundException;

/**
 * Create a conditions system tag.
 * <p>
 * The tag groups together conditions records from the <i>conditions</i> database table with a run validity range that 
 * is between a specified starting and ending run.
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
    
    private MultipleCollectionsAction multipleCollectionsAction = MultipleCollectionsAction.LAST_CREATED; 
    
    private static String getMultipleCollectionsActionString() {
        StringBuffer sb = new StringBuffer();
        for (MultipleCollectionsAction action : MultipleCollectionsAction.values()) {
            sb.append(action.name() + " ");
        }
        sb.setLength(sb.length() - 1);
        return sb.toString();
    }

    /**
     * Define all command options.
     */
    static {
        OPTIONS.addOption(new Option("h", "help", false, "Show help for tag command"));
        OPTIONS.addOption(new Option("t", "tag", true, "Conditions tag name"));
        OPTIONS.addOption(new Option("s", "run-start", true, "Starting run number (required)"));
        OPTIONS.addOption(new Option("e", "run-end", true, "Ending run number (required)"));
        OPTIONS.addOption(new Option("m", "multiple", true, 
                "set run overlap handling (" + getMultipleCollectionsActionString() + ")"));
        OPTIONS.addOption(new Option("D", "dont-prompt", false, "Don't prompt before making tag (careful!)"));
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
        
        // Check if tag exists already.
        if (getManager().getAvailableTags().contains(tagName)) {
            throw new RuntimeException("The tag '" + tagName + "' already exists in the database.");
        }

        // Starting run number (required).
        int runStart = -1;
        if (commandLine.hasOption("s")) {
            runStart = Integer.parseInt(commandLine.getOptionValue("s"));
            LOGGER.config("run start set to " + runStart);
        } else {
            throw new RuntimeException("Missing required -s argument with starting run number.");
        }

        // Ending run number (required).
        int runEnd = -1;
        if (commandLine.hasOption("e")) {
            runEnd = Integer.parseInt(commandLine.getOptionValue("e"));
            LOGGER.config("run end set to " + runEnd);
        } else {
            throw new RuntimeException("Missing required -e argument with starting run number.");
        }

        // Action for disambiguating overlapping collections (default is to use the most recent creation date).
        MultipleCollectionsAction multipleCollectionsAction = MultipleCollectionsAction.LAST_CREATED;
        if (commandLine.hasOption("m")) {
            multipleCollectionsAction = MultipleCollectionsAction
                    .valueOf(commandLine.getOptionValue("m").toUpperCase());
        }
        LOGGER.config("run overlaps will be disambiguated using " + multipleCollectionsAction);

        // Whether to prompt before tagging (default is yes).
        boolean promptBeforeTagging = true;
        if (commandLine.hasOption("d")) {
            promptBeforeTagging = false;
        }
        LOGGER.config("prompt before tagging = " + promptBeforeTagging);

        // Conditions system configuration.
        this.getManager().setXmlConfig("/org/hps/conditions/config/conditions_database_no_svt.xml");

        // Find all the applicable conditions records by their run number ranges.
        ConditionsRecordCollection tagConditionsRecordCollection = this.findConditionsRecords(runStart, runEnd);
        
        if (tagConditionsRecordCollection.size() == 0) {
            throw new RuntimeException("No records found for tag.");
        }

        LOGGER.info("found " + tagConditionsRecordCollection.size() + " conditions records for the tag");

        // Build the collection of tag records to insert into the database.
        final ConditionsTagCollection conditionsTagCollection = this.createConditionsTagCollection(
                tagConditionsRecordCollection, tagName);

        printConditionsRecords(tagConditionsRecordCollection);
        
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
     * Print information about conditions records in the tag to the log.
     * 
     * @param collection the conditions tag collection
     */
    private void printConditionsRecords(ConditionsRecordCollection records) {
        StringBuffer sb = new StringBuffer();
        Set<String> keys = new TreeSet<String>(records.getConditionsKeys());
        for (String key : keys) {
            ConditionsRecordCollection keyRecords = records.findByKey(key);
            keyRecords.sortByKey();
            for (ConditionsRecord record : keyRecords) {
                sb.append("conditions_id: " + record.getRowId() + ", name: " + record.getName() + ", collection_id: "
                        + record.getCollectionId() + ", run_start: " + record.getRunStart() 
                        + ", run_end: " + record.getRunEnd() + ", notes: " + record.getNotes() + '\n');
                
            }
        }        
        LOGGER.info("including " + records.size() + " records in tag ..." + '\n' + sb.toString());
    }
     
    /**
     * Scan through a run range to find conditions records for the tag.
     * 
     * @param runStart the starting run number
     * @param runEnd the ending run number
     * @return the conditions records for the tag
     */
    private ConditionsRecordCollection findConditionsRecords(final int runStart, final int runEnd) {
        if (runStart < 0 ) {
            throw new IllegalArgumentException("The run start " + runStart + " is invalid.");
        }
        if (runEnd < 0 ) {
            throw new IllegalArgumentException("The run end " + runEnd + " is invalid.");
        }
        if (runStart > runEnd ) {
            throw new IllegalArgumentException("The run start is greater than the run end.");
        }
        DatabaseConditionsManager dbManager = this.getManager();
        if (dbManager.isFrozen()) {
            dbManager.unfreeze();
        }
        if (!dbManager.getActiveTags().isEmpty()) {
            dbManager.clearTags();
        }
        final String detectorName = "HPS-dummy-detector";
        ConditionsRecordCollection tagRecords = new ConditionsRecordCollection();
        Set<Integer> ids = new HashSet<Integer>();
        for (int run = runStart; run <= runEnd; run++) {
            LOGGER.info("loading run " + run);
            try {
                dbManager.setDetector(detectorName, run);
            } catch (ConditionsNotFoundException e) {
                throw new RuntimeException(e);
            }
            ConditionsRecordCollection runRecords = dbManager.getConditionsRecords();
            Set<String> keys = runRecords.getConditionsKeys();
            LOGGER.fine("run has " + runRecords.size() + " conditions records");
            for (String key : keys) {
                ConditionsRecord record = runRecords.findUniqueRecord(key, this.multipleCollectionsAction);
                if (record == null) {
                    throw new RuntimeException("Missing expected unique condition record for " + key + ".");
                }
                if (!ids.contains(record.getRowId())) {
                    try {
                        LOGGER.fine("adding conditions to tag ..." + '\n' + record.toString());
                        tagRecords.add(record);
                        ids.add(record.getRowId());
                    } catch (ConditionsObjectException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    LOGGER.fine("Conditions record with row id " + record.getRowId() + " is already in the tag.");
                }
            }
            LOGGER.info("done processing run " + run);
        }
        LOGGER.info("Found " + tagRecords.size() + " conditions records for tag.");
        return tagRecords;
    }
}
