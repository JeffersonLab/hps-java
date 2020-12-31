import java.util.Set;

// Wake up the station
/*
 * EtSystem etSystem = server.getEtSystem(); try { EtStation etStation =
 * server.getEtSystem().stationNameToObject(stationName);
 * etSystem.wakeUpAll(etStation); } catch (Exception e) { LOG.log(Level.WARNING,
 * "Error waking up stat)ion", e); }
 */


 /**
     * Interrupt all active threads in the server process and stop them, if necessary
     */
    /*
    private void cleanUpThreads() {
        LOG.fine("Cleaning up threads...");
        Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
        for (Thread thread : threadSet) {
            LOG.fine("Cleaning up thread: " + thread.getName());
            thread.interrupt();
            try {
                thread.join(1000L);
            } catch (InterruptedException e) {
            }
            if (thread.isAlive()) {
                thread.stop();
            }
            LOG.fine("Done cleaning up thread");
        }
        LOG.fine("Done cleaning up threads");
    }
    */

/*
    void debugPrintThreads() {
        Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
        for (Thread thread : threadSet) {
            LOG.info(thread.getName());
        }
    }*/

/**
 * Handle the settings command.
 */
/*
class SettingsCommandHandler extends CommandHandler {
    CommandResult execute(JSONObject parameters) {
        CommandResult res = null;
        StationManager mgr = Server.this.getStationManager();
        boolean error = false;
        if (parameters.length() > 0) {
            if (parameters.has("start")) {
                int startID = parameters.getInt("start");
                try {
                    Server.this.getStationManager().setStationID(startID);
                    LOG.config("Set new station start ID: " + mgr.getCurrentStationID());
                } catch (IllegalArgumentException e) {
                    LOG.log(Level.SEVERE, "Failed to set new station ID", e);
                    error = true;
                }
            }
            if (parameters.has("workdir")) {
                File newWorkDir = new File(parameters.getString("workdir"));
                try {
                    Server.this.setWorkDir(newWorkDir);
                } catch (Exception e) {
                    LOG.log(Level.SEVERE, "Failed to set new work dir: " + newWorkDir.getPath(), e);
                    error = true;
                }
            }
            if (parameters.has("basename")) {
                String stationBase = parameters.getString("basename");
                try {
                    Server.this.setStationBaseName(stationBase);
                } catch (IllegalArgumentException e) {
                    LOG.log(Level.SEVERE, "Failed to set station base name: " + stationBase, e);
                    error = true;
                }
            }
            if (error) {
                res = new Error("At least one setting failed to update (see server log).");
            } else {
                res = new Success("All settings updated successfully.");
            }
        } else {
            JSONObject jo = new JSONObject();
            jo.put("start", mgr.getCurrentStationID());
            jo.put("workdir", Server.this.getWorkDir());
            jo.put("basename", Server.this.getStationBaseName());
            res = new JSONResult(jo);
        }

        return res;
    }
}
*/
