package org.hps.recon.ecal;

import org.hps.record.daqconfig.ConfigurationManager;
import org.hps.record.daqconfig.DAQConfigDriver;
import org.hps.record.daqconfig.EvioDAQParser;
import org.hps.record.triggerbank.TriggerConfigData;
import org.hps.record.triggerbank.TriggerConfigData.Crate;
import org.hps.rundb.DaoProvider;
import org.hps.rundb.RunManager;
import org.lcsim.event.EventHeader;
import org.lcsim.geometry.Detector;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

/**
 * Driver <code>DatabaseDAQConfigDriver</code> is a variant of the
 * standard DAQ configuration driver that reads configuration data from
 * the run database instead of either local files or an EvIO file.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 */
public class DatabaseDAQConfigDriver extends DAQConfigDriver {
    // Define the crate enumerables by crate number. Crates are
    // in the order 46, 37, 39.
    private static final Crate[] CRATES = { Crate.CONFIG3, Crate.CONFIG1, Crate.CONFIG2 };
    
    /**
     * Updates the DAQ configuration manager with DAQ settings from the
     * run database.
     * @param detector - The detector object. This is not actually used.
     */
    @Override
    public void detectorChanged(Detector detector) {
        // Make sure that the run number is defined.
        if(getRunNumber() == -1) { throw new IllegalArgumentException("Run number is undefined."); }
        
        // Get the trigger configuration data.
        RunManager manager = new RunManager();
        manager.setRun(getRunNumber());
        DaoProvider factory = new DaoProvider(manager.getConnection());
        TriggerConfigData triggerConfig = factory.getTriggerConfigDao().getTriggerConfig(RunManager.getRunManager().getRun());
        
        // Convert the trigger configuration text blocks into individual
        // strings.
        String[][] data = null;
        try { data = getDataFileArrays(triggerConfig); }
        catch(IOException e) {
            throw new RuntimeException("An error occurred when processing the trigger data.");
        }
        
        // Instantiate an EvIO DAQ parser and feed it the data.
        EvioDAQParser daqConfig = new EvioDAQParser();
        for(int i = 0; i < 3; i++) {
            daqConfig.parse(CRATES[i].getCrateNumber(), getRunNumber(), data[i]);
        }
        
        // Update the configuration manager.
        ConfigurationManager.updateConfiguration(daqConfig);
        
        // Close the manager.
        manager.closeConnection();
    }
    
    /**
     * When loading from the database, information is pulled on detector
     * change rather than by looking for a specific object in event data.
     * As such, <code>process</code> is overwritten to do nothing.
     * @param event - Object containing event data.
     */
    @Override
    public void process(EventHeader event) { }
    
    /**
     * Parses the text dump containing the DAQ configuration and parses
     * it into lines. Data is returned as an array of strings. The first
     * array index corresponds to the crate dump and the second array
     * to the line.
     * @param triggerConfig - The DAQ configuration dump object.
     * @return Returns the DAQ configuration parsed as a String array.
     * @throws IOException Occurs if there is an error reading the dump
     * stream.
     */
    private static final String[][] getDataFileArrays(TriggerConfigData triggerConfig) throws IOException {
        // Create file readers to process the data files.
        StringReader[] fr = new StringReader[3];
        BufferedReader[] reader = new BufferedReader[3];
        for(int i = 0; i < 3; i++) {
            fr[i] = new StringReader(triggerConfig.getData().get(CRATES[i]));
            reader[i] = new BufferedReader(fr[i]);
        }
        
        // Convert the crate data into an array of strings. These must
        // be in the order of 46, 37, 39.
        String[][] data = getDataFileArrays(reader);
        
        // Close the readers.
        for(int i = 0; i < 3; i++) {
            reader[i].close();
            fr[i].close();
        }
        
        // Return the converted data.
        return data;
    }
}
