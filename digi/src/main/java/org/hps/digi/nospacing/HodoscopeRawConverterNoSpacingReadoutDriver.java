package org.hps.digi.nospacing;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import org.hps.readout.ReadoutDriver;
//import org.hps.readout.RawConverterNoSpacingReadoutDriver;
import org.hps.readout.rawconverter.AbstractMode3RawConverter;
import org.hps.readout.rawconverter.HodoscopeReadoutMode3RawConverter;
import org.hps.record.daqconfig2019.ConfigurationManager2019;
import org.hps.record.daqconfig2019.DAQConfig2019;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.subdetector.Hodoscope_v1;

/**
 * <code>HodoscopeRawConverterNoSpacingReadoutDriver</code> is an
 * implementation of {@link org.hps.readout.RawConverterReadoutDriver
 * RawConverterReadoutDriver} for the hodoscope subdetector.
 * 
 * @see org.hps.readout.RawConverterReadoutDriver
 */
public class HodoscopeRawConverterNoSpacingReadoutDriver extends RawConverterNoSpacingReadoutDriver {    
    /**
     * The converter object responsible for processing raw hits into
     * proper {@link org.lcsim.event.CalorimeterHit CalorimeterHit}
     * objects.
     */
    private HodoscopeReadoutMode3RawConverter converter = new HodoscopeReadoutMode3RawConverter();
    
    /**
     * Instantiates the driver with the correct default parameters.
     */
    public HodoscopeRawConverterNoSpacingReadoutDriver() {
        super("HodoscopeRawHits", "HodoscopeCorrectedHits");
    }
    
    /**
     * Sets whether or not the DAQ configuration is applied into the driver
     * the EvIO data stream or whether to read the configuration from data files.
     * 
     * @param state - <code>true</code> indicates that the DAQ configuration is
     * applied into the readout system, and <code>false</code> that it
     * is not applied into the readout system.
     */
    public void setDaqConfigurationAppliedintoReadout(boolean state) {
        // Track changes in the DAQ configuration.
        if (state) {
            ConfigurationManager2019.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    // Get the DAQ configuration.
                    DAQConfig2019 daq = ConfigurationManager2019.getInstance();

                    // Load the DAQ settings from the configuration manager.
                    getConverter().setNumberSamplesAfter(daq.getHodoFADCConfig().getNSA());
                    getConverter().setNumberSamplesBefore(daq.getHodoFADCConfig().getNSB());
                    
                    // Get the FADC configuration.
                    getConverter().setFADCConfigHodo2019(daq.getHodoFADCConfig());
                }
            });
        }         
    }    
    
    @Override
    protected AbstractMode3RawConverter getConverter() {
        return converter;
    }
    
    @Override
    protected String getSubdetectorReadoutName(Detector detector) {
        Hodoscope_v1 hodoscopeGeometry = (Hodoscope_v1) detector.getSubdetector("Hodoscope");
        return hodoscopeGeometry.getReadout().getName();
    }
    
    @Override
    protected void updateDetectorDependentParameters(Detector detector) { }    
}
