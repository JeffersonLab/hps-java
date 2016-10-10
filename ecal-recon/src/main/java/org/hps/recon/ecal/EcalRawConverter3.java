package org.hps.recon.ecal;

import hep.aida.IFitResult;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Map;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.ecal.EcalChannel;
import org.hps.conditions.ecal.EcalChannelConstants;
import org.hps.conditions.ecal.EcalConditions;
import org.hps.record.daqconfig.ConfigurationManager;
import org.hps.record.daqconfig.FADCConfig;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.event.RawCalorimeterHit;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.base.BaseRawCalorimeterHit;
import org.lcsim.geometry.Detector;

/**
 * This class is used to convert between {@link org.lcsim.event.RawCalorimeterHit} or
 * {@link org.lcsim.event.RawTrackerHit}, objects with ADC/sample information, and
 * {@link org.lcsim.event.CalorimeterHit}, an object with energy+time information. At minimum this involves pedestal
 * subtraction/addition and gain scaling. Knows how to deal with Mode-1/3/7 FADC readout formats. Can perform Mode-3/7
 * firmware algorithms on Mode-1 data. Can alternatively call pulse-fitting on Mode-1 data. All time walk/time offset
 * corrections are performed to this collection after gains in EcalTimeCorrectionDriver
 *
 * @author Sho Uemura <meeg@slac.stanford.edu>
 * @author Andrea Celentano <andrea.celentano@ge.infn.it>
 * @author Nathan Baltzell <baltzell@jlab.org>
 * @author Holly Szumila <hvanc001@odu.edu>
 */
public class EcalRawConverter3 {


    /**
     * If true, running pedestal is used.
     */
    private boolean useRunningPedestal = true;

    

    /**
     * If true, use the DAQ configuration from EVIO to set EcalRawConverter parameters. This should be removed to a
     * standalone EcalRawConverter solely for trigger emulation.
     */
    private boolean useDAQConfig = false;

    /**
     * The DAQ configuration from EVIO used to set EcalRawConverter parameters if useDAQConfig=true. This should be
     * removed to a standalone EcalRawConverter solely for trigger emulation.
     */
    private FADCConfig config = null;

    /**
     * Whether to use pulse fitting (EcalPulseFitter) to extract pulse energy time. Only applicable to Mode-1 data.
     */
    private boolean useFit = true;

    /**
     * The pulse fitter class.
     */
    private EcalPulseFitter pulseFitter = new EcalPulseFitter();

    /**
     * The time for one FADC sample (units = ns).
     */
    private static final int nsPerSample = 4;

    /**
     * The leading-edge threshold, relative to pedestal, for pulse-finding and time determination. Units = ADC. Used to
     * convert mode-1 readout into mode-3/7 used by clustering. The default value of 12 is what we used for most of the
     * 2014 run.
     */
    private double leadingEdgeThreshold = 12;

    /**
     * Integration range after (NSA) and before (NSB) threshold crossing. Units=ns, same as the DAQ configuration files.
     * These must be multiples of 4 ns. Used for pulse integration in Mode-1, and pedestal subtraction in all modes. The
     * default values of 20/100 are what we had during the entire 2014 run.
     */
    private int NSB = 20;
    private int NSA = 100;

    /**
     * The number of samples in the FADC readout window. Needed in order to properly pedestal-correct clipped pulses for
     * Mode-3/7. Ignored for mode-1 input, since it already knows its number of samples. A non-positive number disables
     * pulse-clipped pedestals and reverts to the old behavior which assumed integration range was constant.
     */
    private int windowSamples = -1;

    /**
     * The maximum number of peaks to be searched for. This is applicable only to Mode-1 data.
     */
    private int nPeak = 3;

    /**
     * Perform Mode-7 algorithm, else Mode-3. Only applicable to Mode-1 data.
     */
    private boolean mode7 = true;

    private EcalConditions ecalConditions = null;

