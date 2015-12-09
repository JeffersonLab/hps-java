package org.hps.job;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.hps.conditions.ConditionsDriver;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.detector.svt.SvtDetectorSetup;
import org.hps.run.database.RunManager;
import org.lcsim.conditions.ConditionsManager.ConditionsNotFoundException;
import org.lcsim.job.JobControlManager;
import org.lcsim.util.Driver;

/**
 * Extension of standard LCSim job manager which does some HPS-specific management of the conditions system.
 *
 * @author Jeremy McCormick, SLAC
 */
public class JobManager extends JobControlManager {

    /**
     * The set of conditions tags (none by default).
     */
    private Set<String> tags = null;
    
    /**
     * Run the job manager from the command line.
     *
     * @param args the command line arguments
     */
    public static void main(final String args[]) {
        // Run the job.
        final JobManager job = new JobManager();
        job.parse(args);
        job.run();
    }

    /**
     * Class constructor.
     */
    public JobManager() {
    }
   
    @Override
    protected Options createCommandLineOptions() {
        Options options = super.createCommandLineOptions();
        options.addOption("t", "tag", true, "conditions system tag (can be used multiple times)");
        return options;
    }
    
    @Override
    public CommandLine parse(final String args[]) {
        CommandLine commandLine = super.parse(args);
        if (commandLine.hasOption("t")) {
            tags = new HashSet<String>();
            for (String tag : commandLine.getOptionValues("t")) {
                tags.add(tag);
            }
        }
        return commandLine;
    }
    
    /**
     * Initialize the conditions system for the job.
     * <p>
     * If detector and run are provided from the command line or conditions driver then the
     * conditions system will be initialized and frozen.
     * 
     * @throws ConditionsNotFoundException if a condition is not found during initialization
     */
    protected void initializeConditions() throws ConditionsNotFoundException {
        
        // Initialize the db conditions manager.
        DatabaseConditionsManager dbManager = DatabaseConditionsManager.getInstance();
        
        // Initialize run manager and add as listener on conditions system.
        RunManager runManager = RunManager.getRunManager();
        dbManager.addConditionsListener(runManager);
        
        // Add class that will setup SVT detector with conditions data.
        dbManager.addConditionsListener(new SvtDetectorSetup());
        
        // Add conditions system tags.
        if (this.tags != null) {
            dbManager.addTags(tags);
        }
                       
        // Call super method which will initialize the conditions system if both the detector and run were provided.
        super.initializeConditions();
        
        // Setup from conditions driver (to be deleted soon!).
        if (!dbManager.isInitialized()) {
            setupConditionsDriver();
        } else {
            // Command line options overrode the conditions driver.
            LOGGER.config("Conditions driver was overridden by command line options!");
        }
        
        if (dbManager.isInitialized()) {
            // Assume conditions system should be frozen since detector and run were both set explicitly.
            LOGGER.config("Job manager is freezing the conditions system.");
            dbManager.freeze();
        }
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
                
        // Close the connection to the run db if necessary.
        RunManager.getRunManager().closeConnection();

        return result;
    }

    /**
     * This method will find the {@link org.hps.conditions.ConditionsDriver} in the list of Drivers registered with the
     * manager and then execute its initialization method, which may override the default behavior of the conditions
     * system.
     * @deprecated Use command line options of {@link org.lcsim.job.JobControlManager} instead.
     */
    @Deprecated
    private void setupConditionsDriver() {
        ConditionsDriver conditionsDriver = null;
        for (final Driver driver : this.getDriverAdapter().getDriver().drivers()) {
            if (driver instanceof ConditionsDriver) {
                conditionsDriver = (ConditionsDriver) driver;
                break;
            }
        }
        if (conditionsDriver != null) {
            LOGGER.config("initializing conditions Driver");
            conditionsDriver.initialize();
            LOGGER.warning("Conditions driver will be removed soon!");
        }
    }
}
