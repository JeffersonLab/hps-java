package org.hps.readout.ecal;

import static org.hps.recon.ecal.EcalUtils.ecalReadoutPeriod;
import static org.hps.recon.ecal.EcalUtils.fallTime;
import static org.hps.recon.ecal.EcalUtils.maxVolt;
import static org.hps.recon.ecal.EcalUtils.nBit;
import static org.hps.recon.ecal.EcalUtils.readoutGain;
import static org.hps.recon.ecal.EcalUtils.riseTime;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.ecal.EcalChannelConstants;
import org.hps.conditions.ecal.EcalConditions;
import org.hps.recon.ecal.EcalUtils;
import org.hps.util.RandomGaussian;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawCalorimeterHit;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.base.BaseRawCalorimeterHit;
import org.lcsim.event.base.BaseRawTrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.Subdetector;
import org.lcsim.geometry.subdetector.HPSEcal3;
import org.lcsim.lcio.LCIOConstants;

/**
 * Performs readout of ECal hits. Simulates time evolution of preamp output
 * pulse.
 *
 * @author Sho Uemura <meeg@slac.stanford.edu>
 * @version $Id: FADCEcalReadoutDriver.java,v 1.4 2013/10/31 00:11:02 meeg Exp $
 */
public class FADCEcalReadoutDriver extends EcalReadoutDriver<RawCalorimeterHit> {

    // Repeated here from EventConstants in evio module to avoid depending on it.
    private static final int ECAL_RAW_MODE = 1;
    private static final int ECAL_PULSE_MODE = 2;
    private static final int ECAL_PULSE_INTEGRAL_MODE = 3;
    private String ecalName = "Ecal";
    private Subdetector ecal;
    private EcalConditions ecalConditions = null;
    //buffer for preamp signals (units of volts, no pedestal)
    private Map<Long, RingBuffer> analogPipelines = null;
    //ADC pipeline for readout (units of ADC counts)
    private Map<Long, FADCPipeline> digitalPipelines = null;
    //buffer for window sums
    private Map<Long, Integer> triggerPathHitSums = null;
    //buffer for timestamps
    private Map<Long, Integer> triggerPathHitTimes = null;
    //queue for hits to be output to clusterer
    private PriorityQueue<RawCalorimeterHit> triggerPathDelayQueue = null;
    //output buffer for hits
    private LinkedList<RawCalorimeterHit> triggerPathCoincidenceQueue = new LinkedList<RawCalorimeterHit>();
    private int bufferLength = 100;
    private int pipelineLength = 2000;
    private double tp = 9.6;
    private int delay0 = 32;
    private int readoutLatency = 100;
    private int readoutWindow = 100;
    private int numSamplesBefore = 5;
    private int numSamplesAfter = 30;
    private int coincidenceWindow = 2;
    private String ecalReadoutCollectionName = "EcalReadoutHits";
    private int mode = ECAL_PULSE_INTEGRAL_MODE;
    private int readoutThreshold = 10;
    private int triggerThreshold = 10;
    private double scaleFactor = 1;
    private double fixedGain = -1;
    private boolean constantTriggerWindow = true;
    private boolean addNoise = false;
    private double pePerMeV = 32.8;
    private boolean use2014Gain = false;
    private PulseShape pulseShape = PulseShape.ThreePole;

    public enum PulseShape {

        CRRC, DoubleGaussian, ThreePole
    }

    public FADCEcalReadoutDriver() {
        flags = 0;
        flags += 1 << LCIOConstants.RCHBIT_TIME; //store timestamp
        hitClass = RawCalorimeterHit.class;
        setReadoutPeriod(ecalReadoutPeriod);
//        converter = new HPSEcalConverter(null);
    }

    /**
     * Add noise (photoelectron statistics and readout/preamp noise) to hits
     * before adding them to the analog pipeline.
     *
     * @param addNoise True to add noise, default of false.
     */
    public void setAddNoise(boolean addNoise) {
        this.addNoise = addNoise;
    }

    /**
     * Sets the trigger-path hit processing algorithm.
     *
     * @param constantTriggerWindow True for 2014+ FADC behavior, false for test
     * run behavior. True by default.
     */
    public void setConstantTriggerWindow(boolean constantTriggerWindow) {
        this.constantTriggerWindow = constantTriggerWindow;
    }

