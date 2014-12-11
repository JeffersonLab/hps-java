package org.hps.conditions.cli;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

import org.apache.commons.cli.Option;
import org.hps.conditions.api.AbstractConditionsObjectCollection;
import org.hps.conditions.api.ConditionsRecord;
import org.hps.conditions.api.ConditionsSeries;
import org.hps.conditions.api.ConditionsRecord.ConditionsRecordCollection;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.database.TableMetaData;
import org.lcsim.conditions.ConditionsManager.ConditionsNotFoundException;

/**
 * This sub-command of the conditions CLI prints conditions table data to
 * the console or optionally writes it to an output file.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
class PrintCommand extends AbstractCommand {

    private static int DEFAULT_RUN_NUMBER = 0;
    private static String DEFAULT_DETECTOR_NAME = "HPS-Proposal2014-v8-6pt6";
    
    PrintCommand() {
        super("print", "Print the table data for a conditions set");
        this.options.addOption(new Option("t", true, "Set the conditions set name"));
        this.options.addOption(new Option("r", true, "Set the run number"));
        this.options.addOption(new Option("d", true, "Set the detector name"));
        this.options.addOption(new Option("f", true, "Write output to a file"));
    }
    
    @SuppressWarnings("rawtypes")
    void execute(String[] arguments) {
        super.execute(arguments);
        
        DatabaseConditionsManager conditionsManager = DatabaseConditionsManager.getInstance();
        
        String conditionsKey = null;
        if (this.commandLine.hasOption("t")) {
            conditionsKey = this.commandLine.getOptionValue("t");
        } else {
            this.printUsage();
            System.exit(1);
        }
        
        String detectorName = DEFAULT_DETECTOR_NAME;
        if (this.commandLine.hasOption("d")) {
            detectorName = this.commandLine.getOptionValue("d");
        }
        
        int runNumber = DEFAULT_RUN_NUMBER;
        if (this.commandLine.hasOption("r")) {
            runNumber = Integer.parseInt(this.commandLine.getOptionValue("r"));
        }
        
        File outputFile = null;
        if (this.commandLine.hasOption("f")) {
            outputFile = new File(commandLine.getOptionValue("f"));
            if (outputFile.exists()) {
                System.err.println("Specified output file already exists.");
                System.exit(1);
            }
        }
        
        try {
            conditionsManager.setDetector(detectorName, runNumber);
        } catch (ConditionsNotFoundException e) {
            throw new RuntimeException(e);
        }
                
        ConditionsRecordCollection conditionsRecords = conditionsManager.findConditionsRecords(conditionsKey);
        for (ConditionsRecord conditionsRecord : conditionsRecords) {
            System.out.println(conditionsRecord.toString());
        }
                
        ConditionsSeries series = conditionsManager.getConditionsSeries(conditionsKey);
 
        PrintStream ps = System.out;
        if (outputFile != null) {
            try {
                ps = new PrintStream(new BufferedOutputStream(new FileOutputStream(outputFile, false)));
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        
        TableMetaData tableMetaData = conditionsManager.findTableMetaData(conditionsKey);
        ps.print("id");
        ps.print(' ');
        String[] fieldNames = tableMetaData.getFieldNames();
        for (String columnName : fieldNames) {
            ps.print(columnName);
            ps.print(' ');
        }
        ps.println();
        
        for (AbstractConditionsObjectCollection collection : series.getCollections()) {
            for (Object object : collection) {
                ps.print(object.toString());
                ps.println();
            }
        }
        ps.flush();
        ps.close();
    }
}