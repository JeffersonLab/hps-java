package org.hps.online.recon.handlers;

import java.util.Arrays;
import java.util.List;

import org.hps.online.recon.CommandHandler;
import org.hps.online.recon.CommandResult;
import org.hps.online.recon.CommandResult.Error;
import org.hps.online.recon.CommandResult.Success;
import org.hps.online.recon.Server;
import org.hps.online.recon.StationProcess;
import org.json.JSONObject;

/**
 * Handle the <i>start</i> command.
 */
class StartCommandHandler extends CommandHandler {

    StartCommandHandler(Server server) {
        super(server);
    }

    public CommandResult execute(JSONObject parameters) {
        CommandResult res = null;
        logger.info("Starting stations...");
        List<Integer> ids = getStationIDs(parameters);
        List<StationProcess> stations = null;
        if (ids.size() == 0) {
            logger.info("All inactive stations will be started");
            stations = mgr.getInactiveStations();
        } else {
            stations = mgr.find(ids);
            logger.info("Starting station IDs: " + Arrays.toString(ids.toArray()));
        }
        int count = stations.size();
        int started = mgr.startStations(stations);

        if (started < count) {
            res = new Error("Failed to start some stations.");
        } else {
            res = new Success("Started " + count + " stations successfully.");
        }
        logger.info("Done starting stations");
        return res;
    }

    @Override
    public String getCommandName() {
        return "start";
    }
}
