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
import org.apache.commons.cli.Options;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.database.QueryBuilder;

/**
 * This is a sub-command to add conditions data using an input text file. The file should be ASCII text that is tab or
 * space delimited and includes headers with the names of the database columns. (These must match exactly!) The user
 * must supply a table name as the target for the SQL insert. An optional collection ID can be supplied, which may not
 * exist already in the table. Otherwise, the command will fail. By default, the next collection ID will be found by the
 * conditions manager.
 * <p>
 * <pre>
 * java -cp hps-distribution-bin.jar org.hps.conditions.cli.CommandLineTool \
 *     -p conditions_dev_local.properties load -t scratch_svt_gains -f ./scratch_svt_gains.txt -c 1
 * </pre>
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
class LoadCommand extends AbstractCommand {

    /**
     * Define command options.
     */
    private static final Options OPTIONS = new Options();
    static {
        OPTIONS.addOption(new Option("h", false, "Show help for load command"));
        OPTIONS.addOption(new Option("t", true, "Set the name of the target table in the database"));
        OPTIONS.addOption(new Option("c", true, "Set the collection ID of this conditions set"));
        OPTIONS.addOption(new Option("f", true, "Set the input data file"));
    }

    /**
     * Class constructor.
     */
    LoadCommand() {
        super("load", "Load a set of conditions into the database from a text file", OPTIONS);
    }

    /**
     * Execute the 'load' command with the given arguments.
     *
     * @param arguments The command arguments.
     */
    @Override
    public void execute(final String[] arguments) {

        final CommandLine commandLine = parse(arguments);

        final String fileName = commandLine.getOptionValue("f");
        if (fileName == null) {
            throw new IllegalArgumentException("Missing file argument.");
        }
        if (!(new File(fileName)).exists()) {
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

        int collectionID;
        if (commandLine.getOptionValue("c") != null) {
            collectionID = Integer.parseInt(commandLine.getOptionValue("c"));
            if (conditionsManager.collectionExists(tableName, collectionID)) {
                throw new IllegalArgumentException("The user supplied collection ID " + collectionID
                        + " already exists in this table.");
            }
        } else {
            collectionID = conditionsManager.getNextCollectionID(tableName);
        }

        final List<String> columnNames = new ArrayList<String>();
        final List<List<String>> rows = new ArrayList<List<String>>();
        parseFile(fileName, columnNames, rows);

        final String insertSql = QueryBuilder.buildInsert(tableName, collectionID, columnNames, rows);
        if (getVerbose()) {
            System.out.println(insertSql);
        }
        // FIXME: This call should go through an object API like ConditionsObjectCollection.insert rather than the
        // manager directly.
        final List<Integer> ids = conditionsManager.updateQuery(insertSql);
        System.out.println("Inserted " + ids.size() + " new rows into table " + tableName + " with collection_id "
                + collectionID);
        conditionsManager.closeConnection(openedConnection);
    }

    /**
     * Parse an input text file and add column names and row data to the input lists.
     * @param fileName The name of the text file.
     * @param columnNames The list of columns (modified by this method).
     * @param rows The list of rows (modified by this method).
     */
    private final void parseFile(final String fileName, final List<String> columnNames, 
            final List<List<String>> rows) {
        final File inputFile = new File(fileName);
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(inputFile));
            final String headerLine = reader.readLine();
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
                final List<String> row = new ArrayList<String>();
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
