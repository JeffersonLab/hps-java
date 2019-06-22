package org.hps.online.recon.commands;

import org.apache.commons.cli.CommandLine;
import org.hps.online.recon.Command;

/**
 * Cleanup a station by deleting its working directory.
 * 
 * This will only work on inactive stations.
 */
class CleanupCommand extends Command {
    
    CleanupCommand() {            
        super("cleanup", "Delete a station's working directory and files", "[IDs]",
                "Provide a list of IDs or none to cleanup all");
    }
            
    protected void process(CommandLine cl) {
        readStationIDs(cl);
    }
}
