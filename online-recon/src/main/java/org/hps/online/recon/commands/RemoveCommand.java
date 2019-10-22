package org.hps.online.recon.commands;

import org.apache.commons.cli.CommandLine;
import org.hps.online.recon.Command;

/**
 * Remove a list of stations by their IDs or if none are
 * given then try to remove all stations.
 * 
 * This will only work on inactive stations.
 */
public class RemoveCommand extends Command {
             
    RemoveCommand() {            
        super("remove", "Remove a station that is inactive", "[IDs]",
                "Provide a list of IDs or none to remove all");
    }
            
    protected void process(CommandLine cl) {
        readStationIDs(cl);
    }        
}
