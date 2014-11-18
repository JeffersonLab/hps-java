package org.hps.job;

import org.hps.conditions.ConditionsDriver;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.lcsim.job.JobControlManager;
import org.lcsim.util.Driver;

public class JobManager extends JobControlManager {

    // Override the basic LCSim conditions system with the HPS conditions manager.
    DatabaseConditionsManager conditionsManager = new DatabaseConditionsManager(); 
    
    public JobManager() {       
    }
    
    public static void main(String args[]) {
        JobManager job = new JobManager();
        job.run(args);
    }
    
    /**
     * Override the parent classes method that runs the job in order to do
     * conditions system initialization, if required.
     * @return True if job was successful.
     */
    @Override
    public boolean run() {
        setupConditions();
        boolean result = super.run();
        DatabaseConditionsManager.getInstance().closeConnection();
        return result;
    }

    /**
     * This method will find the {@link org.hps.conditions.ConditionsDriver} in the 
     * list of Drivers registered with the manager and execute its initialization
     * method, which can override the default behavior of the conditions system.
     */
    private void setupConditions() {
        ConditionsDriver conditionsDriver = null;
        for (Driver driver : this.getDriverAdapter().getDriver().drivers()) {
            if (driver instanceof ConditionsDriver) {
                conditionsDriver = (ConditionsDriver) driver;
                break;
            }
        }
        if (conditionsDriver != null) {
            conditionsDriver.initialize();
        }
    }
}
