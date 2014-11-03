package org.hps.monitoring.ecal.eventdisplay.ui;

import java.awt.Point;
import java.io.IOException;

import org.hps.monitoring.ecal.eventdisplay.util.CrystalDataSet;
import org.hps.monitoring.ecal.eventdisplay.util.EcalWiringManager;

/**
 * Class <code>PDataEventViewer</code> is the passive variant of a data
 * viewer. It displays crystal hardware information read from a given
 * data file and displays it along with crystal energy and index data.
 * 
 * @author Kyle McCarty
 */
public class PDataEventViewer extends PEventViewer {
    // Local variables.
    private static final long serialVersionUID = 1L;
    private final EcalWiringManager ewm;
    
    // Hardware display fields.
    private static final String[] fieldNames = {
        "APD Number", "Preamp Number", "LED Channel", "LED Driver",
        "FADC Slot", "FADC Channel", "Splitter Number", "HV Group",
        "Jout", "MB", "Channel", "Gain"
    };
    
    // Hardware display field indices.
    private static final int FIELD_APD = 0;
    private static final int FIELD_PREAMP = 1;
    private static final int FIELD_LED_CHANNEL = 2;
    private static final int FIELD_LED_DRIVER = 3;
    private static final int FIELD_FADC_SLOT = 4;
    private static final int FIELD_FADC_CHANNEL = 5;
    private static final int FIELD_SPLITTER = 6;
    private static final int FIELD_HV_GROUP = 7;
    private static final int FIELD_JOUT = 8;
    private static final int FIELD_MB = 9;
    private static final int FIELD_CHANNEL = 10;
    private static final int FIELD_GAIN = 11;
    
    /**
     * Initializes a new <code>DataFileViewer</code> that reads from
     * the given event manager for event data and the given hardware
     * data file for crystal hardware data readout.
     * @param dataSource - The manager for event data.
     * @param crystalDataFilePath - The data file for crystal hardware
     * information.
     * @throws IOException Occurs if there is an error reading from
     * either data source.
     */
    public PDataEventViewer(String crystalDataFilePath) throws IOException {
        // Initialize the super class file.
        super();
        
        // Load the crystal data mapping.
        ewm = new EcalWiringManager(crystalDataFilePath);
        
        // Add the crystal data fields.
        for(String fieldName : fieldNames) {
            addStatusField(fieldName);
        }
    }
    
    @Override
    protected void updateStatusPanel() {
        // Run the superclass method.
        super.updateStatusPanel();
        
        // Get the selected crystal.
        Point crystal = ecalPanel.getSelectedCrystal();
        
        // If a crystal is selected, display its data set.
        if(crystal != null) {
            // Get the LCSim coordinate system version of the crystal.
            Point lcsimCrystal = Viewer.toEcalPoint(crystal);
            
            // Get the hardware data set associated with the crystal.
            CrystalDataSet cds = ewm.getCrystalData(lcsimCrystal);
            
            // If the data set exists, update the all the fields.
            if(cds != null) {
                setStatusField(fieldNames[FIELD_APD], "" + cds.getAPDNumber());
                setStatusField(fieldNames[FIELD_PREAMP], cds.getPreamplifierNumber().toString());
                setStatusField(fieldNames[FIELD_LED_CHANNEL], "" + cds.getLEDChannel());
                setStatusField(fieldNames[FIELD_LED_DRIVER], "" + cds.getLEDDriver());
                setStatusField(fieldNames[FIELD_FADC_SLOT], "" + cds.getFADCSlot());
                setStatusField(fieldNames[FIELD_FADC_CHANNEL], "" + cds.getFADCChannel());
                setStatusField(fieldNames[FIELD_SPLITTER], "" + cds.getSplitterNumber());
                setStatusField(fieldNames[FIELD_HV_GROUP], "" + cds.getHighVoltageGroup());
                setStatusField(fieldNames[FIELD_JOUT], "" + cds.getJout());
                setStatusField(fieldNames[FIELD_MB], "" + cds.getMotherboard().toString());
                setStatusField(fieldNames[FIELD_CHANNEL], "" + cds.getChannel());
                setStatusField(fieldNames[FIELD_GAIN], "" + cds.getGain());
            }
        }
    }
}