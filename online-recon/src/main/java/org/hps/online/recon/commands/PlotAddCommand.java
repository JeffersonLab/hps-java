package org.hps.online.recon.commands;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.hps.online.recon.Command;

/**
 * Add plots produced by one or more stations.
 * 
 * @author jeremym
 */
public class PlotAddCommand extends Command {
    
    PlotAddCommand() {
        super("plot-add", "Manage plot files from stations", "[options] [IDs]", 
                "Set the -p option to specify a time in seconds for running the task periodically." + '\n' +
                    "By default the plots will just be added once and the task will not reoccur." + '\n' +
                    "If no IDs are provided then plots from all active stations will be added."
        );
    }
    
    protected Options getOptions() {
        options.addOption(new Option("T", "threads", true, "number of CPU threads to use for hadd command"));
        options.addOption(new Option("a", "append", false, "append to existing target if exists"));
        options.addOption(new Option("d", "delete", false, "delete intermediate plot files when finished adding"));
        options.addOption(new Option("t", "target", true, "target output file for combined plots"));
        options.getOption("t").setRequired(true);
        options.addOption(new Option("p", "period", true, "time in seconds for running task periodically"));
        options.addOption(new Option("v", "verbosity", true, "verbosity of the hadd command (0-99)"));
        return options;
    }
    
    @Override
    protected void process(CommandLine cl) {
        if (cl.hasOption("t")) {
            setParameter("target", cl.getOptionValue("t"));
        }
        if (cl.hasOption("d")) {
            setParameter("delete", true);
        }
        if (cl.hasOption("a")) {
            setParameter("append", true);
        }
        if (cl.hasOption("T")) {
            setParameter("threads", Integer.parseInt(cl.getOptionValue("T")));
        }
        if (cl.hasOption("v")) {
            setParameter("verbosity", Integer.parseInt(cl.getOptionValue("v")));
        }
        if (cl.hasOption("p")) {
            setParameter("period", Long.parseLong(cl.getOptionValue("p")));
        }
        this.readStationIDs(cl);
    }
}
