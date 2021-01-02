package org.hps.online.recon;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Server-side handler for a {@link Command} sent by the {@link Client}
 *
 * Implementations of this class should not use any persistent state
 * so that they are thread-safe, as the {@link #execute(JSONObject)}
 * method may potentially be executed by multiple threads simultaneously.
 */
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

    /**
     * Create a new command handler
     * @param server The instance of the server which is creating the handler
     */
    public CommandHandler(Server server) {
        this.server = server;
        this.mgr = this.server.getStationManager();
        this.logger = this.server.getLogger();
    }

    /**
     * Get station IDs from JSON
     * @param parameters The JSON data
     * @return The list of station IDs (empty list if none specified)
     */
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

    /**
     * Name of the command e.g. "create", "stop", etc.
     * @return Name of the command
     */
    public abstract String getCommandName();

    /**
     * Execute the command.
     * @param jo The JSON input parameters
     * @return The command result
     */
    public abstract CommandResult execute(JSONObject jo) throws CommandException;

}
