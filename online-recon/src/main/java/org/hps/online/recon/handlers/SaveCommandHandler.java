package org.hps.online.recon.handlers;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

import org.hps.online.recon.CommandHandler;
import org.hps.online.recon.CommandResult;
import org.hps.online.recon.CommandResult.Success;
import org.hps.online.recon.CommandResult.Error;
import org.hps.online.recon.Server;
import org.json.JSONObject;

/**
 * Handle the <i>save</i> command which writes the contents
 * of the current AIDA tree to a local file.
 */
class SaveCommandHandler extends CommandHandler {

    SaveCommandHandler(Server server) {
        super(server);
    }

    public String getCommandName() {
        return "save";
    }

    public CommandResult execute(JSONObject parameters) throws CommandException {
        if (!parameters.has("filename")) {
            throw new CommandException("Missing required parameter: filename");
        }
        String filename = parameters.getString("filename");
        server.getLogger().info("Saving AIDA plots to: " + filename);
        try {
            server.save(new File(filename));
            return new Success("Saved plots to: " + filename);
        } catch (IOException ioe) {
            server.getLogger().log(Level.SEVERE, "Failed to save plots to: " + filename, ioe);
            return new Error("Failed to save plots to: " + filename);
        }
    }
}
