package org.hps.conditions.cli;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.cli.Option;
import org.hps.conditions.api.ConditionsObject;
import org.hps.conditions.api.ConditionsObjectCollection;
import org.hps.conditions.api.ConditionsRecord.ConditionsRecordCollection;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.database.TableMetaData;

/**
 * This sub-command of the conditions CLI prints conditions conditions table data by run number
 * to the console or optionally writes it to an output file.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
class PrintCommand extends AbstractCommand {
    
    // By default print to the console.
    PrintStream ps = System.out;
    
    // Print IDs along with field values. 
    boolean printIDs = false;
    
    // Print conditions record and table info (default is yes).
    boolean printHeaders = true;

    // Field delimited for print out.
    char fieldDelimiter = ' ';
    
    DatabaseConditionsManager conditionsManager;
    
    PrintCommand() {
        super("print", "Print the table data for a conditions set");
        this.options.addOption(new Option("t", true, "Set the table name"));
        this.options.addOption(new Option("i", false, "Print the ID for the records (off by default)"));
        this.options.addOption(new Option("f", true, "Write print output to a file"));
        this.options.addOption(new Option("H", false, "Suppress printing of conditions record and table info"));
        this.options.addOption(new Option("d", false, "Use tabs for field delimiter instead of spaces"));
        this.options.addOption(new Option("T", true, "Specify a conditions tag to use for filtering records"));
    }
    
    /**
     * Print out the conditions sets selected by the user's command line arguments.
     */
    void execute(String[] arguments) {
        super.execute(arguments);
        
        conditionsManager = DatabaseConditionsManager.getInstance();
        
        if (!conditionsManager.isInitialized()) {
            throw new RuntimeException("conditions system is not initialized");
        }
        
        // User specified tag of conditions records.
        if (this.commandLine.hasOption("T")) {            
            conditionsManager.setTag(commandLine.getOptionValue("T"));
        }
               
        // Print conditions sets matching a specific conditions key.
        String userConditionsKey = null;
        if (this.commandLine.hasOption("t")) {
            userConditionsKey = this.commandLine.getOptionValue("t");
        }                
        
        // Setup an output file for the print out if requested.
        File outputFile = null;
        if (this.commandLine.hasOption("f")) {
            outputFile = new File(commandLine.getOptionValue("f"));
            try {
                ps = new PrintStream(new BufferedOutputStream(new FileOutputStream(outputFile, false)));
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
            if (outputFile.exists()) {
                System.err.println("Specified output file already exists.");
                System.exit(1);
            }
        }              
        
        // Print IDs in the output.
        if (this.commandLine.hasOption("i")) {
            printIDs = true;
        }

        // Print header info.  Option turns this off.
        if (this.commandLine.hasOption("h")) {
            printHeaders = false;
        }

        // Use tabs instead of spaces for field delimiter.
        if (this.commandLine.hasOption("d")) {
            fieldDelimiter = '\t';
        }
                         
        // List of conditions records to print. 
        ConditionsRecordCollection conditionsRecords = new ConditionsRecordCollection();
        
        // Did the user specify a table to use?
        if (userConditionsKey == null) {
            ps.println("printing all conditions");
            // Use all table names if there was not one specified.
            conditionsRecords.addAll(conditionsManager.getConditionsRecords());
        } else {            
            ps.println("printing conditions with name: " + userConditionsKey);
            // Get records only for the user specified table name.
            conditionsRecords.addAll(conditionsManager.findConditionsRecords(userConditionsKey));
        }
        System.out.println(conditionsRecords.size() + " conditions records found");
        
        // Sort the records by key (table name).
        conditionsRecords.sortByKey();
        
        // Get a unique list of keys from the returned conditions records.
        Set<String> conditionsKeys = conditionsRecords.getConditionsKeys();
            
        // Print the records and the data.
        printConditionsRecords(conditionsKeys);   
        ps.flush();           
        ps.close();
    }

    private void printConditionsRecords(Set<String> conditionsKeys) {
        ps.print("printing conditions sets: ");
        for (String conditionsKey : conditionsKeys) {
            ps.print(conditionsKey + " ");
        }
        ps.println();
        ps.println();
        // Loop over the conditions keys from the conditions records.
        for (String conditionsKey : conditionsKeys) {
                                   
            // The list of collections to print.
            List<ConditionsObjectCollection<?>> collectionList = new ArrayList<ConditionsObjectCollection<?>>();
        
            // Get the table meta data for the conditions key.
            TableMetaData tableMetaData = conditionsManager.findTableMetaData(conditionsKey);
            
            // This shouldn't ever happen but check anyways.
            if (tableMetaData == null) {            
                throw new RuntimeException("The table meta data for " + conditionsKey + " does not exist.  The key might be invalid.");
            }
                               
            // Use only the single collection which would be seen by a user job for this run number and key.
            ConditionsObjectCollection<?> collection = conditionsManager.getCachedConditions(
                    tableMetaData.getCollectionClass(), 
                    tableMetaData.getTableName()).getCachedData();
             
            collectionList.add(collection);
        
            // Print out all the collection data to console or file.
            printCollections(collectionList);
        }
    }

    private void printCollections(List<ConditionsObjectCollection<?>> collectionList) {
        // Loop over all the collections and print them.
        for (ConditionsObjectCollection<?> collection : collectionList) {
            if (printHeaders) {
                printCollectionHeader(collection);
            }
            printColumnNames(collection.getTableMetaData());
            printCollection(collection);
            ps.println();
        }
        ps.flush();
    }

    private void printCollection(ConditionsObjectCollection<?> collection) {
        StringBuffer buffer = new StringBuffer();
        for (Object object : collection) {
            for (String columnName : collection.getTableMetaData().getFieldNames()) {
                buffer.append(((ConditionsObject)object).getFieldValue(columnName));
                buffer.append(fieldDelimiter);
            }
            buffer.setLength(buffer.length() - 1);
            buffer.append('\n');
        }
        ps.print(buffer.toString());
        ps.flush();
    }

    private void printCollectionHeader(ConditionsObjectCollection<?> collection) {        
        ps.println("--------------------------------------");
        ps.print(collection.getConditionsRecord());
        ps.println("--------------------------------------");
        ps.println();
        ps.flush();
    }

    private void printColumnNames(TableMetaData tableMetaData) {
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
