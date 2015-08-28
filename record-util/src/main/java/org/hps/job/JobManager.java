package org.hps.job;

import org.hps.conditions.ConditionsDriver;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.rundb.RunManager;
import org.lcsim.job.JobControlManager;
import org.lcsim.util.Driver;

/**
 * Extension of standard LCSim job manager which does some HPS-specific management of the conditions system.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
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
     * Flag to enable the run database in a job (disabled by default).
     */
    private boolean enableRunDatabase = false;

    /**
     * Class constructor.
     */
    public JobManager() {

        try {
            // Since this is packaged with the distribution, the class is not directly accessible.
            final Object hpsJavaProperties = Class.forName("org.hps.HPSJavaProperties").newInstance();
            logger.info(hpsJavaProperties.toString());
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            // Just don't print info if not accessible (running in test case?).
        }

        // Always want to reset the conditions system before starting the job.
        // FIXME: Should this happen in the run method instead?
        DatabaseConditionsManager.resetInstance();
    }

    /**
     * Override the parent classes method that runs the job in order to perform conditions system initialization.
     *
     * @return <code>true</code> if job was successful
     */
    @Override
    public final boolean run() {

        // Setup the conditions if there is a ConditionsDriver present.
        this.setupConditions();

        // Is usage of run database enabled?
        if (this.enableRunDatabase) {
            // Enable the run database.
            this.setupRunManager();
        }

        // Run the job.
        final boolean result = super.run();

        // Close the conditions database connection if it is open.
        DatabaseConditionsManager.getInstance().closeConnection();

        return result;
    }

    /**
     * Enable the run database.
     *
     * @param enableRunDatabase <code>true</code> to enable run database
     */
    public void setEnableRunDatabase(final boolean enableRunDatabase) {
        this.enableRunDatabase = enableRunDatabase;
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

    /**
     * Set the {@link org.hps.rundb.RunManager} for accessing the run database.
     */
    private void setupRunManager() {
        final RunManager runManager = RunManager.getRunManager();
        DatabaseConditionsManager.getInstance().addConditionsListener(runManager);
    }
}