    /**
     * Override the ECal gains set in the conditions system with a single
     * uniform value.
     *
     * @param fixedGain Units of MeV/(ADC counts in pulse integral). Negative
     * value causes the conditions system to be used for gains. Default is
     * negative.
     */
    public void setFixedGain(double fixedGain) {
        this.fixedGain = fixedGain;
    }

    public void setEcalName(String ecalName) {
        this.ecalName = ecalName;
    }

    /**
     * Threshold for readout-path hits. For 2014+ running this should always
     * equal the trigger threshold.
     *
     * @param readoutThreshold Units of ADC counts, default of 10.
     */
    public void setReadoutThreshold(int readoutThreshold) {
        this.readoutThreshold = readoutThreshold;
    }

    /**
     * Scale factor for trigger-path hit amplitudes. Only used for test run.
     *
     * @param scaleFactor Default of 1.
     */
    public void setScaleFactor(double scaleFactor) {
        this.scaleFactor = scaleFactor;
    }

    /**
     * Threshold for trigger-path hits. For 2014+ running this should always
     * equal the readout threshold.
     *
     * @param triggerThreshold Units of ADC counts, default of 10.
     */
    public void setTriggerThreshold(int triggerThreshold) {
        this.triggerThreshold = triggerThreshold;
    }

    /**
     * Output collection name for readout-path hits.
     *
     * @param ecalReadoutCollectionName
     */
    public void setEcalReadoutCollectionName(String ecalReadoutCollectionName) {
        this.ecalReadoutCollectionName = ecalReadoutCollectionName;
    }

    /**
     * Number of ADC samples to process after each rising threshold crossing. In
     * FADC documentation, "number of samples after" or NSA.
     *
     * @param numSamplesAfter Units of 4 ns FADC clock cycles, default of 30.
     */
    public void setNumSamplesAfter(int numSamplesAfter) {
        this.numSamplesAfter = numSamplesAfter;
    }

    /**
     * Number of ADC samples to process before each rising threshold crossing.
     * In FADC documentation, "number of samples before" or NSB.
     *
     * @param numSamplesBefore Units of 4 ns FADC clock cycles, default of 5.
     */
    public void setNumSamplesBefore(int numSamplesBefore) {
        this.numSamplesBefore = numSamplesBefore;
    }

    /**
     * Start of readout window relative to trigger time (in readout cycles). In
     * FADC documentation, "Programmable Latency" or PL.
     *
     * @param readoutLatency Units of 4 ns FADC clock cycles, default of 100.
     */
    public void setReadoutLatency(int readoutLatency) {
        this.readoutLatency = readoutLatency;
    }

    /**
     * Number of ADC samples to read out. In FADC documentation, "Programmable
     * Trigger Window" or PTW.
     *
     * @param readoutWindow Units of 4 ns FADC clock cycles, default of 100.
     */
    public void setReadoutWindow(int readoutWindow) {
        this.readoutWindow = readoutWindow;
    }

    /**
     * Number of clock cycles for which the same trigger-path hit is sent to the
     * clusterer. Only used for old clusterer simulations (CTPClusterDriver).
     * Otherwise this should be set to 1.
     *
     * @param coincidenceWindow Units of 4 ns FADC clock cycles, default of 1.
     */
    public void setCoincidenceWindow(int coincidenceWindow) {
        this.coincidenceWindow = coincidenceWindow;
    }

    /**
     * Switch between test run and 2014 definitions of gain constants. True for
     * MC studies and mock data in 2014. For all real data (test run and 2014+),
     * test run MC, and 2015+ production MC, this should be false.
     *
     *
     * @param use2014Gain True ONLY for simulation studies in 2014. Default of
     * false.
     */
    public void setUse2014Gain(boolean use2014Gain) {
        this.use2014Gain = use2014Gain;
    }

    /**
     * Model used for the preamp pulse shape.
     *
     * @param pulseShape ThreePole, DoubleGaussian, or CRRC. Default is
     * ThreePole.
     */
    public void setPulseShape(String pulseShape) {
        this.pulseShape = PulseShape.valueOf(pulseShape);
    }

