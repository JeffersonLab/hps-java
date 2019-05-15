package org.hps.readout.rawconverter;

import org.hps.recon.ecal.EcalUtils;
import org.lcsim.geometry.Detector;

/**
 * <code>AbstractBaseRawConverter</code> implements all of the basic
 * functionality common to all raw converters. It does not, however,
 * perform any actual hit conversions.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 * @author Sho Uemura <meeg@slac.stanford.edu>
 * @author Andrea Celentano <andrea.celentano@ge.infn.it>
 * @author Nathan Baltzell <baltzell@jlab.org>
 * @author Holly Szumila <hvanc001@odu.edu>
 */
public abstract class AbstractBaseRawConverter {
    /**
     * The time for one FADC sample (units = ns).
     */
    protected static final int NS_PER_SAMPLE = 4;
    
    /**
     * Stores the <u>n</u>umber of <u>s</u>amples <u>b</u>efore the 
     * threshold-crossing sample that are included in the integration
     * window. This must be a multiple of 4 ns.
     */
    protected int NSB = 20;
    
    /**
     * Stores the <u>n</u>umber of <u>s</u>amples <u>a</u>fter the 
     * threshold-crossing sample that are included in the integration
     * window. This must be a multiple of 4 ns.
     */
    protected int NSA = 100;
    
    /**
     * Sets the number of samples in the integration window
     * subsequent to the threshold-crossing sample.
     * @param nsa - The number of samples. It is required that
     * <code>nsa >= 0</code>.
     * @throws IllegalArgumentException Occurs if an invalid number
     * of samples is given.
     */
    public void setNSA(int nsa) {
        if(NSA % NS_PER_SAMPLE != 0 || NSA < 0) {
            throw new IllegalArgumentException("NSA must be a multiple of " + NS_PER_SAMPLE + "ns and non-negative.");
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
        if(NSB % NS_PER_SAMPLE != 0 || NSB < 0) {
            throw new IllegalArgumentException("NSB must be a multiple of " + NS_PER_SAMPLE + "ns and non-negative.");
        }
        NSB = nsb;
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
    protected double adcToEnergy(double adcValue, long channelID) {
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
    protected double getTotalPedestal(long channelID, int windowSamples, int thresholdCrossing) {
        // Store the indices of the first and last samples.
        int firstSample, lastSample;
        
        // If the total number of samples is larger than the size of
        // the entire window, the firmware will always integrate the
        // entire window.
        if(windowSamples > 0 && (NSA + NSB) / NS_PER_SAMPLE >= windowSamples) {
            firstSample = 0;
            lastSample = windowSamples - 1;
        }
        
        // Otherwise, the integration window is NSB before the
        // threshold-crossing sample and NSA after it. Exceptions are
        // cases where either NSA or NSB would extend beyond the
        // bounds of the available samples, in which case it is the
        // integration window is clipped at its bounds.
        else {
            firstSample = thresholdCrossing - NSB / NS_PER_SAMPLE;
            lastSample = thresholdCrossing + NSA / NS_PER_SAMPLE - 1;
            if(windowSamples > 0) {
                if(firstSample < 0) { firstSample = 0; }
                if(lastSample >= windowSamples) { lastSample = windowSamples - 1; }
            }
        }
        
        // Return the total pedestal.
        return(lastSample - firstSample + 1) * getPedestal(channelID);
    }
    
    /**
     * Gets the pedestal for an entire integration window for Mode-3
     * and Mode-7 hits.
     * @param channelID - The channel ID for the subdetector element
     * on which the sample occurred.
     * @param windowSamples - The number of samples in ADC window.
     * @param thresholdCrossing - The sample on which the
     * threshold-crossing sample occurred.
     * @return Returns the total pedestal across the entire
     * integration window.
     * @throws IllegalArgumentException Occurs if the total number of
     * samples in the ADC window is not defined.
     */
    /*
    protected double getTotalPedestal(long channelID, int windowSamples, int thresholdCrossing) {
        // Window samples must be defined for Mode-3 or Mode-7 data.
        if(windowSamples < 0) {
            throw new IllegalArgumentException("Mode-3 and Mode-7 require that the number of samples in the readout window be defined.");
        }
        
        // Store the first and last samples. The total pedestal will
        // be a single pedestal, times the number of samples in the
        // integration window.
        int firstSample;
        int lastSample;
        
        // There is a special case where the size of the integration
        // window exceeds the size of the ADC sample. In this case,
        // the entire ADC sample size is used for the integration
        // window.
        if(((NSA + NSB) / NS_PER_SAMPLE) >= windowSamples) {
            firstSample = 0;
            lastSample = windowSamples - 1;
        }
        
        // Otherwise, the window is NSB before and NSA after the
        // threshold-crossing sample.
        else {
            // Calculate the start and end of the window.
            firstSample = thresholdCrossing - NSB / NS_PER_SAMPLE;
            lastSample = thresholdCrossing + NSA / NS_PER_SAMPLE - 1;
            
            // Correct for any portion of the pulse that falls
            // outside the ADC sample. There is no data here, so it
            // can not contribute either positively or negatively to
            // the integrated ADC value.
            if(firstSample < 0) { firstSample = 0; }
            if(lastSample >= windowSamples) { lastSample = windowSamples - 1; }
            
        }
        
        // Return the integration window multiplied by the pedestal
        // value for an individual sample.
        return (lastSample - firstSample + 1) * getPedestal(channelID);
    }
    */
}