    /**
     * Currently sets up a listener for DAQ configuration from EVIO. This should be removed to a standalone
     * ECalRawConverter solely for trigger emulation.
     */
    public EcalRawConverter3() {
        // Track changes in the DAQ configuration.
        ConfigurationManager.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                // If the DAQ configuration should be used, load the
                // relevant settings into the driver.
                if (useDAQConfig) {
                    // Get the FADC configuration.
                    config = ConfigurationManager.getInstance().getFADCConfig();

                    // Load the settings.
                    NSB = config.getNSB();
                    NSA = config.getNSA();
                    windowSamples = config.getWindowWidth() / 4;

                    // Get the number of peaks.
                    if (config.getMode() == 1) {
                        nPeak = Integer.MAX_VALUE;
                    } else {
                        nPeak = config.getMaxPulses();
                    }

                    // Print the FADC configuration.
                    System.out.println();
                    System.out.println();
                    System.out.printf("NSA            :: %d ns%n", NSA);
                    System.out.printf("NSB            :: %d ns%n", NSB);
                    System.out.printf("Window Samples :: %d clock-cycles%n", windowSamples);
                    System.out.printf("Max Peaks      :: %d peaks%n", nPeak);
                    System.out.println("======================================================================");
                    System.out.println("=== FADC Pulse-Processing Settings ===================================");
                    System.out.println("======================================================================");
                    config.printConfig(System.out);
                }
            }
        });
    }

    public void setUseFit(boolean useFit) {
        this.useFit = useFit;
    }

    public void setFixShapeParameter(boolean fix) {
        pulseFitter.fixShapeParameter = fix;
    }

    public void setGlobalFixedPulseWidth(double width) {
        pulseFitter.globalThreePoleWidth = width;
        pulseFitter.fixShapeParameter = true;
    }

    /**
     * Pulses with threshold crossing earlier than this will not be fit.
     */
    public void setFitThresholdTimeLo(int sample) {
        pulseFitter.threshRange[0] = sample;
    }

    /**
     * Pulses with threshold crossing time greater than this will not be fit.
     */
    public void setFitThresholdTimeHi(int sample) {
        pulseFitter.threshRange[1] = sample;
    }

    /**
     * Tell Minuit to limit pulse time parameter in fit to be greater than this.
     */
    public void setFitLimitTimeLo(int sample) {
        pulseFitter.t0limits[0] = sample;
    }

    /**
     * Tell Minuit to limit pulse time parameter in fit to be less than this.
     */
    public void setFitLimitTimeHi(int sample) {
        pulseFitter.t0limits[1] = sample;
    }

    /**
     * Set threshold for pulse finding. Units = ADC
     */
    public void setLeadingEdgeThreshold(double thresh) {
        leadingEdgeThreshold = thresh;
    }

    /**
     * Set number of samples after threshold crossing for pulse integration range.
     */
    public void setNSA(int nsa) {
        if (NSA % nsPerSample != 0 || NSA < 0) {
            throw new RuntimeException("NSA must be multiples of 4ns and non-negative.");
        }
        NSA = nsa;
    }

    /**
     * Set number of samples before threshold crossing for pulse integration range.
     */
    public void setNSB(int nsb) {
        if (NSB % nsPerSample != 0 || NSB < 0) {
            throw new RuntimeException("NSB must be multiples of 4ns and non-negative.");
        }
        NSB = nsb;
    }

    /**
     * Set number of samples in readout window. Used for pedestal subtraction for clipped pulses. This is ignored for
     * Mode-1 raw data, since Mode-1 knows its number of samples.
     */
    public void setWindowSamples(int windowSamples) {
        this.windowSamples = windowSamples;
    }

    /**
     * Set maximum number of pulses to search for in Mode-1 data.
     */
    public void setNPeak(int nPeak) {
        if (nPeak < 1 || nPeak > 3) {
            throw new RuntimeException("Npeak must be 1, 2, or 3.");
        }
        this.nPeak = nPeak;
    }

    /**
     * Set Mode-7 emulation on/off. If off, falls back to Mode-3.
     */
    public void setMode7(boolean mode7) {
        this.mode7 = mode7;
    }

 

    /**
     * Enables using running pedestals calculated on the fly from previous events. If false, uses 442 fixed pedestals
     * from the conditions system. Only applies to FADC Mode-1/7 input data formats.
     */
    public void setUseRunningPedestal(boolean useRunningPedestal) {
        this.useRunningPedestal = useRunningPedestal;
    }

   

    /**
     * Set whether to use DAQ configuration read from EVIO to set EcalRawConverter parameters. This should be removed to
     * a standalone EcalRawCongverterDriver solely for trigger emulation.
     */
    public void setUseDAQConfig(boolean state) {
        useDAQConfig = state;
    }



    

    /**
     * Get pedestal for a single ADC sample. Choose whether to use static pedestal from database or running pedestal
     * from mode-7.
     */
    public double getSingleSamplePedestal(EventHeader event, long cellID) {
        if (useDAQConfig) {
            // EcalChannel channel =
            // ecalConditions.getChannelCollection().findGeometric(cellID);
            return config.getPedestal(cellID);
        }
        if (useRunningPedestal && event != null) {
            if (event.hasItem("EcalRunningPedestals")) {
                @SuppressWarnings("unchecked")
                Map<EcalChannel, Double> runningPedMap = (Map<EcalChannel, Double>) event.get("EcalRunningPedestals");
                EcalChannel chan = ecalConditions.getChannelCollection().findGeometric(cellID);
                if (!runningPedMap.containsKey(chan)) {
                    System.err.println("************** Missing Pedestal");
                } else {
                    return runningPedMap.get(chan);
                }
            } else {
                System.err.println("*****************************************************************");
                System.err.println("**  You Requested a Running Pedestal, but it is NOT available. **");
                System.err.println("**     Reverting to the database. Only printing this ONCE.     **");
                System.err.println("*****************************************************************");
                useRunningPedestal = false;
            }
        }
        return findChannel(cellID).getCalibration().getPedestal();
    }

    /**
     * Get pedestal for entire pulse integral. Account for clipping if windowSamples is greater than zero.
     */
    public double getPulsePedestal(EventHeader event, long cellID, int windowSamples, int thresholdCrossing) {
        int firstSample, lastSample;
        if (windowSamples > 0 && (NSA + NSB) / nsPerSample >= windowSamples) {
            // special case where firmware always integrates entire window
            firstSample = 0;
            lastSample = windowSamples - 1;
        } else {
            firstSample = thresholdCrossing - NSB / nsPerSample;
            lastSample = thresholdCrossing + NSA / nsPerSample - 1;
            if (windowSamples > 0) {
                // properly pedestal subtract pulses clipped by edge(s) of
                // readout window:
                if (firstSample < 0)
                    firstSample = 0;
                if (lastSample >= windowSamples)
                    lastSample = windowSamples - 1;
            }
        }
        return (lastSample - firstSample + 1) * getSingleSamplePedestal(event, cellID);
    }

    /**
     * Emulate the FADC250 firmware in conversion of Mode-1 waveform to a Mode-3/7 pulse, given a time for threshold
     * crossing.
     */
    public double[] convertWaveformToPulse(RawTrackerHit hit, int thresholdCrossing, boolean mode7) {

        double fitQuality = -1;

        short samples[] = hit.getADCValues();
        // System.out.println("NewEvent");
        // choose integration range:
        int firstSample, lastSample;
        if ((NSA + NSB) / nsPerSample >= samples.length) {
            // firmware treats this case specially:
            firstSample = 0;
            lastSample = samples.length - 1;
        } else {
            firstSample = thresholdCrossing - NSB / nsPerSample;
            lastSample = thresholdCrossing + NSA / nsPerSample - 1;
        }

        // mode-7's minimum/pedestal (average of first 4 samples):
        double minADC = 0;
        for (int jj = 0; jj < 4; jj++)
            minADC += samples[jj];
        // does the firmware's conversion of min to int occur before or after
        // time calculation? undocumented.
        // minADC=(int)(minADC/4);
        minADC = (minADC / 4);

        // System.out.println("Avg pedestal:\t"+minADC);

        // mode-7's max pulse height:
        double maxADC = 0;
        // int sampleMaxADC=0;

        // mode-3/7's pulse integral:
        double sumADC = 0;

        for (int jj = firstSample; jj <= lastSample; jj++) {

            if (jj < 0)
                continue;
            if (jj >= samples.length)
                break;

            // integrate pulse:
            sumADC += samples[jj];
        }

        // find pulse maximum:
        // if (jj>firstSample && jj<samples.length-5) { // The "5" here is a
        // firmware constant.
        for (int jj = thresholdCrossing; jj < samples.length - 5; jj++) { // The
                                                                          // "5"
                                                                          // here
                                                                          // is
                                                                          // a
                                                                          // firmware
                                                                          // constant.
            if (samples[jj + 1] < samples[jj]) {
                // sampleMaxADC=jj;
                maxADC = samples[jj];
                break;
            }
        }

        // pulse time with 4ns resolution:
        double pulseTime = thresholdCrossing * nsPerSample;

        // calculate Mode-7 high-resolution time:
        if (mode7) {
            if (thresholdCrossing < 4) {
                // special case where firmware sets max to zero and time to 4ns
                // time.
                maxADC = 0;
            } else if (maxADC > 0) {
                // linear interpolation between threshold crossing and
                // pulse maximum to find time at pulse half-height:

                final double halfMax = (maxADC + minADC) / 2;
                int t0 = -1;
                for (int ii = thresholdCrossing - 1; ii < lastSample; ii++) {
                    if (ii >= samples.length - 1)
                        break;
                    if (samples[ii] <= halfMax && samples[ii + 1] > halfMax) {
                        t0 = ii;
                        break;
                    }
                }
                if (t0 > 0) {
                    final int t1 = t0 + 1;
                    final int a0 = samples[t0];
                    final int a1 = samples[t1];
                    // final double slope = (a1 - a0); // units = ADC/sample
                    // final double yint = a1 - slope * t1; // units = ADC
                    pulseTime = ((halfMax - a0) / (a1 - a0) + t0) * nsPerSample;
                }
            }
        }

        if (useFit) {
            IFitResult fitResult = pulseFitter.fitPulse(hit, thresholdCrossing, maxADC);
            if (fitResult != null) {
                fitQuality = fitResult.quality();
                if (fitQuality > 0) {
                    pulseTime = fitResult.fittedParameter("time0") * nsPerSample;
                    sumADC = fitResult.fittedParameter("integral");
                    minADC = fitResult.fittedParameter("pedestal");
                    maxADC = ((Ecal3PoleFunction) fitResult.fittedFunction()).maximum();
                }
            }
        }

        return new double[] {pulseTime, sumADC, minADC, maxADC, fitQuality};
    }

    /**
     * This HitDtoA is for emulating the conversion of Mode-1 readout (RawTrackerHit) into what EcalRawConverter would
     * have created from a Mode-3 or Mode-7 readout. Clustering classes will read the resulting CalorimeterHits same as
     * if they were directly readout from the FADCs in Mode-3/7. For Mode-3, hit time is just the time of threshold
     * crossing, with an optional time-walk correction. For Mode-7, it is a "high-resolution" one calculated by linear
     * interpolation between threshold crossing and pulse maximum. TODO: Generate GenericObject (and corresponding
     * LCRelation) to store min and max to fully emulate mode-7. This is less important for now.
     */
    public ArrayList<CalorimeterHit> HitDtoA(EventHeader event, RawTrackerHit hit) {
        final long cellID = hit.getCellID();
        final short samples[] = hit.getADCValues();
        if (samples.length == 0)
            return null;

        // threshold is pedestal plus threshold configuration parameter:
        final int absoluteThreshold;
        if (useDAQConfig) {
            // EcalChannel channel =
            // ecalConditions.getChannelCollection().findGeometric(hit.getCellID());
            // int leadingEdgeThreshold =
            // ConfigurationManager.getInstance().getFADCConfig().getThreshold(channel.getChannelId());
            int leadingEdgeThreshold = config.getThreshold(cellID);
            absoluteThreshold = (int) (getSingleSamplePedestal(event, cellID) + leadingEdgeThreshold);
        } else {
            absoluteThreshold = (int) (getSingleSamplePedestal(event, cellID) + leadingEdgeThreshold);
        }

        ArrayList<Integer> thresholdCrossings = new ArrayList<Integer>();

        // special case, first sample is above threshold:
        if (samples[0] > absoluteThreshold) {
            thresholdCrossings.add(0);
        }

        // search for threshold crossings:
        for (int ii = 1; ii < samples.length; ++ii) {
            if (samples[ii] > absoluteThreshold && samples[ii - 1] <= absoluteThreshold) {

                // found one:
                thresholdCrossings.add(ii);

                // search for next threshold crossing begins at end of this
                // pulse:
                if (useDAQConfig && ConfigurationManager.getInstance().getFADCConfig().getMode() == 1) {
                    // special case, emulating SSP:
                    ii += 8;
                } else {
                    // "normal" case, emulating FADC250:
                    ii += NSA / nsPerSample - 1;
                }

                // firmware limit on # of peaks:
                if (thresholdCrossings.size() >= nPeak)
                    break;
            }
        }

        // make hits
        ArrayList<CalorimeterHit> newHits = new ArrayList<CalorimeterHit>();
        for (int thresholdCrossing : thresholdCrossings) {
            // do pulse integral:
            final double[] data = convertWaveformToPulse(hit, thresholdCrossing, mode7);
            double time = data[0];
            double sum = data[1];
            // final double min = data[2]; // TODO: stick min and max in a
            // GenericObject with an
            // final double max = data[3]; // LCRelation to finish mode-7
            // emulation
            final double fitQuality = data[4];

            if (!useFit || fitQuality <= 0) {
                // do pedestal subtraction:
                sum -= getPulsePedestal(event, cellID, samples.length, thresholdCrossing);
            }

            // do gain scaling using a dummy gain.  
            

            newHits.add(CalorimeterHitUtilities.create(sum, time, cellID));
        }

        return newHits;
    }

   

    /**
     * This HitDtoA is for Mode-3 data. A time-walk correction can be applied.
     */
    public CalorimeterHit HitDtoA(EventHeader event, RawCalorimeterHit hit, double timeOffset) {
        if (hit.getTimeStamp() % 64 != 0) {
            System.out.println("unexpected timestamp " + hit.getTimeStamp());
        }
        double time = hit.getTimeStamp() / 16.0;
        long id = hit.getCellID();
        double pedestal = getPulsePedestal(event, id, windowSamples, (int) time / nsPerSample);
        double adcSum = hit.getAmplitude() - pedestal;
        //double rawEnergy = adcToEnergy(adcSum);
        
        return CalorimeterHitUtilities.create(adcSum, time + timeOffset, id);
    }

    /**
     * This HitDtoA is exclusively for Mode-7 data, hence the GenericObject parameter.
     */
    public CalorimeterHit HitDtoA(EventHeader event, RawCalorimeterHit hit, GenericObject mode7Data, double timeOffset) {
        double time = hit.getTimeStamp() / 16.0; // timestamps use the full 62.5
                                                 // ps resolution
        long id = hit.getCellID();
        double pedestal = getPulsePedestal(event, id, windowSamples, (int) time / nsPerSample);
        double adcSum = hit.getAmplitude() - pedestal;
        //double rawEnergy = adcToEnergy(adcSum);
        return CalorimeterHitUtilities.create(adcSum, time + timeOffset, id);
    }

    /**
     * This converts a corrected pulse integral (pedestal-subtracted and gain-scaled) back into raw pulse integral with
     * units ADC.
     */
    public RawCalorimeterHit HitAtoD(CalorimeterHit hit) {
        int time = (int) (Math.round(hit.getTime() / 4.0) * 64.0);
        long id = hit.getCellID();
        // Get the channel data.
        //EcalChannelConstants channelData = findChannel(id);
        int amplitude;
        double pedestal = getPulsePedestal(null, id, windowSamples, (int) hit.getTime() / nsPerSample);
        amplitude = (int) Math.round((hit.getRawEnergy() / EcalUtils.MeV) / (EcalUtils.gainFactor * EcalUtils.ecalReadoutPeriod) + pedestal);
       
        // time += findChannel(id).getTimeShift().getTimeShift();
        RawCalorimeterHit h = new BaseRawCalorimeterHit(id, amplitude, time);
        return h;
    }

    /**
     * This should probably be deprecated. HitDtoA(EventHeader,RawTrackerHit) has the same functionality if NSA+NSB >
     * windowSamples, with the exception that that one also finds pulse time instead of this one's always reporting
     * zero.
     */
    public CalorimeterHit HitDtoA(RawTrackerHit hit) {
        double time = hit.getTime();
        long id = hit.getCellID();
        double adcSum = sumADC(hit);
        return CalorimeterHitUtilities.create(adcSum, time, id);
    }
    
    /**
     * Integrate the entire window. Return pedestal-subtracted integral.
     */
    public int sumADC(RawTrackerHit hit) {
        EcalChannelConstants channelData = findChannel(hit.getCellID());
        double pedestal;
        if (useDAQConfig) {
            // EcalChannel channel =
            // ecalConditions.getChannelCollection().findGeometric(hit.getCellID());
            pedestal = config.getPedestal(hit.getCellID());
        } else {
            pedestal = channelData.getCalibration().getPedestal();
        }

        int sum = 0;
        short samples[] = hit.getADCValues();
        for (int isample = 0; isample < samples.length; ++isample) {
            sum += (samples[isample] - pedestal);
        }
        return sum;
    }

    /**
     * Must be set when an object EcalRawConverter is created.
     *
     * @param detector (long)
     */
    public void setDetector(Detector detector) {
        // ECAL combined conditions object.
        ecalConditions = DatabaseConditionsManager.getInstance().getEcalConditions();
        pulseFitter.setDetector(detector);
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
     
}