    /**
     * Shaper time constant. Definition depends on the pulse shape. For the
     * three-pole function, this is equal to RC, or half the peaking time.
     *
     * @param tp Units of ns, default of 9.6.
     */
    public void setTp(double tp) {
        this.tp = tp;
    }

    /**
     * Photoelectrons per MeV, used to calculate noise due to photoelectron
     * statistics. Test run detector had a value of 2 photoelectrons/MeV; new
     * 2014 detector has a value of 32.8 photoelectrons/MeV.
     *
     * @param pePerMeV Units of photoelectrons/MeV, default of 32.8.
     */
    public void setPePerMeV(double pePerMeV) {
        this.pePerMeV = pePerMeV;
    }

    /**
     * Latency between threshold crossing and output of trigger-path hit to
     * clusterer.
     *
     * @param delay0 Units of 4 ns FADC clock cycles, default of 32.
     */
    public void setDelay0(int delay0) {
        this.delay0 = delay0;
    }

    /**
     * Length of analog pipeline.
     *
     * @param bufferLength Units of 4 ns FADC clock cycles, default of 100.
     */
    public void setBufferLength(int bufferLength) {
        this.bufferLength = bufferLength;
        resetFADCBuffers();
    }

    /**
     * Length of digital pipeline. The digital pipeline in the FADC is 2000
     * cells long.
     *
     * @param pipelineLength Units of 4 ns FADC clock cycles, default of 2000.
     */
    public void setPipelineLength(int pipelineLength) {
        this.pipelineLength = pipelineLength;
        resetFADCBuffers();
    }

    /**
     * Mode for readout-path hits.
     *
     * @param mode 1, 2 or 3. Values correspond to the standard FADC mode
     * numbers (1=raw, 2=pulse, 3=pulse integral).
     */
    public void setMode(int mode) {
        this.mode = mode;
        if (mode != ECAL_RAW_MODE && mode != ECAL_PULSE_MODE && mode != ECAL_PULSE_INTEGRAL_MODE) {
            throw new IllegalArgumentException("invalid mode " + mode);
        }
    }

    /**
     * Return the map of preamp signal buffers. For debug only.
     *
     * @return
     */
    public Map<Long, RingBuffer> getSignalMap() {
        return analogPipelines;
    }

    /**
     * Return the map of FADC pipelines. For debug only.
     *
     * @return
     */
    public Map<Long, FADCPipeline> getPipelineMap() {
        return digitalPipelines;
    }

