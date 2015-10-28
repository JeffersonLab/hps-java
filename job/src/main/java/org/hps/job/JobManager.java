package org.hps.job;

import java.io.InputStream;

import org.hps.conditions.ConditionsDriver;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.detector.svt.SvtDetectorSetup;
import org.lcsim.job.JobControlManager;
import org.lcsim.util.Driver;

/**
 * Extension of standard LCSim job manager which does some HPS-specific management of the conditions system.
 *
 * @author Jeremy McCormick, SLAC
 */
public class JobManager extends JobControlManager {

    /**
     * Run the job manager from the command line.
     *
     * @param args the command line arguments
     */
    public static void main(final String args[]) {
        // Run the job.
        final JobManager job = new JobManager();
        job.run(args);
    }

    /**
     * Class constructor.
     */
    public JobManager() {
    }

    /**
     * Override setup so the conditions system can be reset.
     * 
     * @param is the input stream containing config information
     */
    public void setup(InputStream is) {
        
        // Add class that will setup SVT detector with conditions data (this is awkward but has to be done someplace).
        DatabaseConditionsManager.getInstance().addConditionsListener(new SvtDetectorSetup());
        
        super.setup(is);
                
        // Setup the conditions system if there is a ConditionsDriver present.
        this.setupConditions();        
    }
    
    /**
     * Override the parent classes method that runs the job in order to perform conditions system initialization.
     *
     * @return <code>true</code> if job was successful
     */
    @Override
    public final boolean run() {
        
        // Run the job.
        final boolean result = super.run();

        // Close the conditions database connection if it is open.
        DatabaseConditionsManager.getInstance().closeConnection();

        return result;
    }

    /**
     * This method will find the {@link org.hps.conditions.ConditionsDriver} in the list of Drivers registered with the
     * manager and then execute its initialization method, which may override the default behavior of the conditions
     * system.
     */
    private void setupConditions() {
        ConditionsDriver conditionsDriver = null;
        for (final Driver driver : this.getDriverAdapter().getDriver().drivers()) {
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
