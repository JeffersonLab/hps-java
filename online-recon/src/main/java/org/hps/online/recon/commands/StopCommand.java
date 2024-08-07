package org.hps.online.recon.commands;

import org.hps.online.recon.Command;

/**
 * Stop a list of stations by their IDs or if none are given
 * then stop all stations.
 */
public class StopCommand extends Command {

    public StopCommand() {
        super("stop", "Stop a station", "[IDs]",
                "Provide a list of IDs or none for all.");
    }

    protected void process() {
        readStationIDs(cl);
    }
}
