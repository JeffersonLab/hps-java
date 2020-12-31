package org.hps.online.recon;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

public abstract class CommandHandler {

    protected Server server = null;
    protected StationManager mgr = null;
    protected Logger logger = null;

    /**
     * Connection that handlers can throw if there is a command
     * processing error
     */
    @SuppressWarnings("serial")
    public static class CommandException extends Exception {

        public CommandException(String msg) {
            super(msg);
        }

        public CommandException(String msg, Exception e) {
            super(msg, e);
        }
    }

    protected CommandHandler(Server server) {
        this.server = server;
        this.mgr = this.server.getStationManager();
        this.logger = this.server.getLogger();
    }

    public static List<Integer> getStationIDs(JSONObject parameters) {
        List<Integer> ids = new ArrayList<Integer>();
        if (parameters.has("ids")) {
            JSONArray arr = parameters.getJSONArray("ids");
            for (int i = 0; i < arr.length(); i++) {
                ids.add(arr.getInt(i));
            }
        }
        return ids;
    }

    public abstract String getCommandName();

    /**
     * Execute the command.
     * @param jo The JSON input parameters
     * @return The command result
     */
    public abstract CommandResult execute(JSONObject jo) throws CommandException;

}
