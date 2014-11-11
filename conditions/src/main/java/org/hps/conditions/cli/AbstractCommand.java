package org.hps.conditions.cli;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.Parser;
import org.apache.commons.cli.PosixParser;

/**
 * This is the API that sub-commands must implement in the conditions command
 * line interface.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
abstract class AbstractCommand {

    String name;
    String description;
    Options options = new Options();
    Parser parser = new PosixParser();
    CommandLine commandLine;
    boolean verbose = false;

    /**
     * Class constructor.
     * @param name The string that invokes this command.
     * @param description The description of this command.
     */
    AbstractCommand(String name, String description) {
        this.name = name;
        this.description = description;
        options.addOption("h", false, "Print help for this command");
    }

    /**
     * Get the name of this command.
     * @return A String of the name of this command.
     */
    String getName() {
        return this.name;
    }

    /**
     * Get the description of this command.
     * @return A String of the description of this command.
     */
    String getDescription() {
        return this.description;
    }

    /**
     * Options for this command.
     * @return Options object for this command.
     */
    Options getOptions() {
        return options;
    }

    /**
     * Print the usage of this sub-command.
     * @param doExit Whether or not to exit after printing usage.
     */
    void printUsage() {
        HelpFormatter help = new HelpFormatter();
        help.printHelp(getName(), getOptions());
    }

    /**
     * Set whether verbose output is enabled.
     * @param verbose True to enable verbose output.
     */
    void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    /**
     * Execute the command with the arguments. This is the only method that
     * sub-classes must implement.
     * @param arguments The sub-command's arguments.
     */
    void execute(String[] arguments) {
        try {
            commandLine = parser.parse(options, arguments);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        if (commandLine.hasOption("h")) {
            this.printUsage();
            System.exit(0);
        }
    }
}
