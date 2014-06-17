package org.hps.monitoring;

import java.io.File;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.hps.monitoring.gui.MonitoringApplication;

public class MonitoringApplicationMain {

    /**
     * Run the monitoring application from the command line.
     * @param args The command line arguments.
     */
    public static void main(String[] args) {

        // Set up command line parsing.
        Options options = new Options();
        options.addOption(new Option("h", false, "Print help."));
        options.addOption(new Option("c", true, "Load properties file with connection settings."));
        options.addOption(new Option("j", true, "Load properties file with job settings."));
        CommandLineParser parser = new PosixParser();

        // Parse command line arguments.
        CommandLine cl = null;
        try {
            cl = parser.parse(options, args);
        } catch (ParseException e) {
            throw new RuntimeException("Problem parsing command line options.", e);
        }

        // Print help and exit.
        if (cl.hasOption("h")) {
            System.out.println("MonitoringApplication [options]");
            HelpFormatter help = new HelpFormatter();
            help.printHelp(" ", options);
            System.exit(1);
        }

        // Create the application class.
        MonitoringApplication app = new MonitoringApplication();

        // Load the connection settings.
        if (cl.hasOption("c")) {
            app.loadConnectionSettings(new File(cl.getOptionValue("c")));
        }

        // Load the job settings.
        if (cl.hasOption("j")) {
            app.loadJobSettings(new File(cl.getOptionValue("j")));
        }
        
        app.setVisible(true);
    }
    
}
