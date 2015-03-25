package org.hps.readout.ecal;

import static org.hps.recon.ecal.ECalUtils.ecalReadoutPeriod;
import static org.hps.recon.ecal.ECalUtils.fallTime;
import static org.hps.recon.ecal.ECalUtils.maxVolt;
import static org.hps.recon.ecal.ECalUtils.nBit;
import static org.hps.recon.ecal.ECalUtils.readoutGain;
import static org.hps.recon.ecal.ECalUtils.riseTime;

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
import org.hps.recon.ecal.ECalUtils;
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
    private static final int ECAL_WINDOW_MODE = 1;
    private static final int ECAL_PULSE_MODE = 2;
    private static final int ECAL_PULSE_INTEGRAL_MODE = 3;
    private String ecalName = "Ecal";
    private Subdetector ecal;
    private EcalConditions ecalConditions = null;
    //buffer for preamp signals (units of volts, no pedestal)
    private Map<Long, RingBuffer> signalMap = null;
    //ADC pipeline for readout (units of ADC counts)
    private Map<Long, FADCPipeline> pipelineMap = null;
    //buffer for window sums
    private Map<Long, Integer> sumMap = null;
    //buffer for timestamps
    private Map<Long, Integer> timeMap = null;
    //queue for hits to be output to clusterer
    private PriorityQueue<RawCalorimeterHit> outputQueue = null;
    //length of ring buffer (in readout cycles)
    private int bufferLength = 100;
    //length of readout pipeline (in readout cycles)
    private int pipelineLength = 2000;
    //shaper time constant in ns
    private double tp = 6.95;
    //delay (number of readout periods) between start of summing window and output of hit to clusterer
    private int delay0 = 32;
    //start of readout window relative to trigger time (in readout cycles)
    //in FADC documentation, "Programmable Latency" or PL
    private int readoutLatency = 100;
    //number of ADC samples to read out
    //in FADC documentation, "Programmable Trigger Window" or PTW
    private int readoutWindow = 100;
    //number of ADC samples to read out before each rising threshold crossing
    //in FADC documentation, "number of samples before" or NSB
    private int numSamplesBefore = 5;
    //number of ADC samples to read out after each rising threshold crossing
    //in FADC documentation, "number of samples before" or NSA
    private int numSamplesAfter = 30;
//    private HPSEcalConverter converter = null;
    //output buffer for hits
    private LinkedList<RawCalorimeterHit> buffer = new LinkedList<RawCalorimeterHit>();
    //number of readout periods for which a given hit stays in the buffer
    private int coincidenceWindow = 2;
    //output collection name for hits read out from trigger
    private String ecalReadoutCollectionName = "EcalReadoutHits";
    private int mode = ECAL_PULSE_INTEGRAL_MODE;
    private int readoutThreshold = 10;
    private int triggerThreshold = 10;
    private double scaleFactor = 1;
    private double fixedGain = -1;
    private boolean constantTriggerWindow = true;
    private boolean addNoise = false;
   
    // 32.8 p.e./MeV = New detector in 2014
    // 2 p.e./MeV = Test Run detector
    private double pePerMeV = 32.8; //photoelectrons per MeV, used to calculate noise
    
    //switch between test run and 2014 definitions of gain constants
    // true = ONLY simulation studies in 2014
    // false = Test Run data/simulations and 2014+ Detector's real data 
    private boolean use2014Gain = false;
    
    //switch between three pulse shape functions
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

    public void setAddNoise(boolean addNoise) {
        this.addNoise = addNoise;
    }

    public void setConstantTriggerWindow(boolean constantTriggerWindow) {
        this.constantTriggerWindow = constantTriggerWindow;
    }

    public void setFixedGain(double fixedGain) {
        this.fixedGain = fixedGain;
    }

    public void setEcalName(String ecalName) {
        this.ecalName = ecalName;
    }

    public void setReadoutThreshold(int readoutThreshold) {
        this.readoutThreshold = readoutThreshold;
    }

    public void setScaleFactor(double scaleFactor) {
        this.scaleFactor = scaleFactor;
    }

    public void setTriggerThreshold(int triggerThreshold) {
        this.triggerThreshold = triggerThreshold;
    }

    public void setEcalReadoutCollectionName(String ecalReadoutCollectionName) {
        this.ecalReadoutCollectionName = ecalReadoutCollectionName;
    }

    public void setNumSamplesAfter(int numSamplesAfter) {
        this.numSamplesAfter = numSamplesAfter;
    }

    public void setNumSamplesBefore(int numSamplesBefore) {
        this.numSamplesBefore = numSamplesBefore;
    }

    public void setReadoutLatency(int readoutLatency) {
        this.readoutLatency = readoutLatency;
    }

    public void setReadoutWindow(int readoutWindow) {
        this.readoutWindow = readoutWindow;
    }

    public void setCoincidenceWindow(int coincidenceWindow) {
        this.coincidenceWindow = coincidenceWindow;
    }

    public void setUse2014Gain(boolean use2014Gain) {
        this.use2014Gain = use2014Gain;
    }

    public void setPulseShape(String pulseShape) {
        this.pulseShape = PulseShape.valueOf(pulseShape);
    }

    public void setTp(double tp) {
        this.tp = tp;
    }

