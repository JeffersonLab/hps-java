package org.hps.recon.ecal;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.ecal.EcalChannelConstants;
import org.hps.conditions.ecal.EcalConditions;
import org.hps.record.daqconfig.ConfigurationManager;
import org.hps.record.daqconfig.FADCConfig;
import org.lcsim.geometry.Detector;

public class EcalGain {
    /**
     * If true, use a single gain factor for all channels. Else, use 442 gains from the conditions system.
     */
    private boolean constantGain = false;

    /**
     * A single gain factor for all channels (only used if constantGain=true)
     */
    private double gain;

    /**
     * If true, the relationship between ADC and GeV is a convention that includes readoutPeriod and a global scaling
     * factor. If false, it is the currently used convention: E(GeV) = GAIN * ADC
     */
    private boolean use2014Gain = false;
    
    /**
     * Set whether to use DAQ configuration read from EVIO to set EcalRawConverter parameters. This should be removed to
     * a standalone EcalRawCongverterDriver solely for trigger emulation.
     */
    public void setUseDAQConfig(boolean state) {
        useDAQConfig = state;
    }
    
    /**
     * If true, use the DAQ configuration from EVIO to set EcalRawConverter parameters. This should be removed to a
     * standalone EcalRawConverter solely for trigger emulation.
     */
    private boolean useDAQConfig = false;
    
    /**
     * Set global gain value and turn on constant gain. The 442 gains from the conditions system will be ignored.
     */
    public void setGain(double gain) {
        constantGain = true;
        this.gain = gain;
    }

    /**
     * Chooses which ADC --> Energy convention is used. If true, the relationship between ADC and GeV is a convention
     * that includes readoutPeriod and a global scaling factor. If false, it is the currently used convention: E(GeV) =
     * GAIN * ADC
     */
    public void setUse2014Gain(boolean use2014Gain) {
        this.use2014Gain = use2014Gain;
    }
    

    private EcalConditions ecalConditions = null;
    
    
    /**
     * return energy (units of GeV) corresponding to the ADC sum and crystal ID
     */
    public double adcToEnergy(double adcSum, long cellID) {

        // Get the channel data.
        EcalChannelConstants channelData = findChannel(cellID);

        if (useDAQConfig) {
            // float gain =
            // ConfigurationManager.getInstance().getFADCConfig().getGain(ecalConditions.getChannelCollection().findGeometric(cellID));
            return config.getGain(cellID) * adcSum * EcalUtils.MeV;
        } else if (use2014Gain) {
            if (constantGain) {
                return adcSum * EcalUtils.gainFactor * EcalUtils.ecalReadoutPeriod;
            } else {
                return channelData.getGain().getGain() * adcSum * EcalUtils.gainFactor * EcalUtils.ecalReadoutPeriod; // should
                                                                                                                      // not
                                                                                                                      // be
                                                                                                                      // used
                                                                                                                      // for
                                                                                                                      // the
                                                                                                                      // moment
                                                                                                                      // (2014/02)
            }
        } else {
            if (constantGain) {
                return gain * adcSum * EcalUtils.MeV;
            } else {
                return channelData.getGain().getGain() * adcSum * EcalUtils.MeV; // gain
                                                                                 // is
                                                                                 // defined
                                                                                 // as
                                                                                 // MeV/integrated
                                                                                 // ADC
            }
        }
    }
    
    
    


    /**
     * Must be set when an object EcalRawConverter is created.
     *
     * @param detector (long)
     */
    public void setDetector(Detector detector) {
        // ECAL combined conditions object.
        ecalConditions = DatabaseConditionsManager.getInstance().getEcalConditions();
        
    }

    /**
     * Convert physical ID to gain value.
     *
     * @param cellID (long)
     * @return channel constants (EcalChannelConstants)
     */
    public EcalChannelConstants findChannel(long cellID) {
        return ecalConditions.getChannelConstants(ecalConditions.getChannelCollection().findGeometric(cellID));
    }
    

    /**
     * The DAQ configuration from EVIO used to set EcalRawConverter parameters if useDAQConfig=true. This should be
     * removed to a standalone EcalRawConverter solely for trigger emulation.
     */
    private FADCConfig config = null;
    
    /**
     * Currently sets up a listener for DAQ configuration from EVIO. This should be removed to a standalone
     * ECalRawConverter solely for trigger emulation.
     */
    public EcalGain() {
        // Track changes in the DAQ configuration.
        ConfigurationManager.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                // If the DAQ configuration should be used, load the
                // relevant settings into the driver.
                if (useDAQConfig) {
                    // Get the FADC configuration.
                    config = ConfigurationManager.getInstance().getFADCConfig();

                   
                }
            }
        });
    }
   
}
