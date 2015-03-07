package org.hps.conditions.cli;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.lcsim.conditions.ConditionsManager.ConditionsNotFoundException;
import org.lcsim.util.log.DefaultLogFormatter;
import org.lcsim.util.log.LogUtil;

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
    
    static Logger logger = LogUtil.create(CommandLineTool.class.getSimpleName(), new DefaultLogFormatter(), Level.WARNING);

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
                logger.log(Level.SEVERE, "error parsing the options", e);
                printUsage();
                exit(1);
            }

            if (commandLine.hasOption("h") || commandLine.getArgs().length == 0) {
                printUsage();
                exit(0);
            }

            // Set verbosity.
            if (commandLine.hasOption("v")) {
                logger.setLevel(Level.ALL);
                logger.getHandlers()[0].setLevel(Level.ALL);
                verbose = true;
                logger.config("verbose mode enabled");
            }

            // Setup conditions manager from command line options.
            setupConditionsManager(commandLine);

            // Get the sub-command to use.
            String commandName = commandLine.getArgs()[0];
            AbstractCommand command = commands.get(commandName);
            if (command == null) {
                throw new IllegalArgumentException("Unknown command " + commandName);
            }

            // Copy remaining arguments for sub-command.
            String[] commandArguments = new String[commandLine.getArgs().length - 1];
            System.arraycopy(commandLine.getArgs(), 1, commandArguments, 0, commandArguments.length);

            // Excecute the sub-command.
            command.setVerbose(verbose);
            command.execute(commandArguments);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        } finally {
            conditionsManager.closeConnection();
        }
    }

    void setupConditionsManager(CommandLine commandLine) {

        logger.info("setting up conditions manager");
        
        // Create new manager.
        conditionsManager = new DatabaseConditionsManager();

        // Set log level.
        conditionsManager.setLogLevel(logger.getLevel());

        // Connection properties.
        if (commandLine.hasOption("p")) {
            File connectionPropertiesFile = new File(commandLine.getOptionValue("p"));
            conditionsManager.setConnectionProperties(connectionPropertiesFile);
            logger.config("connection properties -p " + connectionPropertiesFile);
        } 
        
        // XML config.
        if (commandLine.hasOption("x")) {
            File xmlConfigFile = new File(commandLine.getOptionValue("x"));
            conditionsManager.setXmlConfig(xmlConfigFile);
            logger.config("XML config -x " + xmlConfigFile);
        }
        
        // If there is a run number or detector number then attempt to initialize the conditions system.
        if (commandLine.hasOption("r") || commandLine.hasOption("d")) {
            
            logger.config("detector name or run number supplied so manager will be initialized");

            // Set detector name.
            String detectorName = null;
            if (commandLine.hasOption("d")) {
                detectorName = commandLine.getOptionValue("d");
                logger.config("detector -d " + detectorName);
            } else {
                detectorName = "HPS-ECalCommissioning-v2";
                logger.config("default detector " + detectorName + " is being used");
            }

            // Get run number.
            Integer run = null;
            if (commandLine.hasOption("r")) {
                run = Integer.parseInt(commandLine.getOptionValue("r"));
                logger.config("run -r " + run);
            } else {
                run = 0;
                logger.config("default run number " + run + " is being used");
            }

            // Setup the conditions manager with user detector name and run number.
            try {
                logger.config("initializing conditions manager with detector " + detectorName + " and run " + run);
                DatabaseConditionsManager.getInstance().setDetector(detectorName, run);
                logger.config("conditions manager initialized successfully");
                logger.getHandlers()[0].flush();
            } catch (ConditionsNotFoundException e) {
                throw new RuntimeException(e);
            }
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
        cli.options.addOption(new Option("d", true, "Set the detector name"));
        cli.options.addOption(new Option("r", true, "Set the run number"));
        cli.options.addOption(new Option("v", false, "Enable verbose print output"));
        cli.options.addOption(new Option("p", true, "Set the database connection properties file"));
        cli.options.addOption(new Option("x", true, "Set the conditions XML configuration file"));
        cli.registerCommand(new LoadCommand());
        cli.registerCommand(new PrintCommand());
        cli.registerCommand(new AddCommand());
        return cli;
    }
}