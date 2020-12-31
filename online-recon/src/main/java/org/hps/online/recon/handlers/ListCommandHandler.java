package org.hps.online.recon.handlers;

import java.util.List;

import org.hps.online.recon.CommandHandler;
import org.hps.online.recon.CommandResult;
import org.hps.online.recon.CommandResult.GenericResult;
import org.hps.online.recon.CommandResult.Error;
import org.hps.online.recon.Server;
import org.hps.online.recon.StationProcess;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Handle the list command.
 */
class ListCommandHandler extends CommandHandler {

    ListCommandHandler(Server server) {
        super(server);
    }

    public String getCommandName() {
        return "list";
    }

    public CommandResult execute(JSONObject parameters) {
        CommandResult res = null;
        List<Integer> ids = getStationIDs(parameters);
        JSONArray arr = new JSONArray();
        if (ids.size() == 0) {
            // Return info on all stations.
            for (StationProcess station : mgr.getStations()) {
                JSONObject jo = station.toJSON();
                arr.put(jo);
            }
        } else {
            // Return info on selected station IDs.
            for (Integer id : ids) {
                StationProcess station = mgr.find(id);
                if (station == null) {
                    // One of the station IDs is invalid.  Just return message about the first bad one.
                    res = new Error("Station with this ID does not exist: " + id);
                    break;
                } else {
                    JSONObject jo = station.toJSON();
                    arr.put(jo);
                }
            }
        }
        if (res == null) {
            res = new GenericResult(arr);
        }
        return res;
    }
}
