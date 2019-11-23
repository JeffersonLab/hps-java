package org.hps.online.recon.commands;

import org.apache.commons.cli.CommandLine;
import org.hps.online.recon.Command;

/**
 * Stop and cancel all server-side plot tasks.
 * 
 * @author jeremym
 */
public class PlotStopCommand extends Command {

    PlotStopCommand() {
        super("plot-stop", "Stop and cancel all server side plot tasks", "", 
                "You will need to run plot-add again to reschedule plot tasks.");
    }
    
    @Override
    protected void process(CommandLine cl) {
        // No options.
    }
}