    /**
     * Digitize values in the analog pipelines and append them to the digital
     * pipelines. Integrate trigger-path hits and add them to the trigger path
     * queues. Read out trigger-path hits to the list sent to the clusterer.
     *
     * @param hits List to be filled by this method.
     */
    @Override
    protected void readHits(List<RawCalorimeterHit> hits) {

        for (Long cellID : analogPipelines.keySet()) {
            RingBuffer signalBuffer = analogPipelines.get(cellID);

            FADCPipeline pipeline = digitalPipelines.get(cellID);
            pipeline.step();

            // Get the channel data.
            EcalChannelConstants channelData = findChannel(cellID);

            double currentValue = signalBuffer.currentValue() * ((Math.pow(2, nBit) - 1) / maxVolt); //12-bit ADC with maxVolt V range
            int pedestal = (int) Math.round(channelData.getCalibration().getPedestal());
            int digitizedValue = Math.min((int) Math.round(pedestal + currentValue), (int) Math.pow(2, nBit)); //ADC can't return a value larger than 4095; 4096 (overflow) is returned for any input >2V
            pipeline.writeValue(digitizedValue);
            int pedestalSubtractedValue = digitizedValue - pedestal;
            //System.out.println(signalBuffer.currentValue() + "   " + currentValue + "   " + pipeline.currentValue());

            Integer sum = triggerPathHitSums.get(cellID);
            if (sum == null && pedestalSubtractedValue > triggerThreshold) {
                triggerPathHitTimes.put(cellID, readoutCounter);
                if (constantTriggerWindow) {
                    int sumBefore = 0;
                    for (int i = 0; i < numSamplesBefore; i++) {
                        if (debug) {
                            System.out.format("trigger %d, %d: %d\n", cellID, i, pipeline.getValue(numSamplesBefore - i - 1));
                        }
                        sumBefore += pipeline.getValue(numSamplesBefore - i - 1);
                    }
                    triggerPathHitSums.put(cellID, sumBefore);
                } else {
                    triggerPathHitSums.put(cellID, pedestalSubtractedValue);
                }
            }
            if (sum != null) {
                if (constantTriggerWindow) {
                    if (triggerPathHitTimes.get(cellID) + numSamplesAfter >= readoutCounter) {
                        if (debug) {
                            System.out.format("trigger %d, %d: %d\n", cellID, readoutCounter - triggerPathHitTimes.get(cellID) + numSamplesBefore - 1, pipeline.getValue(0));
                        }
                        triggerPathHitSums.put(cellID, sum + pipeline.getValue(0));
                    } else if (triggerPathHitTimes.get(cellID) + delay0 <= readoutCounter) {
//                        System.out.printf("sum = %f\n", sum);
                        triggerPathDelayQueue.add(new BaseRawCalorimeterHit(cellID,
                                (int) Math.round(sum / scaleFactor),
                                64 * triggerPathHitTimes.get(cellID)));
                        triggerPathHitSums.remove(cellID);
                    }
                } else {
                    if (pedestalSubtractedValue < triggerThreshold || triggerPathHitTimes.get(cellID) + delay0 == readoutCounter) {
//					System.out.printf("sum = %f\n",sum);
                        triggerPathDelayQueue.add(new BaseRawCalorimeterHit(cellID,
                                (int) Math.round((sum + pedestalSubtractedValue) / scaleFactor),
                                64 * triggerPathHitTimes.get(cellID)));
                        triggerPathHitSums.remove(cellID);
                    } else {
                        triggerPathHitSums.put(cellID, sum + pedestalSubtractedValue);
                    }
                }
            }
            signalBuffer.step();
        }
        while (triggerPathDelayQueue.peek() != null && triggerPathDelayQueue.peek().getTimeStamp() / 64 <= readoutCounter - delay0) {
            if (triggerPathDelayQueue.peek().getTimeStamp() / 64 < readoutCounter - delay0) {
                System.out.println(this.getName() + ": Stale hit in output queue");
                triggerPathDelayQueue.poll();
            } else {
                triggerPathCoincidenceQueue.add(triggerPathDelayQueue.poll());
            }
        }
        while (!triggerPathCoincidenceQueue.isEmpty() && triggerPathCoincidenceQueue.peek().getTimeStamp() / 64 <= readoutCounter - delay0 - coincidenceWindow) {
            triggerPathCoincidenceQueue.remove();
        }
        if (debug) {
            for (RawCalorimeterHit hit : triggerPathCoincidenceQueue) {
                System.out.format("new hit: energy %d\n", hit.getAmplitude());
            }
        }

        hits.addAll(triggerPathCoincidenceQueue);
    }

    @Override
    public void startOfData() {
        super.startOfData();
        if (ecalReadoutCollectionName == null) {
            throw new RuntimeException("The parameter ecalReadoutCollectionName was not set!");
        }
    }

    @Override
    protected void processTrigger(EventHeader event) {
        switch (mode) {
            case ECAL_RAW_MODE:
                if (debug) {
                    System.out.println("Reading out ECal in raw mode");
                }
                event.put(ecalReadoutCollectionName, readWindow(), RawTrackerHit.class, 0, ecalReadoutName);
                break;
            case ECAL_PULSE_MODE:
                if (debug) {
                    System.out.println("Reading out ECal in pulse mode");
                }
                event.put(ecalReadoutCollectionName, readPulses(), RawTrackerHit.class, 0, ecalReadoutName);
                break;
            case ECAL_PULSE_INTEGRAL_MODE:
                if (debug) {
                    System.out.println("Reading out ECal in integral mode");
                }
                event.put(ecalReadoutCollectionName, readIntegrals(), RawCalorimeterHit.class, flags, ecalReadoutName);
                break;
        }
    }

