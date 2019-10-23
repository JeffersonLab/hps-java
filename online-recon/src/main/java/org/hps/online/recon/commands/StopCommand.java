package org.hps.online.recon.commands;

import org.apache.commons.cli.CommandLine;
import org.hps.online.recon.Command;

/**
 * Stop a list of stations by their IDs or if none are given
 * then stop all stations.
 */
public class StopCommand extends Command {
    
    StopCommand() {
        super("stop", "Stop a station", "[IDs]", 
                "Provide a list of IDs or none for all.");
    }
        
    protected void process(CommandLine cl) {
        readStationIDs(cl);
    }        
}
