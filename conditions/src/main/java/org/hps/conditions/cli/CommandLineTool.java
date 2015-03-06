package org.hps.conditions.cli;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.lcsim.conditions.ConditionsManager.ConditionsNotFoundException;

/**
 * <p>
 * This class is a command-line tool for performing commands on the conditions
 * database. It has sub-commands much like the cvs or svn clients. 
 * <p>
 * Command line options allow a custom connection properties file or XML
 * configuration to be supplied by the user which will override the default.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class CommandLineTool {

    Options options = new Options();
    Map<String, AbstractCommand> commands = new HashMap<String, AbstractCommand>();
    PosixParser parser = new PosixParser();
    DatabaseConditionsManager conditionsManager;
    boolean verbose = false;

    public static void main(String[] arguments) {
        CommandLineTool.create().run(arguments);
    }

    void run(String[] arguments) {
        try {
            if (arguments.length == 0) {
                printUsage();
                exit(0);
            }

            CommandLine commandLine = null;
            try {
                commandLine = parser.parse(options, arguments, true);
            } catch (ParseException e) {
                e.printStackTrace();
                printUsage();
                exit(1);
            }

            if (commandLine.hasOption("h") || commandLine.getArgs().length == 0) {
                printUsage();
                exit(0);
            }

            if (commandLine.hasOption("v")) {
                verbose = true;
            }

            setupConditionsManager(commandLine);

            String commandName = commandLine.getArgs()[0];

            AbstractCommand command = commands.get(commandName);
            if (command == null) {
                throw new IllegalArgumentException("Unknown command " + commandName);
            }

            String[] commandArguments = new String[commandLine.getArgs().length - 1];
            System.arraycopy(commandLine.getArgs(), 1, commandArguments, 0, commandArguments.length);

            command.setVerbose(verbose);
            command.execute(commandArguments);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        } finally {
            conditionsManager.closeConnection();
        }
    }

    private void setupConditionsManager(CommandLine commandLine) {
        conditionsManager = new DatabaseConditionsManager();
        if (verbose) {
            conditionsManager.setLogLevel(Level.ALL);
        } else {
            conditionsManager.setLogLevel(Level.WARNING);
        }
        if (commandLine.hasOption("p")) {
            File connectionPropertiesFile = new File(commandLine.getOptionValue("p"));
            if (verbose)
                System.out.println("using connection properties file " + connectionPropertiesFile.getPath());
            conditionsManager.setConnectionProperties(connectionPropertiesFile);
        } 
        if (commandLine.hasOption("x")) {
            File xmlConfigFile = new File(commandLine.getOptionValue("x"));
            conditionsManager.setXmlConfig(xmlConfigFile);
        }         
        
        String detectorName = null;
        if (commandLine.hasOption("d")) {
            detectorName = commandLine.getOptionValue("d");
        } else {
            throw new RuntimeException("Missing -d argument with name of detector.");
        }
        int runNumber = 0;
        if (commandLine.hasOption("r")) {
            runNumber = Integer.parseInt(commandLine.getOptionValue("r"));
        } else {
            throw new RuntimeException("Missing -r argument with run number.");
        }
        try {
            DatabaseConditionsManager.getInstance().setDetector(detectorName, runNumber);
        } catch (ConditionsNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    void printUsage() {
        HelpFormatter help = new HelpFormatter();
        StringBuffer s = new StringBuffer();
        for (String command : commands.keySet()) {
            s.append(command + '\n');
        }
        help.printHelp("CommandLineTool", "Commands:\n" + s.toString(), options, "");
    }

    void exit(int status) {
        System.exit(status);
    }

    void registerCommand(AbstractCommand command) {
        if (commands.containsKey(command.getName())) {
            throw new IllegalArgumentException("There is already a command called " + command.getName());
        }
        commands.put(command.getName(), command);
    }

    static CommandLineTool create() {
        CommandLineTool cli = new CommandLineTool();
        cli.options.addOption(new Option("h", false, "Print help and exit"));
        cli.options.addOption(new Option("d", true, "Set the detector name (required)"));
        cli.options.getOption("d").setRequired(true);
        cli.options.addOption(new Option("r", true, "Set the run number (required)"));
        cli.options.getOption("r").setRequired(true);
        cli.options.addOption(new Option("v", false, "Enable verbose print output"));
        cli.options.addOption(new Option("p", true, "Set the database connection properties file"));
        cli.options.addOption(new Option("x", true, "Set the conditions XML configuration file"));
        cli.registerCommand(new LoadCommand());
        cli.registerCommand(new PrintCommand());
        cli.registerCommand(new AddCommand());
        return cli;
    }
}
