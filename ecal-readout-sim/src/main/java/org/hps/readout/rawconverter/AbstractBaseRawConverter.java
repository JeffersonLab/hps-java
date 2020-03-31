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
 * @author Tongtong Cao <caot@jlab.org>
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
    private int nsb = Integer.MIN_VALUE;
    
    /**
     * Stores the <u>n</u>umber of <u>s</u>amples <u>a</u>fter the 
     * threshold-crossing sample that are included in the integration
     * window. This must be a multiple of 4 ns.
     */
    private int nsa = Integer.MIN_VALUE;
    
    /**
     * Factor of unit conversion for returned value of the method <code>adcToEnergy()</code>.
     * For Ecal, unit of hit energy is GeV, so the factor = <code>EcalUtils.MeV</code>.
     * For hodo, unit of hit energy is self-defined for hodo FADC hits. 
     * Conversion from self-defined-unit/ADC to MeV/ADC for true hits 
     * is taken into account in <code>HodoscopeDigitizationReadoutDriver</code> 
     * by <code>factorGainConversion</code>. Here, factor = 1 for hodo.    
     */
    private double factorUnitConversion = EcalUtils.MeV;
    
    /**
     * Gets the number of samples that are included in the pulse
     * integral after the threshold-crossing sample. Note that this
     * value is inclusive of the threshold-crossing sample.
     * @return Returns the samples in units of nanoseconds.
     */
    public int getNumberSamplesAfter() {
        if(nsa == Integer.MIN_VALUE) {
            throw new RuntimeException("Error: NSA is not defined. Value must be set in steering file.");
        }
        return nsa;
    }
    
    /**
     * Gets the number of samples that are included in the pulse
     * integral before the threshold-crossing sample.
     * @return Returns the samples in units of nanoseconds.
     */
    public int getNumberSamplesBefore() {
        if(nsb == Integer.MIN_VALUE) {
            throw new RuntimeException("Error: NSB is not defined. Value must be set in steering file.");
        }
        return nsb;
    }
    
    /**
     * Gets factor of unit conversion for returned value of the method <code>adcToEnergy()</code>.
     * @return Returns factor of unit conversion for returned value of the method <code>adcToEnergy()</code>.
     */
    public double getFactorUnitConversion() {
        return factorUnitConversion;
    }
    
    /**
     * Sets the number of samples in the integration window
     * subsequent to the threshold-crossing sample.
     * @param nsa - The number of samples. It is required that
     * <code>nsa >= 0</code>.
     * @throws IllegalArgumentException Occurs if an invalid number
     * of samples is given.
     */
    public void setNumberSamplesAfter(int nsa) {
        if(nsa % NS_PER_SAMPLE != 0 || nsa < 0) {
            throw new IllegalArgumentException("NSA must be a multiple of " + NS_PER_SAMPLE + "ns and non-negative.");
        }
        this.nsa = nsa;
    }
    
    /**
     * Sets the number of samples in the integration window preceding
     * the threshold-crossing sample.
     * @param nsb - The number of samples. It is required that
     * <code>nsb >= 0</code>.
     * @throws IllegalArgumentException Occurs if an invalid number
     * of samples is given.
     */
    public void setNumberSamplesBefore(int nsb) {
        if(nsb % NS_PER_SAMPLE != 0 || nsb < 0) {
            throw new IllegalArgumentException("NSB must be a multiple of " + NS_PER_SAMPLE + "ns and non-negative.");
        }
        this.nsb = nsb;
    }
    
    /**
     * Sets factor of unit conversion for returned value of the method <code>adcToEnergy()</code>.
     * @param factor of unit conversion for returned value of the method <code>adcToEnergy()</code>
     * @throws IllegalArgumentException Occurs if an invalid factor is given.
     */
    public void setFactorUnitConversion(double factor) {
        if(factor == EcalUtils.MeV || (int)factor == 1) factorUnitConversion = factor; 
        else {
            throw new IllegalArgumentException("Factor of unit conversion for returned value of the method AbstractBaseRawConverter::adcToEnergy() must be " + EcalUtils.MeV + " or 1.");
        }        
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
        return getGain(channelID) * adcValue * factorUnitConversion;
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
        if(windowSamples > 0 && (nsa + nsb) / NS_PER_SAMPLE >= windowSamples) {
            firstSample = 0;
            lastSample = windowSamples - 1;
        }
        
        // Otherwise, the integration window is NSB before the
        // threshold-crossing sample and NSA after it. Exceptions are
        // cases where either NSA or NSB would extend beyond the
        // bounds of the available samples, in which case it is the
        // integration window is clipped at its bounds.
        else {
            firstSample = thresholdCrossing - nsb / NS_PER_SAMPLE;
            lastSample = thresholdCrossing + nsa / NS_PER_SAMPLE - 1;
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
