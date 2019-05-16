package org.hps.readout.rawconverter;

import java.util.ArrayList;

import org.hps.recon.ecal.CalorimeterHitUtilities;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.RawTrackerHit;

/**
 * <code>AbstractMode1RawConverter</code> provides functionality for
 * the {@link org.hps.readout.rawconverter.AbstractBaseRawConverter
 * AbstractBaseRawConverter} for the handling of Mode-1 hits. It also
 * includes hooks to allow for pulse-fitting of Mode-1 ADC buffers,
 * but does not itself support this behavior.
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
public abstract class AbstractMode1RawConverter extends AbstractBaseRawConverter {
    /**
     * The threshold (defined as over pedestal) at which pulse
     * integration begins. This time of the threshold-crossing sample
     * is also used to determine the hit time. Units are ADC.
     */
    private double integrationThreshold = 12;
    
    /**
     * The maximum number of threshold-crossings that may be
     * integrated in one ADC buffer.
     */
    private int nPeak = 3;
    
    /**
     * Specifies whether or not to output hits with Mode-7 additional
     * data or not. If <code>false</code>, Mode-3 style hits will be
     * produced instead.
     */
    private boolean mode7 = true;
    
    /**
     * Converts a Mode-1 ADC buffer {@link
     * org.lcsim.event.RawTrackerHit RawTrackerHit} object to a
     * {@link org.lcsim.event.CalorimeterHit CalorimeterHit} object
     * with calibrated energy and time values. Both Mode-3 and Mode-7
     * style output is support by this method.
     * @param hit - The hit to convert.
     * @return Returns the converted hit.
     */
    public ArrayList<CalorimeterHit> convertHit(RawTrackerHit hit) {
        // Get the channel ID for the sample.
        final long channelID = hit.getCellID();
        
        // Get the samples. If there are none, than there are no hits
        // that can be returned. Return a value of null to indicate
        // that empty sample.
        final short samples[] = hit.getADCValues();
        if(samples.length == 0) { return null; }
        
        // Calculate the absolute integration threshold This is the
        // normal integration thresold added to the pedestal for the
        // channel.
        final int absoluteThreshold = (int) (getPedestal(channelID) + integrationThreshold);
        
        // Store any threshold crossings that are found.
        ArrayList<Integer> thresholdCrossings = new ArrayList<Integer>();
        
        // If the first sample is already over threshold, it counts
        // as a threshold-crossing. Add it.
        if(samples[0] > absoluteThreshold) {
            thresholdCrossings.add(0);
        }
        
        // Iterate over the ADC samples and search for additional
        // instances of threshold-crossings.
        for(int i = 1; i < samples.length; ++i) {
            // If the sample exceeds the absolute integration
            // threshold, it is a crossing.
            if(samples[i] > absoluteThreshold && samples[i] <= absoluteThreshold) {
                // Add it to the list.
                thresholdCrossings.add(i);
                
                // No new crossings are permitted until the current
                // crossing's integration window has ended. Restart
                // the search subsequent to this point.
                i += getNumberSamplesAfter() / NS_PER_SAMPLE - 1;
                
                // The firmware only allows a certain number of
                // crossings. If that number has been reached, then
                // terminate the search.
                if(thresholdCrossings.size() >= nPeak) { break; }
            }
        }
        
        // Process the threshold-crossings and create hits from them.
        ArrayList<CalorimeterHit> newHits = new ArrayList<CalorimeterHit>();
        for(int thresholdCrossing : thresholdCrossings) {
            // do pulse integral:
            final double[] data = convertWaveformToPulse(hit, thresholdCrossing);
            double time = data[0];
            double sum = data[1];
            // TODO: Mode-7 data is not presently propagated. It should be placed in a GenericObject.
            // final double min = data[2];
            // final double max = data[3];
            final double fitQuality = data[4];
            
            if(fitQuality <= 0) {
                // do pedestal subtraction:
                sum -= getTotalPedestal(channelID, samples.length, thresholdCrossing);
            }
            
            // do gain scaling:
            double energy = adcToEnergy(sum, channelID);
            
            newHits.add(CalorimeterHitUtilities.create(energy, time, channelID));
        }
        
        return newHits;
    }
    
    /**
     * Sets the maximum number of threshold-crossings that may be
     * integrated in one ADC buffer. Allowed values are 1, 2 or 3.
     * @param nPeak - The maximum number of threshold-crossings.
     * @throws IllegalArgumentException Occurs if any value other
     * than 1, 2, or 3 is given.
     */
    public void setMaximumThresholdCrossings(int nPeak) {
        if(nPeak < 1 || nPeak > 3) {
            throw new IllegalArgumentException("The maximum number of allowed peaks must be one of the values 1, 2, or 3.");
        }
        this.nPeak = nPeak;
    }
    
    /**
     * Specifies whether or not output hits should include the
     * additional Mode-7 information or not.
     * @param mode7 - <code>true</code> means that Mode-7 information
     * should be included, and <code>false</code> that only Mode-3
     * information will be output.
     */
    public void setMode7(boolean mode7) {
        this.mode7 = mode7;
    }
    
    /**
     * Performs pulse integration of the ADC buffer stored in the
     * argument hit and outputs the results. These results include:
     * <ul><li>The pulse time</li>
     * <li>The integrated ADC sum</li>
     * <li>The Mode-7 "minimum ADC" value</li>
     * <li>The Mode-7 "maximum ADC" value</li>
     * <li>The fit quality</li></ul>
     * Note that this driver does not implement pulse-fitting. This
     * final value in the returned data is included for the benefit
     * of implementing drivers.
     * @param hit - The hit.
     * @param thresholdCrossing - The threshold-crossing sample where
     * upon the integration window is centered.
     * @return Returns the integration data as a <code>double</code>
     * array of the form <code>{ TIME, ADC_SUM, MODE7_MIN_ADC,
     * MODE7_MAX_ADC, FIT_QUALITY }</code>.
     */
    protected double[] convertWaveformToPulse(RawTrackerHit hit, int thresholdCrossing) {
        // Get the ADC buffer.
        short samples[] = hit.getADCValues();
        
        // Get the integration window.
        int firstSample;
        int lastSample;
        
        // In the event that NSA + NSB is larger than the ADC buffer,
        // the integration range in the entire buffer.
        if((getNumberSamplesAfter() + getNumberSamplesBefore()) / NS_PER_SAMPLE >= samples.length) {
            firstSample = 0;
            lastSample = samples.length - 1;
        }
        
        // Otherwise, just subtract NSB and add NSA to the position
        // of the threshold-crossing sample.
        else {
            firstSample = thresholdCrossing - getNumberSamplesBefore() / NS_PER_SAMPLE;
            lastSample = thresholdCrossing + getNumberSamplesAfter() / NS_PER_SAMPLE - 1;
        }
        
        // Mode-7 stores a "minimum/pedestal" value, which is the
        // average of the first four ADC samples. Calculate it.
        // TODO: Does the firmware's conversion of minADC to an int occur before or after the time calculation? This is undocumented.
        double minADC = 0;
        for(int jj = 0; jj < 4; jj++) { minADC += samples[jj]; }
        minADC = (minADC / 4);
        
        // Integrate the ADC buffer.
        double sumADC = 0;
        for (int sample = firstSample; sample <= lastSample; sample++) {
            // Ignore any samples that occur before or after the ADC
            // buffer. This data does not exist.
            if(sample < 0) { continue; }
            if(sample >= samples.length) { break; }
            
            // Add the ADC sample to the total.
            sumADC += samples[sample];
        }
        
        // Mode-7 stores the pulse maximum. Calculate it.
        // NOTE: The "- 5" is a firmware constant.
        double maxADC = 0;
        for(int sample = thresholdCrossing; sample < samples.length - 5; sample++) {
            if(samples[sample + 1] < samples[sample]) {
                maxADC = samples[sample];
                break;
            }
        }
        
        // The default pulse time has a 4 ns time resolution, and is
        // determined by the threshold-crossing sample.
        double pulseTime = thresholdCrossing * NS_PER_SAMPLE;
        
        // Mode-7 uses a higher resolution time.
        if(mode7) {
            // The case of a threshold-crossing sample at index 4 or
            // lower is treated as a special case by the firmware.
            if(thresholdCrossing < 4) {
                maxADC = 0;
                // TODO: If the firmware sets the time to 4 ns, shouldn't that be done here?
                // pulseTime = 4;
            }
            
            // Otherwise, perform a linear interpolation between the
            // threshold-crossing sample and pulse maximum to find a
            // time at the pulse half-height.
            else if(maxADC > 0) {
                final double halfMax = (maxADC + minADC) / 2;
                int t0 = -1;
                for(int sample = thresholdCrossing - 1; sample < lastSample; sample++) {
                    // Ignore samples larger than the buffer size.
                    if(sample >= samples.length - 1) { break; }
                    
                    // Search for the half-height sample.
                    if(samples[sample] <= halfMax && samples[sample + 1] > halfMax) {
                        t0 = sample;
                        break;
                    }
                }
                
                // If a half-height sample was found, interpolate the
                // the time from it.
                if(t0 > 0) {
                    final int a0 = samples[t0];
                    final int a1 = samples[t0 + 1];
                    pulseTime = ((halfMax - a0) / (a1 - a0) + t0) * NS_PER_SAMPLE;
                }
            }
        }
        
        return new double[] { pulseTime, sumADC, minADC, maxADC, -1 };
    }
}