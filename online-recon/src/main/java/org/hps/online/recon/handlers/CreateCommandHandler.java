package org.hps.online.recon.handlers;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.online.recon.CommandHandler;
import org.hps.online.recon.CommandResult;
import org.hps.online.recon.CommandResult.CommandStatus;
import org.hps.online.recon.CommandResult.Error;
import org.hps.online.recon.CommandResult.Success;
import org.hps.online.recon.Server;
import org.hps.online.recon.StationProcess;
import org.json.JSONObject;

/**
 * Handle the create command.
 */
class CreateCommandHandler extends CommandHandler {

    static final Logger LOG = Logger.getLogger(CreateCommandHandler.class.getPackage().getName());

    CreateCommandHandler(Server server) {
        super(server);
    }

    public String getCommandName() {
        return "create";
    }

    public CommandResult execute(JSONObject parameters) throws CommandException {

        CommandStatus res = null;
        int count = 1;

        if (parameters.has("count")) {
            count = parameters.getInt("count");
        }
        LOG.info("Creating stations: " + count);

        // Create the stations
        List<StationProcess> stats = new ArrayList<StationProcess>();
        for (int i = 0; i < count; i++) {
            try {
                LOG.info("Creating station " + (i + 1) + " of " + count);
                StationProcess station = mgr.create(parameters);
                stats.add(station);
                LOG.info("Created station: " + station.getStationName());
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Error creating station: " + i, e);
            }
        }
        LOG.info("Number of stations created: " + stats.size());

        boolean start = false;
        if (parameters.has("start")) {
            start = parameters.getBoolean("start");
            LOG.info("Stations will be automatically started: " + start);
        }
        if (start) {
            // Start the stations
            int started = mgr.startStations(stats);
            LOG.info("Started " + started + " stations.");

            // Return command status
            if (started < count) {
                res = new Error("Failed to create and start some stations.");
            } else {
                res = new Success("Created and started all stations successfully.");
            }
        } else {
            // Return command status
            if (stats.size() == count) {
                res = new Success("Created all stations successfully.");
            } else {
                res = new Error("Could not create some stations.");
            }
        }
        stats = null;
        return res;
    }
}