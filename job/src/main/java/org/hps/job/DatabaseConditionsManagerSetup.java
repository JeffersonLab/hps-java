package org.hps.job;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.detector.svt.SvtDetectorSetup;
import org.hps.rundb.RunManager;
import org.lcsim.conditions.ConditionsListener;
import org.lcsim.conditions.ConditionsManager;
import org.lcsim.job.DefaultConditionsSetup;

/**
 * Provides setup for HPS specific conditions manager.
 * @author jeremym
 */
public final class DatabaseConditionsManagerSetup extends DefaultConditionsSetup {

    private Logger LOGGER = Logger.getLogger(DatabaseConditionsManagerSetup.class.getPackage().getName());   
    private boolean enableRunManager = true;
    private Set<String> tags = null;
    private boolean freeze = false;
    private DatabaseConditionsManager manager = null;
    
    public DatabaseConditionsManagerSetup() {        
        manager = new DatabaseConditionsManager();
        ConditionsManager.setDefaultConditionsManager(manager);
    }
    
    /**
     * Set whether system should be frozen after initialization.
     * @param freeze <code>true</code> to freeze conditions after initialization
     */
    public void setFreeze(boolean freeze) {
        this.freeze = freeze;
    }
    
    /**
     * Enable the run manager in the job.
     * @param enableRunManager <code>true</code> to enable run manager
     */
    public void setEnableRunManager(boolean enableRunManager) {
        this.enableRunManager = enableRunManager;
    }
    
    /**
     * Set the set of tags for filtering conditions records.
     * @param tags the set of tags for filtering conditions records
     */
    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    /**
     * Configure the conditions system before it is initialized.
     * <p>
     * An SVT specific setup class is added to the manager here.
     * <p>
     * This method will also optionally setup the {@link org.hps.run.database.RunManager} 
     * and can pass a set of tags to the conditions manager.
     */
    @Override
    public void configure() {
   
        if (enableRunManager) {
            manager.addConditionsListener(RunManager.getRunManager());
        }
        
        // Add class that will setup SVT detector with conditions data.
        manager.addConditionsListener(new SvtDetectorSetup());
                        
        // Add conditions system tags.
        if (this.tags != null) {
            LOGGER.config("adding tags " + tags.toString());
            manager.addTags(tags);
        }
        
        // Add extra listeners to manager.
        for (ConditionsListener listener : listeners) {
            manager.addConditionsListener(listener);
        }
    }
     
    /**
     * Do post initialization of conditions system, which will freeze the manager if it
     * is already fully initialized, meaning that the detector name and run were given
     * as arguments which will override the information from the event header in the data.
     */
    @Override
    public void postInitialize() {
        if (DatabaseConditionsManager.getInstance().isInitialized() || this.freeze) {
            LOGGER.config("Job manager is freezing the conditions system.");
            DatabaseConditionsManager.getInstance().freeze();
        }
    }
    
    /**
     * Do cleanup of conditions system after job.
     * <p>
     * Shuts down the database connection to the conditions manager.
     */
    @Override
    public void cleanup() {
 
        // Close the conditions database connection.
        Connection connection = DatabaseConditionsManager.getInstance().getConnection();
        try {
            if (connection != null && !connection.isClosed()) {
                DatabaseConditionsManager.getInstance().closeConnection();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, e.getMessage(), e);
        }
    }
}