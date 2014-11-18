package org.hps.job;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.lcsim.job.JobControlManager;

public class JobManager extends JobControlManager {
    
    public JobManager() {
        // Override the basic LCSim conditions system with the HPS conditions manager.
        new DatabaseConditionsManager();
    }
    
    public static void main(String args[]) {
        JobManager job = new JobManager();
        job.run(args);
    }
    
    public boolean run() {
        boolean result = super.run();
        DatabaseConditionsManager.getInstance().closeConnection();
        return result;
    }
}
