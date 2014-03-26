package org.hps.recon.tracking;

//--- java ---//
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import org.hps.conditions.deprecated.HPSSVTCalibrationConstants;
import org.hps.conditions.deprecated.HPSSVTConstants;
import org.hps.conditions.deprecated.SvtUtils;
import org.hps.readout.ecal.ClockSingleton;
import org.hps.readout.ecal.ReadoutTimestamp;
import org.hps.readout.ecal.TriggerableDriver;
import org.hps.util.RandomGaussian;
//--- lcsim ---//
import org.lcsim.detector.tracker.silicon.ChargeCarrier;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.LCRelation;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.event.base.BaseLCRelation;
import org.lcsim.event.base.BaseRawTrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.lcio.LCIOConstants;
import org.lcsim.recon.tracking.digitization.sisim.CDFSiSensorSim;
import org.lcsim.recon.tracking.digitization.sisim.SiElectrodeData;
import org.lcsim.recon.tracking.digitization.sisim.SiElectrodeDataCollection;
import org.lcsim.recon.tracking.digitization.sisim.SiSensorSim;
import org.lcsim.recon.tracking.digitization.sisim.config.SimTrackerHitReadoutDriver;

/**
 *
 * @author Sho Uemura <meeg@slac.stanford.edu>
 * @version $Id: SimpleSvtReadout.java,v 1.19 2013/08/07 23:38:32 meeg Exp $
 */
public class SimpleSvtReadout extends TriggerableDriver {

    private SimTrackerHitReadoutDriver readoutDriver = new SimTrackerHitReadoutDriver();
    private SiSensorSim siSimulation = new CDFSiSensorSim();
    private String outputCollection = "SVTRawTrackerHits";
    private String relationCollection = "SVTTrueHitRelations";
    private Map<SiSensor, PriorityQueue<StripHit>[]> hitMap = new HashMap<SiSensor, PriorityQueue<StripHit>[]>();
    //readout period time offset in ns
    private double readoutOffset = 0.0;
    private double readoutLatency = 240.0;
    private double pileupCutoff = 300.0;
    private String readout = "TrackerHits";
    private double timeOffset = 30.0;
    private boolean noPileup = false;
    private boolean addNoise = true;
    //cut settings
    private boolean enableThresholdCut = true;
    private int samplesAboveThreshold = 3;
    private double noiseThreshold = 2.0;
    private boolean enablePileupCut = true;
    private boolean dropBadChannels = true;

    public SimpleSvtReadout() {
        add(readoutDriver);
        triggerDelay = 100.0;
    }

    public void setAddNoise(boolean addNoise) {
        this.addNoise = addNoise;
    }

    public void setEnablePileupCut(boolean enablePileupCut) {
        this.enablePileupCut = enablePileupCut;
    }

    public void setEnableThresholdCut(boolean enableThresholdCut) {
        this.enableThresholdCut = enableThresholdCut;
    }

    public void setNoiseThreshold(double noiseThreshold) {
        this.noiseThreshold = noiseThreshold;
    }

    public void setSamplesAboveThreshold(int samplesAboveThreshold) {
        this.samplesAboveThreshold = samplesAboveThreshold;
    }

    public void setNoPileup(boolean noPileup) {
        this.noPileup = noPileup;
    }

    public void setDropBadChannels(boolean dropBadChannels) {
        this.dropBadChannels = dropBadChannels;
    }

    /**
     *
     */
    @Override
    public void detectorChanged(Detector detector) {
        super.detectorChanged(detector);

        String[] readouts = {readout};
        readoutDriver.setCollections(readouts);

        if (!noPileup) {
            for (SiSensor sensor : SvtUtils.getInstance().getSensors()) {
                PriorityQueue<StripHit>[] hitQueues = new PriorityQueue[HPSSVTConstants.TOTAL_STRIPS_PER_SENSOR];
                hitMap.put(sensor, hitQueues);
            }
        }
    }

