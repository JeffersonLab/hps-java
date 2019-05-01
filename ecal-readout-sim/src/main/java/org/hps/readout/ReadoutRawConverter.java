package org.hps.readout;

import org.hps.recon.ecal.CalorimeterHitUtilities;
import org.hps.recon.ecal.EcalUtils;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.RawCalorimeterHit;
import org.lcsim.geometry.Detector;

/**
 * <code>ReadoutRawConverter</code> is a very basic converter that
 * handles the conversion of a Mode-3 {@link
 * org.lcsim.event.RawCalorimeterHit RawCalorimeterHit} to a {@link
 * org.lcsim.event.CalorimeterHit CalorimeterHit} object with gain,
 * pedestal subtraction, and time shift added. It requires specific
 * implementations for each subdetector which are required to define
 * the appropriate methods for obtaining these calibrations.
 *
 * @author Sho Uemura <meeg@slac.stanford.edu>
 * @author Andrea Celentano <andrea.celentano@ge.infn.it>
 * @author Nathan Baltzell <baltzell@jlab.org>
 * @author Holly Szumila <hvanc001@odu.edu>
 * @author Kyle McCarty <mccarty@jlab.org>
 */
public abstract class ReadoutRawConverter {
    /**
     * The time for one FADC sample (units = ns).
     */
    private static final int nsPerSample = 4;
    
    /**
     * Stores the <b>n</b>umber of <b>s</b>amples <b>b</b>efore the 
     * threshold-crossing sample that are included in the integration
     * window. This must be a multiple of 4 ns.
     */
    private int NSB = 20;
    /**
     * Stores the <b>n</b>umber of <b>s</b>amples <b>a</b>fter the 
     * threshold-crossing sample that are included in the integration
     * window. This must be a multiple of 4 ns.
     */
    private int NSA = 100;
    
    /**
     * Converts a Mode-3 digitized hit to a {@link
     * org.lcsim.event.CalorimeterHit CalorimeterHit} object with
     * calibrated energy and time values.
     * @param hit - The hit to convert.
     * @param timeOffset - The time offset of the hit.
     * @return Returns the converted hit.
     */
    public CalorimeterHit convertHit(RawCalorimeterHit hit, double timeOffset) {
        // Check that the timestamp makes sense for the hit.
        if(hit.getTimeStamp() % 64 != 0) {
            System.out.println("Unexpected timestamp: " + hit.getTimeStamp());
        }
        
        // Get the basic hit parameters.
        long channelID = hit.getCellID();
        double time = hit.getTimeStamp() / 16.0;
        
        // Get the subdetector-appropriate pedestal.
        double pedestal = getPulsePedestal(channelID, -1, (int) time / nsPerSample);
        
        // Convert the hit to an energy.
        double adcSum = hit.getAmplitude() - pedestal;
        double rawEnergy = adcToEnergy(adcSum, channelID);
        
        // Calculate the time for the hit.
        time -= getTimeShift(channelID);
        
        // Return the result as a CalorimeterHit object.
        return CalorimeterHitUtilities.create(rawEnergy, time + timeOffset, channelID);
    }
    
    /**
     * Gets the pedestal for an entire pulse integral. This method
     * accounts for pulse-clipping if <code>windowSamples</code> is
     * defined.
     * @param event - The event containing the data,
     * @param cellID - The channel ID of the detector element on
     * which the pulse occurred.
     * @param windowSamples - The number of samples in the ADC
     * window.
     * @param thresholdCrossing - The index of the threshold-crossing
     * sample.
     * @return Returns the total pedestal for the integration window
     * as a <code>double</code>.
     */
    public double getPulsePedestal(long channelID, int windowSamples, int thresholdCrossing) {
        // Store the indices of the first and last samples.
        int firstSample, lastSample;
        
        // If the total number of samples is larger than the size of
        // the entire window, the firmware will always integrate the
        // entire window.
        if(windowSamples > 0 && (NSA + NSB) / nsPerSample >= windowSamples) {
            firstSample = 0;
            lastSample = windowSamples - 1;
        }
        
        // Otherwise, the integration window is NSB before the
        // threshold-crossing sample and NSA after it. Exceptions are
        // cases where either NSA or NSB would extend beyond the
        // bounds of the available samples, in which case it is the
        // integration window is clipped at its bounds.
        else {
            firstSample = thresholdCrossing - NSB / nsPerSample;
            lastSample = thresholdCrossing + NSA / nsPerSample - 1;
            if(windowSamples > 0) {
                if(firstSample < 0) { firstSample = 0; }
                if(lastSample >= windowSamples) { lastSample = windowSamples - 1; }
            }
        }
        
        // Return the total pedestal.
        return(lastSample - firstSample + 1) * getPedestal(channelID);
    }
    
    /**
     * Updates any detector-dependent parameters or functionality
     * used by the converter.
     * @param detector - The detector object representing the new
     * detector.
     */
    public abstract void updateDetector(Detector detector);
    
    /**
     * Convert an ADC value to an energy value.
     * @param adcSum - The ADC value.
     * @param cellID - The channel ID for the detector element from
     * which the ADC value is derived.
     * @return Returns the ADC value in units of GeV.
     */
    private double adcToEnergy(double adcValue, long channelID) {
        return getGain(channelID) * adcValue * EcalUtils.MeV;
    }
    
    /**
     * Gets the gain for the specified subdetector channel.
     * @param cellID - The detector element ID.
     * @return Returns the value of the gain as a
     * <code>double</code>.
     */
    protected abstract double getGain(long channelID);
    
    /**
     * Gets the pedestal for the specified subdetector channel.
     * @param cellID - The detector element ID.
     * @return Returns the value of the pedestal as a
     * <code>double</code>.
     */
    protected abstract double getPedestal(long channelID);
    
    /**
     * Gets the time shift for the specified subdetector channel.
     * @param cellID - The detector element ID.
     * @return Returns the value of the time shift as a
     * <code>double</code>.
     */
    protected abstract double getTimeShift(long channelID);
    
    /**
     * Sets the number of samples in the integration window
     * subsequent to the threshold-crossing sample.
     * @param nsa - The number of samples. It is required that
     * <code>nsa >= 0</code>.
     * @throws IllegalArgumentException Occurs if an invalid number
     * of samples is given.
     */
    public void setNSA(int nsa) {
        if(NSA % nsPerSample != 0 || NSA < 0) {
            throw new IllegalArgumentException("NSA must be a multiple of " + nsPerSample + "ns and non-negative.");
        }
        NSA = nsa;
    }
    
    /**
     * Sets the number of samples in the integration window preceding
     * the threshold-crossing sample.
     * @param nsb - The number of samples. It is required that
     * <code>nsb >= 0</code>.
     * @throws IllegalArgumentException Occurs if an invalid number
     * of samples is given.
     */
    public void setNSB(int nsb) {
        if(NSB % nsPerSample != 0 || NSB < 0) {
            throw new IllegalArgumentException("NSB must be a multiple of " + nsPerSample + "ns and non-negative.");
        }
        NSB = nsb;
    }
}