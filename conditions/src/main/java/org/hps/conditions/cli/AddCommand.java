package org.hps.conditions.cli;

import java.io.PrintStream;
import java.util.Date;

import org.hps.conditions.api.ConditionsObjectException;
import org.hps.conditions.api.ConditionsRecord;
import org.hps.conditions.api.FieldValueMap;

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
        super("add", "Add a conditions record");
        options.addOption("r", true, "The starting run number");
        options.getOption("r").setRequired(true);
        options.addOption("e", true, "The ending run number");
        options.addOption("k", true, "The conditions key");
        options.addOption("t", true, "The table name");
        options.getOption("t").setRequired(true);
        options.addOption("T", true, "A tag value");
        options.addOption("u", true, "Your user name");
        options.addOption("c", true, "The collection ID");
        options.getOption("c").setRequired(true);
        options.addOption("m", true, "The notes");
    }
       
    void execute(String[] arguments) {
        super.execute(arguments);
                
        int runStart = Integer.parseInt(commandLine.getOptionValue("r"));
        int runEnd = runStart;
        if (commandLine.hasOption("e")) {
            runEnd = Integer.parseInt(commandLine.getOptionValue("e"));
        }
        
        String tableName = commandLine.getOptionValue("t");
        String name = tableName;
        if (commandLine.hasOption("k")) {
            name = commandLine.getOptionValue("k");
        }
        
        String createdBy = System.getProperty("user.name");
        if (commandLine.hasOption("u")) {
            createdBy = commandLine.getOptionValue("u");
        }
        
        String tag = null;
        if (commandLine.hasOption("T")) {
            tag = commandLine.getOptionValue("T");
        }
        
        String notes = null;
        if (commandLine.hasOption("m")) {
            notes = commandLine.getOptionValue("m");
        }
        
        int collectionId = Integer.parseInt(commandLine.getOptionValue("c"));

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
        try {
            conditionsRecord.insert();
        } catch (ConditionsObjectException e) {
            throw new RuntimeException("An error occurred while adding a conditions record.", e);
        }
        ps.println("successfully added conditions record ...");
        ps.println(conditionsRecord);
    }

}