    /**
     *
     */
    @Override
    public void process(EventHeader event) {
        super.process(event);

        List<StripHit> stripHits = doSiSimulation();

        if (!noPileup) {
            for (StripHit stripHit : stripHits) {
                SiSensor sensor = stripHit.sensor;
                int channel = stripHit.channel;

                PriorityQueue<StripHit>[] hitQueues = hitMap.get(sensor);
                if (hitQueues[channel] == null) {
                    hitQueues[channel] = new PriorityQueue<StripHit>();
                }
                hitQueues[channel].add(stripHit);
            }

            // dump stale hits
            for (SiSensor sensor : SvtUtils.getInstance().getSensors()) {
                PriorityQueue<StripHit>[] hitQueues = hitMap.get(sensor);
                for (int i = 0; i < hitQueues.length; i++) {
                    if (hitQueues[i] != null) {
                        while (!hitQueues[i].isEmpty() && hitQueues[i].peek().time < ClockSingleton.getTime() - (readoutLatency + pileupCutoff)) {
//                                System.out.format("Time %f: Dump stale hit with time %f\n",ClockSingleton.getTime(),hitQueues[i].peek().time);
                            hitQueues[i].poll();
                        }
                        if (hitQueues[i].isEmpty()) {
                            hitQueues[i] = null;
                        }
                    }
                }
            }

            // If an ECal trigger is received, make hits from pipelines
            checkTrigger(event);
        } else {
            // Create a list to hold the analog data
            List<RawTrackerHit> hits = new ArrayList<RawTrackerHit>();

            for (StripHit stripHit : stripHits) {
                SiSensor sensor = stripHit.sensor;
                int channel = stripHit.channel;
                double amplitude = stripHit.amplitude;
                short[] samples = new short[6];

                double[] signal = new double[6];
                for (int i = 0; i < 6; i++) {
                    signal[i] = HPSSVTCalibrationConstants.getPedestal(sensor, channel);
                }
                if (addNoise) {
                    addNoise(sensor, channel, signal);
                }

                for (int i = 0; i < 6; i++) {
                    double time = i * HPSSVTConstants.SAMPLING_INTERVAL - timeOffset;
                    signal[i] += amplitude * pulseAmplitude(time, HPSSVTCalibrationConstants.getTShaping(sensor, channel));
//                    signal[i] += amplitude * pulseAmplitude(time, HPSSVTCalibrationConstants.getTShaping(sensor, channel)) + HPSSVTCalibrationConstants.getPedestal(sensor, channel);
                    samples[i] = (short) Math.round(signal[i]);
                }

                long cell_id = SvtUtils.makeCellID(sensor, channel);

                RawTrackerHit hit = new BaseRawTrackerHit(0, cell_id, samples, new ArrayList<SimTrackerHit>(stripHit.simHits), sensor);
//                        System.out.println("Making RTH");
                if (readoutCuts(hit)) {
//                    System.out.println("RTH passed cuts");
                    hits.add(hit);
                }
            }

            int flags = 1 << LCIOConstants.TRAWBIT_ID1;
//            flags += 1 << LCIOConstants.RTHBIT_HITS;
            event.put(outputCollection, hits, RawTrackerHit.class, flags, readout);
//            System.out.println("Made " + hits.size() + " RawTrackerHits");
        }
    }

    private List<StripHit> doSiSimulation() {
        List<StripHit> stripHits = new ArrayList<StripHit>();
        for (SiSensor sensor : SvtUtils.getInstance().getSensors()) {

            // Set the sensor to be used in the charge deposition simulation
            siSimulation.setSensor(sensor);

            // Perform the charge deposition simulation
            Map<ChargeCarrier, SiElectrodeDataCollection> electrodeDataMap = siSimulation.computeElectrodeData();

            for (ChargeCarrier carrier : ChargeCarrier.values()) {

                // If the sensor is capable of collecting the given charge carrier
                // then obtain the electrode data for the sensor
                if (sensor.hasElectrodesOnSide(carrier)) {

                    SiElectrodeDataCollection electrodeDataCol = electrodeDataMap.get(carrier);

                    // If there is no electrode data available create a new instance of electrode data
                    if (electrodeDataCol == null) {
                        electrodeDataCol = new SiElectrodeDataCollection();
                    }

                    // Loop over all sensor channels
                    for (Integer channel : electrodeDataCol.keySet()) {

                        // Get the electrode data for this channel
                        SiElectrodeData electrodeData = electrodeDataCol.get(channel);
                        Set<SimTrackerHit> simHits = electrodeData.getSimulatedHits();

                        // compute hit time as the unweighted average of SimTrackerHit times; this is dumb but okay since there's generally only one SimTrackerHit
                        double time = 0.0;
                        for (SimTrackerHit hit : simHits) {
                            time += hit.getTime();
                        }
                        time /= simHits.size();
                        time += ClockSingleton.getTime();

                        // Get the charge in units of electrons
                        double charge = electrodeData.getCharge();

                        double resistorValue = 100;  // Ohms
                        double inputStageGain = 1.5;
                        double amplitude = (charge / HPSSVTConstants.MIP) * resistorValue * inputStageGain * Math.pow(2, 14) / 2000;

                        stripHits.add(new StripHit(sensor, channel, amplitude, time, simHits));
                    }
                }
            }
            // Clear the sensors of all deposited charge
            siSimulation.clearReadout();
        }
        return stripHits;
    }

