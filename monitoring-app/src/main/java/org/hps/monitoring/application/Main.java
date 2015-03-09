package org.hps.monitoring.application;

import java.io.File;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.hps.monitoring.application.model.Configuration;

/**
 * This is the front-end for running the monitoring app via a {@link #main(String[])} method.
 */
public final class Main {

    private Main() {
    }
    
    public static void main(String[] args) {
        
        // Set up command line parsing.
        Options options = new Options();
        options.addOption(new Option("h", false, "Print help."));
        options.addOption(new Option("c", true, "Load a properties file with configuration parameters."));
        CommandLineParser parser = new PosixParser();
        
        // Parse command line arguments.
        final CommandLine cl;
        try {
            cl = parser.parse(options, args);
        } catch (ParseException e) {
            throw new RuntimeException("Problem parsing command line options.", e);
        }

        // Print help and exit.
        if (cl.hasOption("h")) {
            HelpFormatter help = new HelpFormatter();
            help.printHelp(" ", options);
            System.exit(1);
        }
        
        // Load the connection settings.
        Configuration configuration = null;
        if (cl.hasOption("c")) {
            configuration = new Configuration(new File(cl.getOptionValue("c")));
        }        
        
        MonitoringApplication.create(configuration);
    }    
}