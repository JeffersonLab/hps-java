package org.hps.run.database;

import java.io.File;
import java.util.logging.Logger;

import org.hps.conditions.run.RunSpreadsheet;
import org.hps.conditions.run.RunSpreadsheet.RunData;

/**
 * Builds a complete {@link RunSummary} object from various data sources, including the data catalog and the run
 * spreadsheet, so that it is ready to be inserted into the run database using the DAO interfaces.  This class also 
 * extracts EPICS data, scaler data, trigger config and SVT config information from all of the EVIO files in a run.
 * <p>
 * The setters and some other methods follow the builder pattern and so can be chained by the caller.
 * 
 * @author Jeremy McCormick, SLAC
 * @see RunSummary
 * @see RunSummaryImpl
 */
final class SpreadsheetBuilder extends AbstractRunBuilder {
    
    private static final Logger LOGGER = Logger.getLogger(SpreadsheetBuilder.class.getPackage().getName());
    
    private File spreadsheetFile;
    
    void setSpreadsheetFile(File spreadsheetFile) {
        this.spreadsheetFile = spreadsheetFile;
    }

    /**
     * Update the current run summary from information in the run spreadsheet.
     * 
     * @param spreadsheetFile file object pointing to the run spreadsheet (CSV format)
     * @return this object
     */
    @Override
    void build() {       
        if (this.spreadsheetFile == null) {
            throw new IllegalStateException("The spreadsheet file was never set.");
        }
        if (getRunSummary() == null) {
            throw new IllegalStateException("The run summary was never set.");
        }
        LOGGER.fine("updating from spreadsheet file " + spreadsheetFile.getPath());
        RunSpreadsheet runSpreadsheet = new RunSpreadsheet(spreadsheetFile);
        RunData data = runSpreadsheet.getRunMap().get(getRunSummary().getRun());        
        if (data != null) {
            LOGGER.info("found run data ..." + '\n' + data.getRecord());
            
            // Trigger config name.
            String triggerConfigName = data.getRecord().get("trigger_config");
            if (triggerConfigName != null) {
                getRunSummary().setTriggerConfigName(triggerConfigName);
                LOGGER.info("set trigger config name <" + getRunSummary().getTriggerConfigName() + "> from spreadsheet");
            }
            
            // Notes.
            String notes = data.getRecord().get("notes");
            if (notes != null) {
                getRunSummary().setNotes(notes);
                LOGGER.info("set notes <" + getRunSummary().getNotes() + "> from spreadsheet");
            }
            
            // Target.
            String target = data.getRecord().get("target");
            if (target != null) {
                getRunSummary().setTarget(target);
                LOGGER.info("set target <" + getRunSummary().getTarget() + "> from spreadsheet");
            }
        } else {
            LOGGER.warning("No record for this run was found in spreadsheet.");
        }
    }
}