    @Override
    public double readoutDeltaT() {
        double triggerTime = ClockSingleton.getTime() + triggerDelay;
        int cycle = (int) Math.floor((triggerTime - readoutOffset + ClockSingleton.getDt()) / readoutPeriod);
        double readoutTime = (cycle - readoutLatency) * readoutPeriod + readoutOffset - ClockSingleton.getDt();
        return readoutTime;
    }

    protected short[] getWindow(long cellID) {
        FADCPipeline pipeline = digitalPipelines.get(cellID);
        short[] adcValues = new short[readoutWindow];
        for (int i = 0; i < readoutWindow; i++) {
            adcValues[i] = (short) pipeline.getValue(readoutLatency - i - 1);
//			if (adcValues[i] != 0) {
//				System.out.println("getWindow: " + adcValues[i] + " at i = " + i);
//			}
        }
        return adcValues;
    }

    protected List<RawTrackerHit> readWindow() {
//		System.out.println("Reading FADC data");
        List<RawTrackerHit> hits = new ArrayList<RawTrackerHit>();
        for (Long cellID : digitalPipelines.keySet()) {
            short[] adcValues = getWindow(cellID);
            EcalChannelConstants channelData = findChannel(cellID);
            boolean isAboveThreshold = false;
            for (int i = 0; i < adcValues.length; i++) {
                if (adcValues[i] > channelData.getCalibration().getPedestal() + readoutThreshold) {
                    isAboveThreshold = true;
                    break;
                }
            }
            if (isAboveThreshold) {
                hits.add(new BaseRawTrackerHit(cellID, 0, adcValues));
            }
        }
        return hits;
    }

    protected List<RawTrackerHit> readPulses() {
//		System.out.println("Reading FADC data");
        List<RawTrackerHit> hits = new ArrayList<RawTrackerHit>();
        for (Long cellID : digitalPipelines.keySet()) {
            short[] window = getWindow(cellID);
            short[] adcValues = null;
            int pointerOffset = 0;
            int numSamplesToRead = 0;
            int thresholdCrossing = 0;

            // Get the channel data.
            EcalChannelConstants channelData = findChannel(cellID);

            for (int i = 0; i < readoutWindow; i++) {
                if (numSamplesToRead != 0) {
                    if (adcValues == null) {
                        throw new RuntimeException("expected a pulse buffer, none found (this should never happen)");
                    }
                    adcValues[adcValues.length - numSamplesToRead] = window[i - pointerOffset];
                    numSamplesToRead--;
                    if (numSamplesToRead == 0) {
                        hits.add(new BaseRawTrackerHit(cellID, thresholdCrossing, adcValues));
                    }
                } else if ((i == 0 || window[i - 1] <= channelData.getCalibration().getPedestal() + readoutThreshold) && window[i] > channelData.getCalibration().getPedestal() + readoutThreshold) {
                    thresholdCrossing = i;
                    pointerOffset = Math.min(numSamplesBefore, i);
                    numSamplesToRead = pointerOffset + Math.min(numSamplesAfter, readoutWindow - i - pointerOffset - 1);
                    adcValues = new short[numSamplesToRead];
                }
            }
        }
        return hits;
    }

    protected List<RawCalorimeterHit> readIntegrals() {
//		System.out.println("Reading FADC data");
        List<RawCalorimeterHit> hits = new ArrayList<RawCalorimeterHit>();
        for (Long cellID : digitalPipelines.keySet()) {
            short[] window = getWindow(cellID);
            int adcSum = 0;
            int pointerOffset = 0;
            int numSamplesToRead = 0;
            int thresholdCrossing = 0;

            // Get the channel data.
            EcalChannelConstants channelData = findChannel(cellID);

            if (window != null) {
                for (int i = 0; i < readoutWindow; i++) {
                    if (numSamplesToRead != 0) {
                        if (debug) {
                            System.out.format("readout %d, %d: %d\n", cellID, numSamplesBefore + numSamplesAfter - numSamplesToRead, window[i - pointerOffset]);
                        }
                        adcSum += window[i - pointerOffset];
                        numSamplesToRead--;
                        if (numSamplesToRead == 0) {
                            hits.add(new BaseRawCalorimeterHit(cellID, adcSum, 64 * thresholdCrossing));
                        }
                    } else if ((i == 0 || window[i - 1] <= channelData.getCalibration().getPedestal() + readoutThreshold) && window[i] > channelData.getCalibration().getPedestal() + readoutThreshold) {
                        thresholdCrossing = i;
                        pointerOffset = Math.min(numSamplesBefore, i);
                        numSamplesToRead = pointerOffset + Math.min(numSamplesAfter, readoutWindow - i - pointerOffset - 1);
                        adcSum = 0;
                    }
                }
            }
        }
        return hits;
    }

