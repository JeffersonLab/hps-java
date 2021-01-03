package org.hps.online.recon.handlers;

import org.hps.online.recon.CommandHandler;
import org.hps.online.recon.CommandResult;
import org.hps.online.recon.CommandResult.JSONResult;
import org.hps.online.recon.CommandResult.Success;
import org.hps.online.recon.Server;
import org.json.JSONObject;

class ConfigCommandHandler extends CommandHandler {

    ConfigCommandHandler(Server server) {
        super(server);
    }

    public CommandResult execute(JSONObject parameters) {
        CommandResult res = null;
        if (parameters.length() == 0) {
            logger.info("Returning existing station config.");
            res = new JSONResult(server.getStationProperties().toJSON());
        } else {
            logger.config("Loading new station config: " + parameters.toString());
            server.getStationProperties().fromJSON(parameters);
            logger.info("New config loaded.");
            res = new Success("Loaded new station config. Create a new station to use it.");
        }
        return res;
    }

    @Override
    public String getCommandName() {
        return "config";
    }
}