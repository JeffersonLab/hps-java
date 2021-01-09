package org.hps.online.recon.handlers;

import java.util.List;

import org.hps.online.recon.CommandHandler;
import org.hps.online.recon.CommandResult;
import org.hps.online.recon.Server;
import org.hps.online.recon.StationProcess;
import org.hps.online.recon.CommandResult.Error;
import org.hps.online.recon.CommandResult.Success;
import org.json.JSONObject;

/**
 * Handle the <i>stop</i> command.
 */
class StopCommandHandler extends CommandHandler {

    StopCommandHandler(Server server) {
        super(server);
    }

    public CommandResult execute(JSONObject parameters) {
        CommandResult res = null;
        List<Integer> ids = getStationIDs(parameters);
        List<StationProcess> stations = null;
        int active = 0;
        int stopped;
        if (ids.size() == 0) {
            List<StationProcess> activeStations = mgr.getActiveStations();
            active = activeStations.size();
            stations = activeStations;
        } else {
            stations = mgr.find(ids);
        }
        stopped = mgr.stopStations(stations);
        if (ids.size() == 0) {
            if (stopped < active) {
                res = new Error("Failed to stop at least one station.");
            } else {
                res = new Success("Stopped all stations.");
            }
        } else {
            if (stopped < ids.size()) {
                res = new Error("Failed to stop at least one station (may have been inactive).");
            } else {
                res = new Success("Stopped stations: " + ids.toString());
            }
        }
        return res;
    }

    @Override
    public String getCommandName() {
        return "stop";
    }
}
