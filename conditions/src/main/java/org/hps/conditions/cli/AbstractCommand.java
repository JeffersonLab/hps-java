package org.hps.conditions.cli;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.Parser;
import org.apache.commons.cli.PosixParser;

/**
 * This is the API that sub-commands such as 'load' or 'print' must implement
 * in the conditions command line interface.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
abstract class AbstractCommand {

    /**
     * The name of the (sub)command.
     */
    private String name;

    /**
     * The description of the command.
     */
    private String description;

    /**
     * The options this command takes on the command line (Apache CLI).
     */
    private final Options options;

    /**
     * The parser for the options.
     */
    private final Parser parser = new PosixParser();

    /**
     * Verbose setting.
     */
    private boolean verbose = false;

    /**
     * Class constructor.
     * @param name The string that invokes this command.
     * @param description The description of this command.
     * @param options The command's options (Apache CLI).
     */
    AbstractCommand(final String name, final String description, final Options options) {
        this.name = name;
        this.description = description;
        this.options = options;
    }

    /**
     * Get the name of this command.
     * @return A String of the name of this command.
     */
    final String getName() {
        return this.name;
    }

    /**
     * Get the description of this command.
     * @return A String of the description of this command.
     */
    protected final String getDescription() {
        return this.description;
    }

    /**
     * Options for this command.
     * @return Options object for this command.
     */
    protected final Options getOptions() {
        return options;
    }

    /**
     * Print the usage of this sub-command.
     */
    protected final void printUsage() {
        final HelpFormatter help = new HelpFormatter();
        help.printHelp(getName(), getOptions());
    }

    /**
     * Set whether verbose output is enabled.
     * @param verbose True to enable verbose output.
     */
    final void setVerbose(final boolean verbose) {
        this.verbose = verbose;
    }

    /**
     * Get verbose flag.
     * @return The verbose flag.
     */
    protected boolean getVerbose() {
        return verbose;
    }

    /**
     * Parse the sub-command's options.
     * @param arguments The sub-command's arguments.
     * @return The parsed command line.
     */
    protected final CommandLine parse(final String[] arguments) {
        CommandLine commandLine = null;
        try {
            commandLine = parser.parse(options, arguments);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        if (commandLine.hasOption("h")) {
            this.printUsage();
            System.exit(0);
        }
        return commandLine;
    }

    /**
     * The sub-command execution method that should be implemented by sub-classes.
     * @param arguments The command's argument list.
     */
    abstract void execute(final String[] arguments);
}