    /**
     * Fill the analog pipelines with the preamp pulses generated by hits in the
     * ECal.
     *
     * @param hits ECal hits from SLIC/Geant4.
     */
    @Override
    protected void putHits(List<CalorimeterHit> hits) {
        //fill the readout buffers
        for (CalorimeterHit hit : hits) {
            RingBuffer eDepBuffer = analogPipelines.get(hit.getCellID());
            double energyAmplitude = hit.getRawEnergy();
            // Get the channel data.
            EcalChannelConstants channelData = findChannel(hit.getCellID());
            if (addNoise) {
                //add preamp noise and photoelectron Poisson noise in quadrature
                double noise;
                if (use2014Gain) {
                    noise = Math.sqrt(Math.pow(channelData.getCalibration().getNoise() * channelData.getGain().getGain() * EcalUtils.gainFactor * EcalUtils.ecalReadoutPeriod, 2)
                            + hit.getRawEnergy() / (EcalUtils.lightYield * EcalUtils.quantumEff * EcalUtils.surfRatio));
                } else {
                    noise = Math.sqrt(Math.pow(channelData.getCalibration().getNoise() * channelData.getGain().getGain() * EcalUtils.MeV, 2)
                            + hit.getRawEnergy() * EcalUtils.MeV / pePerMeV);
                }
                energyAmplitude += RandomGaussian.getGaussian(0, noise);
            }
            if ((1) * readoutPeriod + readoutTime() - (ClockSingleton.getTime() + hit.getTime()) >= readoutPeriod) {
                throw new RuntimeException("trying to add a hit to the analog pipeline, but time seems incorrect");
            }
            for (int i = 0; i < bufferLength; i++) {
                eDepBuffer.addToCell(i, energyAmplitude * pulseAmplitude((i + 1) * readoutPeriod + readoutTime() - (ClockSingleton.getTime() + hit.getTime()), hit.getCellID()));
            }
        }
    }

    @Override
    protected void initReadout() {
        //initialize buffers
        triggerPathHitSums = new HashMap<Long, Integer>();
        triggerPathHitTimes = new HashMap<Long, Integer>();
        triggerPathDelayQueue = new PriorityQueue(20, new TimeComparator());
        resetFADCBuffers();
    }

    @Override
    public void detectorChanged(Detector detector) {
        // Get the Subdetector.
        ecal = detector.getSubdetector(ecalName);

        // ECAL combined conditions object.
        ecalConditions = DatabaseConditionsManager.getInstance().getEcalConditions();

        resetFADCBuffers();
    }

    private boolean resetFADCBuffers() {
        if (ecal == null) {
            return false;
        }
        analogPipelines = new HashMap<Long, RingBuffer>();
        digitalPipelines = new HashMap<Long, FADCPipeline>();
        Set<Long> cells = ((HPSEcal3) ecal).getNeighborMap().keySet();
        for (Long cellID : cells) {
            EcalChannelConstants channelData = findChannel(cellID);
            analogPipelines.put(cellID, new RingBuffer(bufferLength));
            digitalPipelines.put(cellID, new FADCPipeline(pipelineLength, (int) Math.round(channelData.getCalibration().getPedestal())));
        }
        return true;
    }

