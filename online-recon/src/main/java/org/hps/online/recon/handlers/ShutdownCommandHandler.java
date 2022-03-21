package org.hps.online.recon.handlers;

import org.hps.online.recon.CommandHandler;
import org.hps.online.recon.CommandResult;
import org.hps.online.recon.Server;
import org.json.JSONObject;

/**
 * Handle the <i>shutdown</i> command which shuts down a running server instance
 */
class ShutdownCommandHandler extends CommandHandler {

    ShutdownCommandHandler(Server server) {
        super(server);
    }

    public CommandResult execute(JSONObject parameters) {
        return new CommandResult.Shutdown(parameters.getInt("wait"));
    }

    @Override
    public String getCommandName() {
        return "shutdown";
    }
}