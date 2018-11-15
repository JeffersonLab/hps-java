package org.hps.conditions.cli;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.hps.conditions.database.DatabaseConditionsManager;

/**
 * This is the API that sub-commands such as 'load' or 'print' must implement in the conditions command line interface.
 *
 * @author Jeremy McCormick, SLAC
 */
abstract class AbstractCommand {

    /**
     * The description of the command.
     */
    private final String description;

    /**
     * The name of the (sub)command.
     */
    private final String name;

    /**
     * The options this command takes on the command line (Apache CLI).
     */
    private final Options options;

    /**
     * The parser for the options.
     */
    private final PosixParser parser = new PosixParser();
    
    private DatabaseConditionsManager manager;

    /**
     * Class constructor.
     *
     * @param name the string that invokes this command
     * @param description the description of this command
     * @param options the command's options (Apache CLI)
     */
    AbstractCommand(final String name, final String description, final Options options) {
        this.name = name;
        this.description = description;
        this.options = options;
    }

    /**
     * The sub-command execution method that should be implemented by sub-classes.
     *
     * @param arguments The command's argument list.
     */
    abstract void execute(final String[] arguments);

    /**
     * Get the description of this command.
     *
     * @return the description of this command
     */
    protected final String getDescription() {
        return this.description;
    }

    /**
     * Get the name of this command.
     *
     * @return the name of this command
     */
    final String getName() {
        return this.name;
    }

    /**
     * Get the <code>Options</code> for this command (Apache CLI).
     *
     * @return the <code>Options</code> object for this command
     */
    protected final Options getOptions() {
        return this.options;
    }

    /**
     * Parse the sub-command's options.
     *
     * @param arguments the sub-command's arguments
     * @return the parsed command line
     */
    protected final CommandLine parse(final String[] arguments) {
        CommandLine commandLine = null;
        try {
            commandLine = this.parser.parse(this.options, arguments);
        } catch (final ParseException e) {
            throw new RuntimeException(e);
        }
        if (commandLine.hasOption("h")) {
            this.printUsage();
            System.exit(0);
        }
        return commandLine;
    }

    /**
     * Print the usage of this sub-command.
     */
    protected final void printUsage() {
        final HelpFormatter help = new HelpFormatter();
        help.printHelp(this.getName(), this.getOptions());
    }
    
    /**
     * Convenience method for getting the conditions manager.
     * @return the conditions manager
     */
    public DatabaseConditionsManager getManager() {
        if (manager == null) {
            manager = DatabaseConditionsManager.getInstance();
        }
        return manager;
    }
    
    public void cleanup() {
        manager.closeConnection();
        manager = null;
    }
}
