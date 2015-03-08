package org.hps.conditions.cli;

import java.io.PrintStream;
import java.util.Date;

import org.hps.conditions.api.ConditionsObjectException;
import org.hps.conditions.api.ConditionsRecord;
import org.hps.conditions.api.FieldValueMap;
import org.hps.conditions.database.DatabaseConditionsManager;

/**
 * This is a command for the conditions CLI that will add a conditions record,
 * making a conditions set with a particular collection ID available by 
 * run number via the {@link org.hps.conditions.database.DatabaseConditionsManager}.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class AddCommand extends AbstractCommand {
    
    PrintStream ps = System.out;
    
    AddCommand() {
        super("add", "Add a conditions record to associate a collection to a run range");
        options.addOption("r", true, "The starting run number (required)");
        options.getOption("r").setRequired(true);
        options.addOption("e", true, "The ending run number (default is starting run number)");
        options.addOption("t", true, "The table name (required)");
        options.getOption("t").setRequired(true);
        options.addOption("c", true, "The collection ID (required)");
        options.getOption("c").setRequired(true);
        options.addOption("T", true, "A tag value (optional)");
        options.addOption("u", true, "Your user name (optional)");
        options.addOption("m", true, "The notes about this conditions set (optional)");
    }
       
    void execute(String[] arguments) {
        super.execute(arguments);
        
        // This command has 3 required options.
        if (commandLine.getOptions().length == 0) {
            this.printUsage();
            System.exit(1);
        }

        // Run start (required).
        int runStart;        
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
        String name = tableName;

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
        
        // Tag to assign (optional).
        String tag = null;
        if (commandLine.hasOption("T")) {
            tag = commandLine.getOptionValue("T");
        }
        
        // Notes (optional).
        String notes = null;
        if (commandLine.hasOption("m")) {
            notes = commandLine.getOptionValue("m");
        }
        
        // Create the conditions record to insert.
        ConditionsRecord conditionsRecord = createConditionsRecord(
                runStart, 
                runEnd, 
                tableName, 
                name, 
                collectionId, 
                createdBy, 
                tag, 
                notes);       
        try {
            boolean createdConnection = false;
            DatabaseConditionsManager manager = DatabaseConditionsManager.getInstance();
            if (!DatabaseConditionsManager.getInstance().isConnected()) {
                createdConnection = manager.openConnection();
            }
            conditionsRecord.insert();
            manager.closeConnection(createdConnection);
        } catch (ConditionsObjectException e) {
            throw new RuntimeException("An error occurred while adding a conditions record.", e);
        }
        ps.println("successfully added conditions record ...");
        ps.println(conditionsRecord);
    }

    /**
     * @param runStart
     * @param runEnd
     * @param tableName
     * @param name
     * @param collectionId
     * @param createdBy
     * @param tag
     * @param notes
     * @return
     */
    private ConditionsRecord createConditionsRecord(int runStart, int runEnd, String tableName, String name, int collectionId, String createdBy, String tag, String notes) {
        ConditionsRecord conditionsRecord = new ConditionsRecord();
        FieldValueMap fieldValues = new FieldValueMap();
        fieldValues.put("run_start", runStart);
        fieldValues.put("run_end", runEnd);
        fieldValues.put("table_name", tableName);
        fieldValues.put("name", name);
        fieldValues.put("collection_id", collectionId);
        fieldValues.put("created_by", createdBy);
        if (tag != null) {
            fieldValues.put("tag", tag);
        }
        if (notes != null) {
            fieldValues.put("notes", notes);
        }
        conditionsRecord.setFieldValues(fieldValues);
        fieldValues.put("created", new Date());
        return conditionsRecord;
    }
}