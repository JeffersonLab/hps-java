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

/**
 * This class is a command-line tool for performing commands on the conditions database using sub-commands for
 * operations such as 'add' and 'print'.
 * <p>
 * Command line options can be used to supply a custom connection properties file or XML which will override the
 * default.
 *
 * @author Jeremy McCormick, SLAC
 */
public final class CommandLineTool {

    /**
     * Initialize the logger.
     */
    private static final Logger LOGGER = Logger.getLogger(CommandLineTool.class.getPackage().getName());

    private static Options OPTIONS = new Options();

    static {
        OPTIONS.addOption(new Option("h", false, "print help"));
        OPTIONS.addOption(new Option("d", true, "detector name"));
        OPTIONS.addOption(new Option("r", true, "run number"));
        OPTIONS.addOption(new Option("p", true, "database connection properties file"));
        OPTIONS.addOption(new Option("x", true, "conditions XML configuration file"));
        OPTIONS.addOption(new Option("t", true, "conditions tag to use for filtering records"));
        OPTIONS.addOption(new Option("l", true, "log level of the conditions manager (INFO, FINE, etc.)"));
    }

    /**
     * Create a basic instance of this class.
     *
     * @return the instance of this class
     */
    private static CommandLineTool create() {
        final CommandLineTool cli = new CommandLineTool();
        cli.registerCommand(new LoadCommand());
        cli.registerCommand(new PrintCommand());
        cli.registerCommand(new AddCommand());
        cli.registerCommand(new TagCommand());
        cli.registerCommand(new RunSummaryCommand());
        return cli;
    }

    /**
     * The main method for the class.
     *
     * @param arguments The command line arguments.
     */
    public static void main(final String[] arguments) {
        CommandLineTool.create().run(arguments);
    }

    /**
     * The map of named command handlers.
     */
    private final Map<String, AbstractCommand> commands = new HashMap<String, AbstractCommand>();

    /**
     * The database conditions system manager.
     */
    private DatabaseConditionsManager conditionsManager;

    /**
     * The options parser.
     */
    private final PosixParser parser = new PosixParser();

    /**
     * Exit with the given status.
     *
     * @param status the exit status
     */
    private void exit(final int status) {
        System.exit(status);
    }

    /**
     * Print the usage statement for this tool to the console.
     */
    final void printUsage() {
        final HelpFormatter help = new HelpFormatter();
        final StringBuffer s = new StringBuffer();
        for (final String command : this.commands.keySet()) {
            s.append(command + '\n');
        }
        help.printHelp("CommandLineTool", "Commands:\n" + s.toString(), OPTIONS, "");
    }

    /**
     * Register a sub-command handler.
     *
     * @param command the sub-command handler
     */
    private void registerCommand(final AbstractCommand command) {
        if (this.commands.containsKey(command.getName())) {
            throw new IllegalArgumentException("There is already a command called " + command.getName());
        }
        this.commands.put(command.getName(), command);
    }

    /**
     * Run the command line tool, parsing the command line and sending arguments to sub-command handlers.
     *
     * @param arguments the command line arguments passed directly from {@link #main(String[])}
     */
    private void run(final String[] arguments) {
        try {
            if (arguments.length == 0) {
                this.printUsage();
                this.exit(0);
            }

            CommandLine commandLine = null;
            try {
                commandLine = this.parser.parse(OPTIONS, arguments, true);
            } catch (final ParseException e) {
                LOGGER.log(Level.SEVERE, "Error parsing the options.", e);
                this.printUsage();
                this.exit(1);
            }

            if (commandLine.hasOption("h") || commandLine.getArgs().length == 0) {
                this.printUsage();
                this.exit(0);
            }

            // Setup conditions manager from command line options.
            this.setupConditionsManager(commandLine);

            // Get the sub-command to use.
            final String commandName = commandLine.getArgs()[0];
            final AbstractCommand command = this.commands.get(commandName);
            if (command == null) {
                throw new IllegalArgumentException("Unknown command " + commandName);
            }

            // Copy remaining arguments for sub-command.
            final String[] commandArguments = new String[commandLine.getArgs().length - 1];
            System.arraycopy(commandLine.getArgs(), 1, commandArguments, 0, commandArguments.length);

            // Execute the sub-command.
            command.execute(commandArguments);
        } catch (final Exception e) {
            e.printStackTrace();
            System.exit(1);
        } finally {
            this.conditionsManager.closeConnection();
        }
    }

    /**
     * Setup the conditions system based on command line arguments.
     *
     * @param commandLine the parsed command line arguments
     */
    private void setupConditionsManager(final CommandLine commandLine) {

        LOGGER.info("Setting up the conditions manager ...");

        // Create new manager.
        this.conditionsManager = DatabaseConditionsManager.getInstance();

        // Set the conditions manager log level (does not affect logger of this class or sub-commands).
        if (commandLine.hasOption("l")) {
            final Level newLevel = Level.parse(commandLine.getOptionValue("l"));
            Logger.getLogger(DatabaseConditionsManager.class.getPackage().getName()).setLevel(newLevel);
            LOGGER.config("conditions manager log level will be set to " + newLevel.toString());
        }

        // Connection properties.
        if (commandLine.hasOption("p")) {
            final File connectionPropertiesFile = new File(commandLine.getOptionValue("p"));
            this.conditionsManager.setConnectionProperties(connectionPropertiesFile);
            LOGGER.config("using connection properties " + connectionPropertiesFile.getPath());
        }

        // Conditions system XML configuration file.
        if (commandLine.hasOption("x")) {
            final File xmlConfigFile = new File(commandLine.getOptionValue("x"));
            this.conditionsManager.setXmlConfig(xmlConfigFile);
            LOGGER.config("using XML config " + xmlConfigFile.getPath());
        }

        // User specified tag of conditions records.
        if (commandLine.hasOption("t")) {
            final String tag = commandLine.getOptionValue("t");
            this.conditionsManager.addTag(tag);
            LOGGER.config("using tag " + tag);
        }

        // If there is a run number or detector number then attempt to initialize the conditions system.
        if (commandLine.hasOption("r") || commandLine.hasOption("d")) {

            if (!commandLine.hasOption("r")) {
                // Missing run number.
                throw new RuntimeException(
                        "Missing -r option with run number which must be given when detector name is used.");
            }

            if (!commandLine.hasOption("d")) {
                // Missing detector name.
                throw new RuntimeException(
                        "Missing -d option with detector name which must be given when run number is used.");
            }

            // Set detector name.
            String detectorName = null;
            if (commandLine.hasOption("d")) {
                detectorName = commandLine.getOptionValue("d");
                LOGGER.config("using detector " + detectorName);
            } else {
                detectorName = "HPS-ECalCommissioning-v2";
            }

            // Get run number.
            Integer run = null;
            if (commandLine.hasOption("r")) {
                run = Integer.parseInt(commandLine.getOptionValue("r"));
                LOGGER.config("using run " + run);
            } else {
                run = 0;
            }

            // Setup the conditions manager with user detector name and run number.
            try {
                LOGGER.config("Initializing conditions manager with detector " + detectorName + " and run " + run
                        + " ...");
                DatabaseConditionsManager.getInstance().setDetector(detectorName, run);
                LOGGER.config("Conditions manager was initialized successfully!");
                LOGGER.getHandlers()[0].flush();
            } catch (final ConditionsNotFoundException e) {
                throw new RuntimeException("Error setting up the conditions manager.", e);
            }
        }
        LOGGER.info("Done setting up the conditions manager!");
    }
}