    private void addNoise(SiSensor sensor, int channel, double[] signal) {
        double noise = HPSSVTCalibrationConstants.getNoise(sensor, channel);
        for (int i = 0; i < 6; i++) {
            signal[i] += RandomGaussian.getGaussian(0, noise);
        }
    }

    private boolean readoutCuts(RawTrackerHit hit) {
        if (enableThresholdCut && !samplesAboveThreshold(hit)) {
//            System.out.println("Failed threshold cut");
            return false;
        }
        if (enablePileupCut && !pileupCut(hit)) {
//            System.out.println("Failed pileup cut");
            return false;
        }
        if (dropBadChannels && !badChannelCut(hit)) {
//            System.out.println("Failed bad channel cut");
            return false;
        }
        return true;
    }

    private boolean badChannelCut(RawTrackerHit hit) {
        SiSensor sensor = (SiSensor) hit.getDetectorElement();
        int channel = hit.getIdentifierFieldValue("strip");
        return !HPSSVTCalibrationConstants.isBadChannel(sensor, channel);
    }

    private boolean pileupCut(RawTrackerHit hit) {
        short[] samples = hit.getADCValues();
        return (samples[2] > samples[1] || samples[3] > samples[2]);
    }

    private boolean samplesAboveThreshold(RawTrackerHit hit) {
        SiSensor sensor = (SiSensor) hit.getDetectorElement();
        int channel = hit.getIdentifierFieldValue("strip");
        HPSSVTCalibrationConstants.getChannelConstants(sensor, channel);
        double pedestal = HPSSVTCalibrationConstants.getPedestal(sensor, channel);
        double noise = HPSSVTCalibrationConstants.getNoise(sensor, channel);
        int count = 0;
        short[] samples = hit.getADCValues();
        for (int i = 0; i < samples.length; i++) {
//            System.out.format("%d, %d\n", samples[i] - pedestal, noise * 3.0);
            if (samples[i] - pedestal > noise * noiseThreshold) {
                count++;
            }
        }
        return count >= samplesAboveThreshold;
    }

