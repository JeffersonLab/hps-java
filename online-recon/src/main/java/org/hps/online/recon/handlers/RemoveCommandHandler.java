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
 * Handle the remove command.
 */
class RemoveCommandHandler extends CommandHandler {

    RemoveCommandHandler(Server server) {
        super(server);
    }

    public CommandResult execute(JSONObject parameters) {
        CommandResult res = null;
        List<Integer> ids = getStationIDs(parameters);
        int removed = 0;
        List<StationProcess> stations = null;
        if (ids.size() == 0) {
            stations = mgr.getInactiveStations();
        } else {
            stations = mgr.find(ids);
        }
        removed = mgr.remove(stations);
        if (ids.size() == 0) {
            if (mgr.getStationCount() > 0) {
                res = new Error("Failed to remove at least one station (active stations must be stopped first).");
            } else {
                res = new Success("Removed all stations.");
            }
        } else {
            if (removed < ids.size()) {
                res = new Error("Failed to remove at least one station.");
            } else {
                res = new Success("Removed stations: " + ids.toString());
            }
        }
        if (removed == 0) {
            res = new Error("No stations were removed.");
        }
        return res;
    }

    @Override
    public String getCommandName() {
        return "remove";
    }
}
