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
 * This class is a command-line tool for performing commands on the conditions database using sub-commands for
 * operations such as 'add' and 'print'.
 * <p>
 * Command line options can be used to supply a custom connection properties file or XML which will override the
 * default.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
// FIXME: Print outs should use conditions manager's log settings and not boolean verbose flag.
public class CommandLineTool {

    /**
     * Setup logging.
     */
    private static final Logger LOGGER =
            LogUtil.create(CommandLineTool.class.getSimpleName(), new DefaultLogFormatter(), Level.WARNING);

    /**
     * The top level options (does not include sub-command options).
     */
    private Options options = new Options();

    /**
     * The map of named command handlers.
     */
    private Map<String, AbstractCommand> commands = new HashMap<String, AbstractCommand>();

    /**
     * The options parser.
     */
    private PosixParser parser = new PosixParser();

    /**
     * The database conditions system manager.
     */
    private DatabaseConditionsManager conditionsManager;

    /**
     * The verbose setting.
     */
    private boolean verbose = false;

    /**
     * The main method for the class.
     *
     * @param arguments The command line arguments.
     */
    public static void main(final String[] arguments) {
        CommandLineTool.create().run(arguments);
    }

    /**
     * Run the command line tool, parsing the command line and sending arguments to sub-command handlers.
     *
     * @param arguments the command line arguments passed directly from {@link #main(String[])}
     */
    private void run(final String[] arguments) {
        try {
            if (arguments.length == 0) {
                printUsage();
                exit(0);
            }

            CommandLine commandLine = null;
            try {
                commandLine = parser.parse(options, arguments, true);
            } catch (ParseException e) {
                LOGGER.log(Level.SEVERE, "error parsing the options", e);
                printUsage();
                exit(1);
            }

            if (commandLine.hasOption("h") || commandLine.getArgs().length == 0) {
                printUsage();
                exit(0);
            }

            // Set verbosity.
            if (commandLine.hasOption("v")) {
                LOGGER.setLevel(Level.ALL);
                LOGGER.getHandlers()[0].setLevel(Level.ALL);
                verbose = true;
                LOGGER.config("verbose mode enabled");
            }

            // Setup conditions manager from command line options.
            setupConditionsManager(commandLine);

            // Get the sub-command to use.
            final String commandName = commandLine.getArgs()[0];
            final AbstractCommand command = commands.get(commandName);
            if (command == null) {
                throw new IllegalArgumentException("Unknown command " + commandName);
            }

            // Copy remaining arguments for sub-command.
            final String[] commandArguments = new String[commandLine.getArgs().length - 1];
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

    /**
     * Setup the conditions system based on command line arguments.
     *
     * @param commandLine the parsed command line arguments
     */
    private void setupConditionsManager(final CommandLine commandLine) {

        LOGGER.info("setting up conditions manager");

        // Create new manager.
        conditionsManager = DatabaseConditionsManager.getInstance();

        // Set log level.
        conditionsManager.setLogLevel(LOGGER.getLevel());

        // Connection properties.
        if (commandLine.hasOption("p")) {
            final File connectionPropertiesFile = new File(commandLine.getOptionValue("p"));
            conditionsManager.setConnectionProperties(connectionPropertiesFile);
            LOGGER.config("connection properties -p " + connectionPropertiesFile);
        }

        // XML config.
        if (commandLine.hasOption("x")) {
            final File xmlConfigFile = new File(commandLine.getOptionValue("x"));
            conditionsManager.setXmlConfig(xmlConfigFile);
            LOGGER.config("XML config -x " + xmlConfigFile);
        }

        // If there is a run number or detector number then attempt to initialize the conditions system.
        if (commandLine.hasOption("r") || commandLine.hasOption("d")) {

            LOGGER.config("detector name or run number supplied so manager will be initialized");

            // Set detector name.
            String detectorName = null;
            if (commandLine.hasOption("d")) {
                detectorName = commandLine.getOptionValue("d");
                LOGGER.config("detector -d " + detectorName);
            } else {
                detectorName = "HPS-ECalCommissioning-v2";
                LOGGER.config("default detector " + detectorName + " is being used");
            }

            // Get run number.
            Integer run = null;
            if (commandLine.hasOption("r")) {
                run = Integer.parseInt(commandLine.getOptionValue("r"));
                LOGGER.config("run -r " + run);
            } else {
                run = 0;
                LOGGER.config("default run number " + run + " is being used");
            }

            // Setup the conditions manager with user detector name and run number.
            try {
                LOGGER.config("initializing conditions manager with detector " + detectorName + " and run " + run);
                DatabaseConditionsManager.getInstance().setDetector(detectorName, run);
                LOGGER.config("conditions manager initialized successfully");
                LOGGER.getHandlers()[0].flush();
            } catch (ConditionsNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Print the usage statement for this tool to the console.
     */
    final void printUsage() {
        final HelpFormatter help = new HelpFormatter();
        final StringBuffer s = new StringBuffer();
        for (String command : commands.keySet()) {
            s.append(command + '\n');
        }
        help.printHelp("CommandLineTool", "Commands:\n" + s.toString(), options, "");
    }

    /**
     * Exit with the given status.
     *
     * @param status the exit status
     */
    private void exit(final int status) {
        System.exit(status);
    }

    /**
     * Register a sub-command handler.
     * @param command the sub-command handler
     */
    private void registerCommand(final AbstractCommand command) {
        if (commands.containsKey(command.getName())) {
            throw new IllegalArgumentException("There is already a command called " + command.getName());
        }
        commands.put(command.getName(), command);
    }

    /**
     * Create a basic instance of this class.
     * @return the instance of this class
     */
    private static CommandLineTool create() {
        final CommandLineTool cli = new CommandLineTool();
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
