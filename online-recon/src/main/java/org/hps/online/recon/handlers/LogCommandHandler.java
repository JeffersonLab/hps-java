package org.hps.online.recon.handlers;

import java.io.File;
import java.util.List;

import org.apache.commons.io.input.Tailer;
import org.hps.online.recon.CommandHandler;
import org.hps.online.recon.CommandResult;
import org.hps.online.recon.CommandResult.LogStreamResult;
import org.hps.online.recon.Server;
import org.hps.online.recon.SimpleLogListener;
import org.json.JSONObject;

/**
 * Tail a station's log file.
 */
class LogCommandHandler extends CommandHandler {

    protected LogCommandHandler(Server server) {
        super(server);
    }

    public CommandResult execute(JSONObject jo) {
        CommandResult res = null;
        List<Integer> ids = getStationIDs(jo);
        if (ids.size() == 1) {
            int id = ids.get(0);
            Long delayMillis = 1000L;
            if (jo.has("delayMillis")) {
                delayMillis = jo.getLong("delayMillis");
            }
            SimpleLogListener listener = new SimpleLogListener();
            Tailer tailer = mgr.getLogTailer(id, listener, delayMillis);
            File logFile = mgr.find(id).getLogFile();
            res = new LogStreamResult(tailer, listener, logFile);
        } else if (ids.size() > 1) {
            res = new CommandResult.Error("Multiple station IDs not supported for log command.");
        } else if (ids.size() == 0) {
            res = new CommandResult.Error("No station IDs were given in parameters.");
        }
        return res;
    }

    @Override
    public String getCommandName() {
        return "log";
    }
}
