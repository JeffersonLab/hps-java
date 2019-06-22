package org.hps.online.recon;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.lcsim.event.EventHeader;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

import hep.aida.IBaseHistogram;
import hep.aida.IManagedObject;
import hep.aida.ITree;

/**
 * Driver to save intermediate AIDA files from online reconstruction station.
 * 
 * Plots can be reset after they are saved so that combined plot file can be
 * created incrementally.
 * 
 * @author jermc
 */
public class PlotDriver extends Driver {

    /**
     * Package logger.
     */
    private static Logger LOGGER = Logger.getLogger(PlotDriver.class.getPackageName());
    
    /** 
     * File name pattern which requires: 
     * station name, run number, file sequence number, file extension
     */
    private String fileNamePat = "%s_run_%05d_seq_%03d%s";
    
    /** Name of online reconstruction station. */
    private String stationName;
    
    /** Dynamic file path. */
    private String filePath;
    
    /** Output directory of files. */
    private String outputDir = System.getProperty("user.dir");

    /** Output file extension (default is ROOT output). */
    private String fileExt = ".root";
    
    /** Run number from conditions system for file naming. */
    private int runNumber = -1;
    
    /** Event interval for saving intermediate output files. */
    private int eventSaveInterval = -1;
    
    /** Current file sequence number for saving output files. */
    private int fileSeq = 0;
    
    /** True to reset all AIDA histograms after saving the output file. */
    private boolean resetAfterSave;
    
    /** Total events processed. */
    private int eventsProcessed;
    
    /** Default AIDA instance in this process. */
    private AIDA aida = AIDA.defaultInstance();
        
    /**
     * Set the event save interval.
     * @param eventSaveInterval The event save interval
     */
    public void setEventSaveInterval(int eventSaveInterval) {
        this.eventSaveInterval = eventSaveInterval;
    }
    
    /**
     * Set whether the plots should be reset after saving.
     * @param resetAfterSave True if plots should be reset after saving
     */
    public void setResetAfterSave(boolean resetAfterSave) {
        this.resetAfterSave = resetAfterSave;
    }
    
    /**
     * Set the station name, which is automatically converted to lower case,
     * as it is better for file naming.
     * 
     * @param stationName The station name.
     */
    public void setStationName(String stationName) {
        // Change name to lower case so it doesn't look weird. :)
        this.stationName = stationName.toLowerCase();
    }
    
    /**
     * Set the output directory for writing plot files
     * @param outputDir The output directory for writing plot files
     */
    public void setOutputDir(String outputDir) {
        this.outputDir = outputDir;
    }
    
    /**
     * Set the plot file extension which should be ".aida" or ".root".
     * @param fileExt The plot file extension
     */
    public void setFileExt(String fileExt) {
        if (!(fileExt.equals(".root") || fileExt.equals(".aida"))) {
            throw new IllegalArgumentException("Bad file extension: " + fileExt);
        }
        this.fileExt = fileExt;
    }
    
    /**
     * Handle conditions update to get the run number.
     */
    public void detectorChanged(Detector det) {
        this.runNumber = DatabaseConditionsManager.getInstance().getRun();
        setFilePath();
    }
    
    /**
     * Set the current file output name based on Driver parameters and run number.
     */
    private void setFilePath() {
        String fileName = String.format(this.fileNamePat, 
                this.stationName, this.runNumber, this.fileSeq, this.fileExt);
        this.filePath = this.outputDir + File.separator + fileName;
        LOGGER.info("Set new file path: " + this.filePath);
    }
    
    /**
     * Handle start of data by checking if station name is set.
     */
    public void startOfData() {
        if (stationName == null) {
            throw new RuntimeException("The station name was not set.");
        }
    }

    /**
     * Process events.
     */
    public void process(EventHeader event) {
        ++eventsProcessed;
        if (eventsProcessed % this.eventSaveInterval == 0) {
            // Save intermediate output file.
            save();
        }
    }
    
    /**
     * Handle end of data.
     */
    public void endOfData() {
        if (eventsProcessed > 0) {
            LOGGER.info("Saving final output file: " + this.filePath);
            save();
        }
    }
    
    /**
     * Save plots to new output file name, resetting the AIDA instance if enabled.
     */
    private void save() {
        try {
            LOGGER.info("Saving AIDA plots to: " + this.filePath);
            aida.saveAs(this.filePath);
            ++this.fileSeq;
            if (this.resetAfterSave) {
                LOGGER.info("Resetting the AIDA plots after saving to output file.");
                this.resetAida();
            }
            this.setFilePath();
        } catch (IOException e) {
            throw new RuntimeException("Error saving AIDA file", e);
        }
    }
    
    /**
     * Reset all objects in the AIDA instance.
     */
    private void resetAida() {
        ITree tree = aida.tree();
        for (String objectName : tree.listObjectNames()) {
            if (!objectName.endsWith("/")) {
                try {
                    IManagedObject object = tree.find(objectName);
                    if (object instanceof IBaseHistogram) {
                        ((IBaseHistogram) object).reset();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
