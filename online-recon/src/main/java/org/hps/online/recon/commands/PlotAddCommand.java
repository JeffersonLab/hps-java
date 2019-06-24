package org.hps.online.recon.commands;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.hps.online.recon.Command;

public class PlotAddCommand extends Command {
    
    PlotAddCommand() {
        super("plot-add", "Manage plot files from stations", "[options] [IDs]", "");
    }
    
    protected Options getOptions() {
        options.addOption(new Option("d", "delete", false, "delete intermediate plot files when finished adding"));
        options.addOption(new Option("t", "target", true, "target output file for combined plots"));
        options.getOption("t").setRequired(true);
        return options;
    }
    
    @Override
    protected void process(CommandLine cl) {
        if (cl.hasOption("t")) {
            setParameter("target", cl.getOptionValue("t"));
        }
        if (cl.hasOption("d")) {
            setParameter("delete", true);
        } else {
            setParameter("delete", false);
        }
        this.readStationIDs(cl);
    }
}
