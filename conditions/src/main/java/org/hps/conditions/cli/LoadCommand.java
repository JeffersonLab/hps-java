package org.hps.conditions.cli;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.database.QueryBuilder;

/**
 * <p>
 * This is a sub-command to add conditions data using an input text file. The
 * file should be ASCII text that is tab or space delimited and includes headers
 * with the names of the database columns. (These must match exactly!) The user
 * must supply a table name as the target for the SQL insert. An optional
 * collection ID can be supplied, which may not exist already in the table.
 * Otherwise, the command will fail. By default, the next collection ID will be
 * found by the conditions manager.
 * <p>
 * 
 * <pre>
 * java -cp hps-distribution-bin.jar org.hps.conditions.cli.CommandLineTool -p conditions_dev_local.properties \
 *     load -t scratch_svt_gains -f ./scratch_svt_gains.txt -c 1
 * </pre>
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
class LoadCommand extends AbstractCommand {

    LoadCommand() {
        super("load", "Load a set of conditions into the database from a text file");
        this.options.addOption(new Option("t", true, "Set the name of the target table in the database"));
        this.options.addOption(new Option("c", true, "Set the collection ID of this conditions set"));
        this.options.addOption(new Option("f", true, "Set the input data file"));
    }

    @Override
    public void execute(String[] arguments) {
        super.execute(arguments);
        
        String fileName = commandLine.getOptionValue("f");
        if (fileName == null) {
            throw new IllegalArgumentException("Missing file argument.");
        }

        String tableName = commandLine.getOptionValue("t");
        if (tableName == null) {
            throw new IllegalArgumentException("Missing table name.");
        }

        DatabaseConditionsManager conditionsManager = DatabaseConditionsManager.getInstance();
        if (conditionsManager == null) {
            throw new RuntimeException("The DatabaseConditionsManager was not setup properly.");
        }

        int collectionID;
        if (commandLine.getOptionValue("c") != null) {
            collectionID = Integer.parseInt(commandLine.getOptionValue("c"));
            if (conditionsManager.collectionExists(tableName, collectionID)) {
                throw new IllegalArgumentException("The user supplied collection ID " + collectionID + " already exists in this table.");
            }
        } else {
            collectionID = conditionsManager.getNextCollectionID(tableName);
        }

        List<String> columnNames = new ArrayList<String>();
        List<List<String>> rows = new ArrayList<List<String>>();
        parseFile(fileName, columnNames, rows);

        String insertSql = QueryBuilder.buildInsert(tableName, collectionID, columnNames, rows);
        if (verbose)
            System.out.println(insertSql);
        List<Integer> IDs = conditionsManager.updateQuery(insertSql);
        System.out.println("Inserted " + IDs.size() + " new rows into table " + tableName + " with collection_id " + collectionID);
    }

    void parseFile(String fileName, List<String> columnNames, List<List<String>> rows) {
        File inputFile = new File(fileName);
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(inputFile));
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new IllegalArgumentException("The file is empty.");
            }
            StringTokenizer tokenizer = new StringTokenizer(headerLine, " \t");
            while (tokenizer.hasMoreTokens()) {
                columnNames.add(tokenizer.nextToken().trim());
            }
            String line = null;
            while ((line = reader.readLine()) != null) {
                tokenizer = new StringTokenizer(line, " \t");
                List<String> row = new ArrayList<String>();
                while (tokenizer.hasMoreTokens()) {
                    row.add(tokenizer.nextToken().trim());
                }
                rows.add(row);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
