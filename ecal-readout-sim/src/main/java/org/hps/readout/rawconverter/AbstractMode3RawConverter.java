package org.hps.readout.rawconverter;

import org.hps.recon.ecal.CalorimeterHitUtilities;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.RawCalorimeterHit;

/**
 * <code>AbstractMode3RawConverter</code> provides functionality for
 * the {@link org.hps.readout.rawconverter.AbstractBaseRawConverter
 * AbstractBaseRawConverter} for the handling of Mode-3 hits.
 * <br/><br/>
 * <code>AbstractMode3RawConverter</code> does not implement any
 * behavior that is specific to a subdetector. That is left for its
 * implementing drivers.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 * @author Sho Uemura <meeg@slac.stanford.edu>
 * @author Andrea Celentano <andrea.celentano@ge.infn.it>
 * @author Nathan Baltzell <baltzell@jlab.org>
 * @author Holly Szumila <hvanc001@odu.edu>
 * @see org.hps.readout.rawconverter.AbstractBaseRawConverter
 */
public abstract class AbstractMode3RawConverter extends AbstractBaseRawConverter {
    /**
     * Stores the number of samples in the readout window. This is
     * needed to properly account for pulse-clipping in Mode-3 and
     * Mode-7 data, where this value is not retained. This value is
     * not used for Mode-1 input, as the number of samples is known
     * for this data.
     */
    protected int windowSamples = -1;
    
    /**
     * Converts a Mode-3 digitized hit to a {@link
     * org.lcsim.event.CalorimeterHit CalorimeterHit} object with
     * calibrated energy and time values.
     * @param hit - The hit to convert.
     * @param timeOffset - The time offset of the hit.
     * @return Returns the converted hit.
     */
    public CalorimeterHit convertHit(RawCalorimeterHit hit, double timeOffset) {
        // Get the hit time and channel ID.
        long channelID = hit.getCellID();
        double time = hit.getTimeStamp() / 16.0;
        
        // Get the pedestal for the entire hit integration window.
        double pedestal = getTotalPedestal(channelID, windowSamples, (int) time / NS_PER_SAMPLE);
        
        // Get the pedestal-corrected ADC sum and convert it to
        // units of energy.
        double adcSum = hit.getAmplitude() - pedestal;
        double rawEnergy = adcToEnergy(adcSum, channelID);
        
        // Calculate the time for the hit.
        time -= getTimeShift(channelID);
        
        // Create a new calorimeter hit and return it.
        return CalorimeterHitUtilities.create(rawEnergy, time + timeOffset, channelID);
    }
    
    /**
     * Gets the time shift for the specified subdetector channel.
     * @param cellID - The detector element ID.
     * @return Returns the value of the time shift as a
     * <code>double</code>.
     */
    protected abstract double getTimeShift(long channelID);
    
    /**
     * Sets the number of samples in the readout window. Note that
     * this value is ignored for Mode-1 data, since Mode-1 data
     * retains the number of ADC samples already.
     * @param windowSamples - The number of 4 ns clock-cycle samples
     * present in the readout window.
     */
    public void setWindowSamples(int windowSamples) {
        this.windowSamples = windowSamples;
    }
}