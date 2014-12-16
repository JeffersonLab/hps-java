package org.hps.conditions.cli;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.apache.commons.cli.Option;
import org.hps.conditions.api.AbstractConditionsObjectCollection;
import org.hps.conditions.api.ConditionsObject;
import org.hps.conditions.api.ConditionsRecord.ConditionsRecordCollection;
import org.hps.conditions.api.ConditionsSeries;
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
    
    // Print all available conditions sets without disambiguation.
    boolean printAllAvailable = false;

    // Print conditions record and table info (default is yes).
    boolean printHeaders = true;

    // Field delimited for print out.
    char fieldDelimiter = ' ';
    
    PrintCommand() {
        super("print", "Print the table data for a conditions set");
        this.options.addOption(new Option("t", true, "Set the conditions set name"));
        this.options.addOption(new Option("a", false, "Use all available conditions for the run number and key name"));
        this.options.addOption(new Option("i", false, "Print the ID for the records (off by default)"));
        this.options.addOption(new Option("f", true, "Write print output to a file"));
        this.options.addOption(new Option("H", false, "Suppress printing of conditions record and table info"));
        this.options.addOption(new Option("T", false, "Use tabs for field delimiter instead of spaces"));
    }
    
    /**
     * Print out the conditions sets selected by the user's command line arguments.
     */
    @SuppressWarnings("rawtypes")
    void execute(String[] arguments) {
        super.execute(arguments);
        
        DatabaseConditionsManager conditionsManager = DatabaseConditionsManager.getInstance();
        if (!this.verbose) {
            // If not running in verbose mode then only print severe errors from conditions manager.
            conditionsManager.setLogLevel(Level.SEVERE);
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

        // Print all available conditions with this key and run number and do not disambiguate the collections (e.g. by date).
        if (this.commandLine.hasOption("a")) {
            printAllAvailable = true;
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
        if (this.commandLine.hasOption("T")) {
            fieldDelimiter = '\t';
        }
                         
        // Get a list of conditions records from the key.
        ConditionsRecordCollection conditionsRecords = new ConditionsRecordCollection();
        // Did user specify a key to use?
        if (userConditionsKey == null) {
            // Use all key names if there was not one specified.
            conditionsRecords.addAll(conditionsManager.getConditionsRecords());
        } else {            
            // Get records for the user specified key.
            conditionsRecords.addAll(conditionsManager.findConditionsRecords(userConditionsKey));
        }
        
        conditionsRecords.sortByKey();
        
        // Get a unique list of keys from the returned conditions records.
        Set<String> conditionsKeys = conditionsRecords.getConditionsKeys();
            
        // Loop over the conditions keys from the conditions records.
        for (String conditionsKey : conditionsKeys) {
                       
            // The list of collections to print.
            List<AbstractConditionsObjectCollection> collectionList = new ArrayList<AbstractConditionsObjectCollection>();
        
            // Get the table meta data for the conditions key.
            TableMetaData tableMetaData = conditionsManager.findTableMetaData(conditionsKey);
            
            // This shouldn't ever happen but check anyways.
            if (tableMetaData == null) {            
                throw new RuntimeException("The table meta data for " + conditionsKey + " does not exist.  The key might be invalid.");
            }
        
            // Should all available collections be printed?
            if (printAllAvailable) {
                // Use all available conditions sets for this run number and key, without performing any disambiguation.
                ConditionsSeries series = conditionsManager.getConditionsSeries(conditionsKey);
                collectionList.addAll(series);
            } else {
                // Use only the single collection which would be seen by a user job for this run number and key.
                AbstractConditionsObjectCollection collection = conditionsManager.getCollection(tableMetaData.getCollectionClass());
                collectionList.add(collection);
            }

            // Print out all the collection data to console or file.
            printCollections(collectionList);
        }   
        ps.flush();           
        ps.close();
    }

    private void printCollections(List<AbstractConditionsObjectCollection> collectionList) {
        // Loop over all the collections and print them.
        for (AbstractConditionsObjectCollection collection : collectionList) {
            if (printHeaders) {
                printCollectionHeader(collection);
            }
            printColumnNames(collection.getTableMetaData());
            printCollection(collection);
            ps.println();
        }
    }

    private void printCollection(AbstractConditionsObjectCollection collection) {
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
    }

    private void printCollectionHeader(AbstractConditionsObjectCollection collection) {
        ps.println("--------------------------------------");
        ps.println();
        ps.println(collection.getConditionsRecord());
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