    @Override
    protected void processTrigger(EventHeader event) {
        if (noPileup) {
            return;
        }
//            System.out.println("Got trigger");

        // Create a list to hold the analog data
        List<RawTrackerHit> hits = new ArrayList<RawTrackerHit>();
        List<LCRelation> trueHitRelations = new ArrayList<LCRelation>();
        // Calculate time of first sample
        double firstSample = Math.floor((ClockSingleton.getTime() - readoutLatency - readoutOffset) / HPSSVTConstants.SAMPLING_INTERVAL) * HPSSVTConstants.SAMPLING_INTERVAL + readoutOffset;

        for (SiSensor sensor : SvtUtils.getInstance().getSensors()) {
            PriorityQueue<StripHit>[] hitQueues = hitMap.get(sensor);
            for (int channel = 0; channel < hitQueues.length; channel++) {
                if (!addNoise && (hitQueues[channel] == null || hitQueues[channel].isEmpty())) {
                    continue;
                }
                double[] signal = new double[6];
                for (int i = 0; i < 6; i++) {
                    signal[i] = HPSSVTCalibrationConstants.getPedestal(sensor, channel);
                }
                if (addNoise) {
                    addNoise(sensor, channel, signal);
                }

                List<SimTrackerHit> simHits = new ArrayList<SimTrackerHit>();

                if (hitQueues[channel] != null) {
                    for (StripHit hit : hitQueues[channel]) {
                        double totalContrib = 0;
                        for (int i = 0; i < 6; i++) {
                            double sampleTime = firstSample + i * HPSSVTConstants.SAMPLING_INTERVAL;
                            double signalAtTime = hit.amplitude * pulseAmplitude(sampleTime - hit.time, HPSSVTCalibrationConstants.getTShaping(sensor, channel));
                            totalContrib += signalAtTime;
                            signal[i] += signalAtTime;
//                                    System.out.format("new value of signal[%d] = %f\n", i, signal[i]);
                        }
                        if (totalContrib > 4.0 * HPSSVTCalibrationConstants.getNoise(sensor, channel)) {
//                            System.out.format("adding %d simHits\n", hit.simHits.size());
                            simHits.addAll(hit.simHits);
                        }
                    }
                }

                short[] samples = new short[6];
                for (int i = 0; i < 6; i++) {
                    samples[i] = (short) Math.round(signal[i]);
                }
//                        if (hitQueues[channel] != null && !hitQueues[channel].isEmpty()) {
//                            for (int i = 0; i < 6; i++) {
//                                System.out.format("samples[%d] = %d\n", i, samples[i]);
//                            }
//                        }
                long cell_id = SvtUtils.makeCellID(sensor, channel);
                RawTrackerHit hit = new BaseRawTrackerHit(0, cell_id, samples, simHits, sensor);
                if (readoutCuts(hit)) {
                    hits.add(hit);
//                    System.out.format("simHits: %d\n", simHits.size());
                    for (SimTrackerHit simHit : hit.getSimTrackerHits()) {
                        LCRelation hitRelation = new BaseLCRelation(hit, simHit);
                        trueHitRelations.add(hitRelation);
                    }
                }
            }
        }

        int flags = 1 << LCIOConstants.TRAWBIT_ID1;
//            flags += 1 << LCIOConstants.RTHBIT_HITS;
        event.put(outputCollection, hits, RawTrackerHit.class, flags, readout);
        event.put(relationCollection, trueHitRelations, LCRelation.class, 0);
        System.out.println("Made " + hits.size() + " RawTrackerHits");
        System.out.println("Made " + trueHitRelations.size() + " LCRelations");
    }

    @Override
    public double readoutDeltaT() {
        double triggerTime = ClockSingleton.getTime() + triggerDelay;
//        int cycle = (int) Math.floor((triggerTime - readoutOffset + ClockSingleton.getDt()) / Apv25Constants.SAMPLING_INTERVAL);
        // Calculate time of first sample
        double firstSample = Math.floor((triggerTime - readoutLatency - readoutOffset) / HPSSVTConstants.SAMPLING_INTERVAL) * HPSSVTConstants.SAMPLING_INTERVAL + readoutOffset;

        return firstSample;
    }

    private class StripHit implements Comparable {

        SiSensor sensor;
        int channel;
        double amplitude;
        double time;
        Set<SimTrackerHit> simHits;

        public StripHit(SiSensor sensor, int channel, double amplitude, double time, Set<SimTrackerHit> simHits) {
            this.sensor = sensor;
            this.channel = channel;
            this.amplitude = amplitude;
            this.time = time;
            this.simHits = simHits;
        }

        @Override
        public int compareTo(Object o) {
            double deltaT = time - ((StripHit) o).time;
            if (deltaT > 0) {
                return 1;
            } else if (deltaT < 0) {
                return -1;
            } else {
                return 0;
            }
        }
    }

    private double pulseAmplitude(double time, double tp) {
        if (time <= 0.0) {
            return 0.0;
        }
        return (time / tp) * Math.exp(1.0 - time / tp);
    }
    
    public int getTimestampType() {
        return ReadoutTimestamp.SYSTEM_TRACKER;
    }
}
