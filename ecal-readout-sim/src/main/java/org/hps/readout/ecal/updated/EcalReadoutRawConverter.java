package org.hps.readout.ecal.updated;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.ecal.EcalChannelConstants;
import org.hps.conditions.ecal.EcalConditions;
import org.hps.readout.ReadoutRawConverter;
import org.lcsim.geometry.Detector;

/**
 * <code>EcalReadoutRawConverter</code> handles the implementation of
 * calorimeter-specific functionality for {@link
 * org.hps.readout.ecal.updated.ReadoutRawConverter
 * ReadoutRawConverter}.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 * @see org.hps.readout.ecal.updated.ReadoutRawConverter
 */
public class EcalReadoutRawConverter extends ReadoutRawConverter {
    /**
     * Stores the calibrations and conditions for the calorimeter
     * subdetector.
     */
    private EcalConditions ecalConditions = null;
    
    @Override
    public void updateDetector(Detector detector) {
        ecalConditions = DatabaseConditionsManager.getInstance().getEcalConditions();
    }
    
    @Override
    protected double getGain(long cellID) {
        return findChannel(cellID).getGain().getGain();
    }
    
    @Override
    protected double getPedestal(long cellID) {
        return findChannel(cellID).getCalibration().getPedestal();
    }
    
    @Override
    protected double getTimeShift(long cellID) {
        return findChannel(cellID).getTimeShift().getTimeShift();
    }
    
    /**
     * Get the calorimeter conditions for the specified channel.
     * @param cellID - The channel ID.
     * @return Returns an object containing the conditions for the
     * specified calorimeter channel.
     */
    private EcalChannelConstants findChannel(long cellID) {
        return ecalConditions.getChannelConstants(ecalConditions.getChannelCollection().findGeometric(cellID));
    }
}