    /**
     * Returns pulse amplitude at the given time (relative to hit time). Gain is
     * applied.
     *
     * @param time Units of ns. Relative to hit time (negative=before the start
     * of the pulse).
     * @param cellID Crystal ID as returned by hit.getCellID().
     * @return Amplitude, units of volts/GeV.
     */
    private double pulseAmplitude(double time, long cellID) {
        // Get the channel data.
        EcalChannelConstants channelData = findChannel(cellID);

        if (use2014Gain) {
            //if fixedGain is set, multiply the default gain by this factor
            double corrGain;
            if (fixedGain > 0) {
                corrGain = fixedGain;
            } else {
                corrGain = 1.0 / channelData.getGain().getGain();
            }

            return corrGain * readoutGain * pulseAmplitude(time, pulseShape, tp);
        } else {
            //normalization constant from cal gain (MeV/integral bit) to amplitude gain (amplitude bit/GeV)
            double gain;
            if (fixedGain > 0) {
                gain = readoutPeriod / (fixedGain * EcalUtils.MeV * ((Math.pow(2, nBit) - 1) / maxVolt));
            } else {
                gain = readoutPeriod / (channelData.getGain().getGain() * EcalUtils.MeV * ((Math.pow(2, nBit) - 1) / maxVolt));
            }

            return gain * pulseAmplitude(time, pulseShape, tp);
        }
    }

    /**
     * Returns pulse amplitude at the given time (relative to hit time).
     *
     * @param time Units of ns. Relative to hit time (negative=before the start
     * of the pulse).
     * @return Amplitude, units of inverse ns. Normalized so the pulse integral
     * is 1.
     */
    public static double pulseAmplitude(double time, PulseShape shape, double shapingTime) {
        if (time <= 0.0) {
            return 0.0;
        }
        switch (shape) {
            case CRRC:
                //peak at tp
                //peak value 1/(tp*e)
                return ((time / (shapingTime * shapingTime)) * Math.exp(-time / shapingTime));
            case DoubleGaussian:
                //According to measurements the output signal can be fitted by two gaussians, one for the rise of the signal, one for the fall
                //peak at 3*riseTime
                //peak value 1/norm

                double norm = ((riseTime + fallTime) / 2) * Math.sqrt(2 * Math.PI); //to ensure the total integral is equal to 1: = 33.8
                return funcGaus(time - 3 * riseTime, (time < 3 * riseTime) ? riseTime : fallTime) / norm;
            case ThreePole:
                //peak at 2*tp
                //peak value 2/(tp*e^2)
                return ((time * time / (2 * shapingTime * shapingTime * shapingTime)) * Math.exp(-time / shapingTime));
            default:
                return 0.0;
        }
    }

    // Gaussian function needed for the calculation of the pulse shape amplitude  
    public static double funcGaus(double t, double sig) {
        return Math.exp(-t * t / (2 * sig * sig));
    }

    public class FADCPipeline {

        private final int[] array;
        private final int size;
        private int ptr;

        public FADCPipeline(int size) {
            this.size = size;
            array = new int[size]; //initialized to 0
            ptr = 0;
        }

        //construct pipeline with a nonzero initial value
        public FADCPipeline(int size, int init) {
            this.size = size;
            array = new int[size];
            for (int i = 0; i < size; i++) {
                array[i] = init;
            }
            ptr = 0;
        }

        /**
         * Write value to current cell
         */
        public void writeValue(int val) {
            array[ptr] = val;
        }

        /**
         * Write value to current cell
         */
        public void step() {
            ptr++;
            if (ptr == size) {
                ptr = 0;
            }
        }

        //return content of specified cell (pos=0 for current cell)
        public int getValue(int pos) {
            if (pos >= size || pos < 0) {
                throw new ArrayIndexOutOfBoundsException();
            }
            return array[((ptr - pos) % size + size) % size];
        }
    }

    /**
     * Convert physical ID to gain value.
     *
     * @param cellID (long)
     * @return channel constants (EcalChannelConstants)
     */
    private EcalChannelConstants findChannel(long cellID) {
        return ecalConditions.getChannelConstants(ecalConditions.getChannelCollection().findGeometric(cellID));
    }

    public static class TimeComparator implements Comparator<RawCalorimeterHit> {

        @Override
        public int compare(RawCalorimeterHit o1, RawCalorimeterHit o2) {
            return o1.getTimeStamp() - o2.getTimeStamp();
        }
    }
}