//    public void setFallTime(double fallTime) {
//        this.fallTime = fallTime;
//    }
    public void setPePerMeV(double pePerMeV) {
        this.pePerMeV = pePerMeV;
    }

//    public void setRiseTime(double riseTime) {
//        this.riseTime = riseTime;
//    }
    public void setDelay0(int delay0) {
        this.delay0 = delay0;
    }

    public void setBufferLength(int bufferLength) {
        this.bufferLength = bufferLength;
        resetFADCBuffers();
    }

    public void setPipelineLength(int pipelineLength) {
        this.pipelineLength = pipelineLength;
        resetFADCBuffers();
    }

    public void setMode(int mode) {
        this.mode = mode;
        if (mode != ECAL_WINDOW_MODE && mode != ECAL_PULSE_MODE && mode != ECAL_PULSE_INTEGRAL_MODE) {
            throw new IllegalArgumentException("invalid mode " + mode);
        }
    }

    /**
     * Return the map of preamp signal buffers. For debug only.
     *
     * @return
     */
    public Map<Long, RingBuffer> getSignalMap() {
        return signalMap;
    }

    /**
     * Return the map of FADC pipelines. For debug only.
     *
     * @return
     */
    public Map<Long, FADCPipeline> getPipelineMap() {
        return pipelineMap;
    }

    @Override
    protected void readHits(List<RawCalorimeterHit> hits) {

        for (Long cellID : signalMap.keySet()) {
            RingBuffer signalBuffer = signalMap.get(cellID);

            FADCPipeline pipeline = pipelineMap.get(cellID);
            pipeline.step();
            
            // Get the channel data.
            EcalChannelConstants channelData = findChannel(cellID);

            double currentValue = signalBuffer.currentValue() * ((Math.pow(2, nBit) - 1) / maxVolt); //12-bit ADC with maxVolt V range
            int pedestal = (int) Math.round(channelData.getCalibration().getPedestal());
            int digitizedValue = Math.min((int) Math.round(pedestal + currentValue), (int) Math.pow(2, nBit)); //ADC can't return a value larger than 4095; 4096 (overflow) is returned for any input >2V
            pipeline.writeValue(digitizedValue);
            int pedestalSubtractedValue = digitizedValue - pedestal;
            //System.out.println(signalBuffer.currentValue() + "   " + currentValue + "   " + pipeline.currentValue());

            Integer sum = sumMap.get(cellID);
            if (sum == null && pedestalSubtractedValue > triggerThreshold) {
                timeMap.put(cellID, readoutCounter);
                if (constantTriggerWindow) {
                    int sumBefore = 0;
                    for (int i = 0; i < numSamplesBefore; i++) {
                        if (debug) {
                            System.out.format("trigger %d, %d: %d\n", cellID, i, pipeline.getValue(numSamplesBefore - i - 1));
                        }
                        sumBefore += pipeline.getValue(numSamplesBefore - i - 1);
                    }
                    sumMap.put(cellID, sumBefore);
                } else {
                    sumMap.put(cellID, pedestalSubtractedValue);
                }
            }
            if (sum != null) {
                if (constantTriggerWindow) {
                    if (timeMap.get(cellID) + numSamplesAfter >= readoutCounter) {
                        if (debug) {
                            System.out.format("trigger %d, %d: %d\n", cellID, readoutCounter - timeMap.get(cellID) + numSamplesBefore - 1, pipeline.getValue(0));
                        }
                        sumMap.put(cellID, sum + pipeline.getValue(0));
                    } else if (timeMap.get(cellID) + delay0 <= readoutCounter) {
//                        System.out.printf("sum = %f\n", sum);
                        outputQueue.add(new BaseRawCalorimeterHit(cellID,
                                (int) Math.round(sum / scaleFactor),
                                64 * timeMap.get(cellID)));
                        sumMap.remove(cellID);
                    }
                } else {
                    if (pedestalSubtractedValue < triggerThreshold || timeMap.get(cellID) + delay0 == readoutCounter) {
//					System.out.printf("sum = %f\n",sum);
                        outputQueue.add(new BaseRawCalorimeterHit(cellID,
                                (int) Math.round((sum + pedestalSubtractedValue) / scaleFactor),
                                64 * timeMap.get(cellID)));
                        sumMap.remove(cellID);
                    } else {
                        sumMap.put(cellID, sum + pedestalSubtractedValue);
                    }
                }
            }
            signalBuffer.step();
        }
        while (outputQueue.peek() != null && outputQueue.peek().getTimeStamp() / 64 <= readoutCounter - delay0) {
            if (outputQueue.peek().getTimeStamp() / 64 < readoutCounter - delay0) {
                System.out.println(this.getName() + ": Stale hit in output queue");
                outputQueue.poll();
            } else {
                buffer.add(outputQueue.poll());
            }
        }
        while (!buffer.isEmpty() && buffer.peek().getTimeStamp() / 64 <= readoutCounter - delay0 - coincidenceWindow) {
            buffer.remove();
        }
        if (debug) {
            for (RawCalorimeterHit hit : buffer) {
                System.out.format("new hit: energy %d\n", hit.getAmplitude());
            }
        }

        hits.addAll(buffer);
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
            case ECAL_WINDOW_MODE:
                if (debug) {
                    System.out.println("Reading out ECal in window mode");
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
        FADCPipeline pipeline = pipelineMap.get(cellID);
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
        for (Long cellID : pipelineMap.keySet()) {
            short[] adcValues = getWindow(cellID);
            hits.add(new BaseRawTrackerHit(cellID, 0, adcValues));
        }
        return hits;
    }

    protected List<RawTrackerHit> readPulses() {
//		System.out.println("Reading FADC data");
        List<RawTrackerHit> hits = new ArrayList<RawTrackerHit>();
        for (Long cellID : pipelineMap.keySet()) {
            short[] window = getWindow(cellID);
            short[] adcValues = null;
            int pointerOffset = 0;
            int numSamplesToRead = 0;
            int thresholdCrossing = 0;
            
            // Get the channel data.
            EcalChannelConstants channelData = findChannel(cellID);
            
            for (int i = 0; i < readoutWindow; i++) {
                if (numSamplesToRead != 0) {
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
        for (Long cellID : pipelineMap.keySet()) {
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

    @Override
    protected void putHits(List<CalorimeterHit> hits) {
        //fill the readout buffers
        for (CalorimeterHit hit : hits) {
            RingBuffer eDepBuffer = signalMap.get(hit.getCellID());
            double energyAmplitude = hit.getRawEnergy();
            // Get the channel data.
            EcalChannelConstants channelData = findChannel(hit.getCellID());
            if (addNoise) {
                //add preamp noise and photoelectron Poisson noise in quadrature
                double noise;
                if (use2014Gain) {
                    noise = Math.sqrt(Math.pow(channelData.getCalibration().getNoise() * channelData.getGain().getGain() * ECalUtils.gainFactor * ECalUtils.ecalReadoutPeriod, 2) 
                    		+ hit.getRawEnergy() / (ECalUtils.lightYield * ECalUtils.quantumEff * ECalUtils.surfRatio));
                } else {
                    noise = Math.sqrt(Math.pow(channelData.getCalibration().getNoise() * channelData.getGain().getGain() * ECalUtils.MeV, 2) 
                    		+ hit.getRawEnergy() * ECalUtils.MeV / pePerMeV);
                }
                energyAmplitude += RandomGaussian.getGaussian(0, noise);
            }
            for (int i = 0; i < bufferLength; i++) {
                eDepBuffer.addToCell(i, energyAmplitude * pulseAmplitude((i + 1) * readoutPeriod + readoutTime() - (ClockSingleton.getTime() + hit.getTime()), hit.getCellID()));
            }
        }
    }

    @Override
    protected void initReadout() {
        //initialize buffers
        sumMap = new HashMap<Long, Integer>();
        timeMap = new HashMap<Long, Integer>();
        outputQueue = new PriorityQueue(20, new TimeComparator());
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
        signalMap = new HashMap<Long, RingBuffer>();
        pipelineMap = new HashMap<Long, FADCPipeline>();
        Set<Long> cells = ((HPSEcal3) ecal).getNeighborMap().keySet();
        for (Long cellID : cells) {
        	EcalChannelConstants channelData = findChannel(cellID);
            signalMap.put(cellID, new RingBuffer(bufferLength));
            pipelineMap.put(cellID, new FADCPipeline(pipelineLength, (int) Math.round(channelData.getCalibration().getPedestal())));
        }
        return true;
    }

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
                gain = readoutPeriod / (fixedGain * ECalUtils.MeV * ((Math.pow(2, nBit) - 1) / maxVolt));
            } else {
                gain = readoutPeriod / (channelData.getGain().getGain() * ECalUtils.MeV * ((Math.pow(2, nBit) - 1) / maxVolt));
            }

            return gain * pulseAmplitude(time, pulseShape, tp);
        }
    }

    /**
     * Returns pulse amplitude at the given time (relative to hit time).
     * Amplitude is normalized so the pulse integral is 1.
     *
     * @param time
     * @return
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
