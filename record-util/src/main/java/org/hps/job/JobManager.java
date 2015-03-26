package org.hps.job;

import org.hps.conditions.ConditionsDriver;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.lcsim.job.JobControlManager;
import org.lcsim.util.Driver;

/**
 * Extension of standard LCSim job manager which does some HPS-specific management
 * of the conditions system.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class JobManager extends JobControlManager {
    
    public JobManager() {
        // Always want to reset conditions system before starting the job.
        DatabaseConditionsManager.resetInstance();
    }
    
    public static void main(String args[]) {
        // Run the job.
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
        
        // Setup the conditions if there is a ConditionsDriver present.
        setupConditions();
        
        // Run the job.
        boolean result = super.run();
        
        // Close the conditions database connection if it is open.        
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
