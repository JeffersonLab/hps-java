package org.hps.rundb.builder;

import org.hps.rundb.RunSummaryImpl;

/**
 * Class for incrementally building records for the run database.
 * <p>
 * Classes that add information to the run summary or create objects
 * for insertion into the run database should implement this.
 */
public abstract class AbstractRunBuilder {
    
    private RunSummaryImpl runSummary;
        
    void setRunSummary(RunSummaryImpl runSummary) {
        this.runSummary = runSummary;
    }
    
    RunSummaryImpl getRunSummary() {
        return runSummary;
    }
    
    int getRun() {
        if (this.runSummary == null) {
            throw new IllegalStateException("The run summary object was never set.");
        }
        return this.runSummary.getRun();
    }
    
    /**
     * Abstract method that sub-classes should implement to update the run summary or 
     * create objects for insertion into the database.
     */
    abstract void build();
}
