package org.hps.online.recon.commands;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.hps.online.recon.Command;

/**
 * Show status summary of online reconstruction server and ET system.
 */
public class StatusCommand extends Command {
           
    StatusCommand() {
        super("status", "Show server and station status", "[options]", "");
    }
    
    protected Options getOptions() {
        options.addOption(new Option("v", "verbose", false, "show verbose station info"));
        return options;
    }
    
    void setVerbose(boolean verbose) {
        setParameter("verbose", verbose);
    }
    
    public void process(CommandLine cl) {
        if (cl.hasOption("v")) {
            setVerbose(true);
        } else {
            setVerbose(false);
        }
    }
}    
