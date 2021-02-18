package org.hps.recon.ecal;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.record.daqconfig2019.ConfigurationManager2019;
import org.hps.record.daqconfig2019.FADCConfigHodo2019;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.RawTrackerHit;

/**
 * <code>HodoOnlineRawConverter2019</code> handles the conversion of raw hits of
 * all modes to energy hit <code>CalorimeterHit</code> objects. This converter
 * will employ the runtime values for all parameters and is intended to emulate
 * the firmware specifically.<br/>
 * <br/>
 * The converter requires the presence of the DAQ configuration manager, which
 * is activated by <code>DAQConfigDriver2019</code>. <br/>
 * <br/>
 * This converter is primarily employed in the trigger and hardware diagnostic
 * processes as well as the readout simulation in Monte Carlo. <br/>
 * <br/>
 * 
 * @author Tongtong Cao <caot@jlab.org>
 */
public class HodoOnlineRawConverter2019 {
    // Defines the maximum number of peaks that may be extracted from
    // a single waveform.
    private int nPeak = Integer.MAX_VALUE;
    // The DAQ configuration manager for FADC parameters.
    private FADCConfigHodo2019 config = null;
    // The number of nanoseconds in a clock-cycle (sample).
    private static final int nsPerSample = 4;

    /**
     * Instantiates the <code>HodoOnlineRawConverter2019</code> and connects it to
     * the <code>ConfigurationManager2019</code> to receive settings from the DAQ
     * configuration.
     */
    public HodoOnlineRawConverter2019() {
        // Track changes in the DAQ configuration.
        ConfigurationManager2019.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Get the FADC configuration.
                config = ConfigurationManager2019.getInstance().getHodoFADCConfig();
            }
        });
    }

    /**
     * Gets the pedestal for a given crystal and threshold crossing.
     * 
     * @param cellID            - The cell ID of the crystal.
     * @param windowSamples     - The size of the readout window. A value of
     *                          <code>-1</code> indicates an infinite readout
     *                          window.
     * @param thresholdCrossing - The time of the threshold crossing in 4-nanosecond
     *                          clock-cycles (samples).
     * @return Returns the pedestal for the crystal and threshold crossing.
     */
    public int getPulsePedestal(long cellID, int windowSamples, int thresholdCrossing) {
        // Track the starting and ending samples over which integration
        // will occur. Only the intermediary samples need be considered
        // for pedestal calculation.
        int firstSample, lastSample;

        // For finite readout windows, calculate the pedestal based on
        // the size of the full readout window in the event that the
        // integration window is larger than the readout window.
        if (windowSamples > 0 && (config.getNSA() + config.getNSB()) / nsPerSample >= windowSamples) {
            firstSample = 0;
            lastSample = windowSamples - 1;
        }

        // Otherwise, the pedestal should be calculated based on the
        // integration window size.
        else {
            // Define the sample width as equivalent to the integration
            // window size.
            firstSample = thresholdCrossing - config.getNSB() / nsPerSample;
            lastSample = thresholdCrossing + config.getNSA() / nsPerSample - 1;

            // In the event of a finite readout window, ignore any
            // samples that fall outside the readout window. Since these
            // are clipped and will not be integrated, these pedestals
            // do not contribute.
            if (windowSamples > 0) {
                if (firstSample < 0) {
                    firstSample = 0;
                }
                if (lastSample >= windowSamples) {
                    lastSample = windowSamples - 1;
                }
            }
        }

        // Calculate and return the pedestal. The extra 1 MeV added to
        // the pedestal offsets the rounding error that is incurred when
        // converting it to an integer.
        return (int) ((lastSample - firstSample + 1) * (config.getPedestal(cellID) + 0.001));
    }

    /**
     * Converts a mode-1 digitized waveform into standard energy hit.
     * 
     * @param hit - The "hit" object representing the digitized waveform for a given
     *            crystal.
     * @return Returns a list of <code>CalorimeterHit</code> objects parsed from the
     *         waveform.
     */
    public List<CalorimeterHit> HitDtoA(RawTrackerHit hit) {
        // Get the cell ID for the crystal as well as the digitized
        // waveform samples.
        final long cellID = hit.getCellID();
        final short[] waveform = hit.getADCValues();

        // If there are no samples, then there is nothing to integrate.
        if (waveform.length == 0) {
            return null;
        }

        // The pulse integration threshold is defined as the combination
        // of the pedestal and the threshold configuration parameter.
        final int absoluteThreshold = (int) (config.getPedestal(cellID) + config.getThreshold(cellID));

        // Store each instance of a threshold crossing in that can be
        // found within the digitized waveform.
        List<Integer> thresholdCrossings = new ArrayList<Integer>();

        // Check for the special case of the first sample exceeding
        // the integration threshold.
        if (waveform[0] > absoluteThreshold) {
            thresholdCrossings.add(0);
        }

        // Search the remaining samples for threshold crossings.
        // Crossing sample is over threshold, and its previous sample is not over
        // threshold.
        // If a sample is over threshold, then check if any sample in the past 7 for
        // model 1 (or NSA - 1 sample for other models) is a crossing sample. If no, a
        // pulse is recorded.
        thresholdLoop: for (int sample = 1; sample < waveform.length; sample++) {
            if (waveform[sample] > absoluteThreshold && waveform[sample - 1] <= absoluteThreshold) {
                if (sample < 8) {
                    if (waveform[0] > absoluteThreshold)
                        continue thresholdLoop;
                    for (int pastSample = 1; pastSample < sample; pastSample++) {
                        if (waveform[pastSample] > absoluteThreshold && waveform[pastSample - 1] <= absoluteThreshold)
                            continue thresholdLoop;
                    }
                    thresholdCrossings.add(sample);
                } else {
                    for (int pastSample = sample - 7; pastSample < sample; pastSample++) {
                        if (waveform[pastSample] > absoluteThreshold && waveform[pastSample - 1] <= absoluteThreshold)
                            continue thresholdLoop;
                    }
                    thresholdCrossings.add(sample);
                }

                // If there is a limit defined on the maximum number
                // of peaks that may be processed, terminate the search
                // after this number of peaks have been found.
                if (thresholdCrossings.size() >= nPeak) {
                    break thresholdLoop;
                }
            }
        }

        // Use the previously located threshold crossing to generate
        // calorimeter hits.
        List<CalorimeterHit> newHits = new ArrayList<CalorimeterHit>();
        for (int thresholdCrossing : thresholdCrossings) {
            // Obtain the pedestal for the pulse.
            int pedestal = getPulsePedestal(cellID, waveform.length, thresholdCrossing);

            // Perform the pulse integral.
            final int[] data = convertWaveformToPulse(waveform, thresholdCrossing);
            int time = data[0];
            int sum = data[1];

            // Perform pedestal subtraction.
            sum -= pedestal;

            // Perform gain scaling.
            double energy = adcToEnergy(sum, cellID);

            // Hits should not have less than zero energy.
            if (energy < 0) {
                Logger.getGlobal().log(Level.INFO, "A hit was produced with " + energy + " energy!");
            }

            // Create a new hit and add it to the list.
            newHits.add(CalorimeterHitUtilities.create(energy, time, cellID));
        }

        // Return the list of hits.
        return newHits;
    }

    /**
     * Converts a value in ADC in a crystal to energy in units of TBD.
     * 
     * @param adcSum - The ADC value to convert.
     * @param cellID - The cell ID of the crystal containing the value.
     * @return Returns the ADC value as an energy in units of TBD.
     */
    private double adcToEnergy(int adcSum, long cellID) {
        // Define the gain. To mimic the hardware, this is done through
        // manipulation of integer values only. We multiply by 256 to
        // preserve extra digits of accuracy.
        int gain = (int) (256.0 * (config.getGain(cellID) + 0.001));

        // Multiply the gain by the pulse-subtracted energy sum. Also
        // remove the extra factor of 256.
        int energy = (int) ((adcSum * gain) / 256.0);

        // Return the final energy in units of TBD.
        return energy;
    }

    /**
     * Emulate the FADC250 firmware in conversion of Mode-1 waveform to a Mode-3/7
     * pulse, given a time for threshold crossing.
     */

    /**
     * Converts a mode-1 digitized waveform to a mode-3/7 pulse for a a given
     * threshold crossing.
     * 
     * @param waveform          - The digitized waveform. Each array value should
     *                          correspond to a sample of the waveform in units of
     *                          ADC.
     * @param thresholdCrossing - The time of the threshold crossing in samples.
     * @return Returns a <code>double</code> primitive of size 2. The first value
     *         represents the time in nanoseconds of the pulser and the second value
     *         the total integrated value of the pulse in ADC.
     */
    private int[] convertWaveformToPulse(short[] waveform, int thresholdCrossing) {
        // Store the integration range.
        int firstSample, lastSample;

        // If the integration range is larger than the number of samples,
        // then all the samples are used for pulse integration.
        if ((config.getNSA() + config.getNSB()) / nsPerSample >= waveform.length) {
            firstSample = 0;
            lastSample = waveform.length - 1;
        }

        // Otherwise, the integration range covers a number of samples
        // before and after the threshold crossing as defined by the
        // run parameters.
        else {
            firstSample = thresholdCrossing - config.getNSB() / nsPerSample;
            lastSample = thresholdCrossing + config.getNSA() / nsPerSample - 1;
        }

        // Perform the pulse integral.
        int sumADC = 0;
        integrationLoop: for (int sample = firstSample; sample <= lastSample; sample++) {
            // If the current sample occurs before the readout window,
            // then it does not exist and can not be integrated. Skip it.
            if (sample < 0) {
                continue integrationLoop;
            }

            // Likewise, samples that occur after the readout window are
            // not retained and must be skipped.
            if (sample >= waveform.length) {
                break integrationLoop;
            }

            // Otherwise, add the sample to the pulse total.
            sumADC += waveform[sample];
        }

        // Calculate the pulse time with a 4 nanosecond resolution.
        int pulseTime = thresholdCrossing * nsPerSample;

        // Return both the pulse time and the total integrated pulse ADC.
        return new int[] { pulseTime, sumADC };
    }

}