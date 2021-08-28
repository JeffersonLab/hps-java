package org.hps.online.recon.handlers;

import org.hps.online.recon.CommandHandler;
import org.hps.online.recon.CommandResult;
import org.hps.online.recon.CommandResult.Success;
import org.hps.online.recon.Server;
import org.hps.online.recon.StationProperties;
import org.json.JSONObject;

/**
 * Handle the <i>set</i> command
 */
class SetCommandHandler extends CommandHandler {

    SetCommandHandler(Server server) {
        super(server);
    }

    public CommandResult execute(JSONObject parameters) throws CommandException {
        String name = parameters.getString("name");
        String value = parameters.getString("value");
        StationProperties statProp = server.getStationProperties();
        if (statProp.has(name)) {
            statProp.get(name).from(value);
            logger.info("Set prop: " + name + "=" + value);
        } else {
            throw new CommandException("Property does not exist: " + name);
        }
        return new Success("Set prop: " + name + "=" + value);
    }

    @Override
    public String getCommandName() {
        return "set";
    }
}

