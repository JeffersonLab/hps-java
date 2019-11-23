package org.hps.online.recon.commands;

import org.apache.commons.cli.CommandLine;
import org.hps.online.recon.Command;

/**
 * Get a list of station info as JSON from a list of IDs or
 * if none are given then return info for all stations.
 */
public class ListCommand extends Command {
    
    ListCommand() {
        super("list", "List station information in JSON format", "[IDs]",
                "Provide a list of IDs or none to list information for all");
    }
            
    protected void process(CommandLine cl) {
        readStationIDs(cl);
    }       
}
