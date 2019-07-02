package org.hps.online.recon.commands;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.hps.online.recon.Command;

/**
 * Tail the log file of a station.
 * 
 * @author jeremym
 */
public class LogCommand extends Command {

    LogCommand() {
        super("log", "Tail log file of station", "[ID]", "Provide a single station ID.");
    }
    
    protected Options getOptions() {
        this.options.addOption(new Option("d", "delay", true, "delay in millis between reading log files"));
        return this.options;
    }

    @Override
    protected void process(CommandLine cl) {
        if (cl.getArgList().size() != 1) {
            throw new IllegalArgumentException("Must provide a single station ID as argument.");
        }
        if (cl.hasOption("d")) {
            this.setParameter("delayMillis", Long.parseLong(cl.getOptionValue("d")));
        } 
        this.readStationIDs(cl);
    }
}
