package org.lcsim.hps.recon.tracking.apv25;

//--- Java ---//
import hep.aida.ref.histogram.Profile1D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

//--- apache ---//
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.BinomialDistribution;
import org.apache.commons.math.distribution.BinomialDistributionImpl;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;


//--- org.lcsim ---//
import org.lcsim.detector.tracker.silicon.ChargeCarrier;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.detector.tracker.silicon.SiSensorElectrodes;
import org.lcsim.event.EventHeader;
import org.lcsim.geometry.Detector;
import org.lcsim.math.probability.Erf;
import org.lcsim.recon.tracking.digitization.sisim.SiElectrodeData;
import org.lcsim.recon.tracking.digitization.sisim.SiElectrodeDataCollection;
import org.lcsim.recon.tracking.digitization.sisim.SiSensorSim;
import org.lcsim.recon.tracking.digitization.sisim.CDFSiSensorSim;
import org.lcsim.recon.tracking.digitization.sisim.config.SimTrackerHitReadoutDriver;
import org.lcsim.util.aida.AIDA;
import org.lcsim.util.Driver;


//--- hps-java ---//
import org.lcsim.hps.recon.tracking.apv25.HPSAPV25.APV25Channel.APV25AnalogPipeline;
import org.lcsim.hps.recon.tracking.SvtUtils;
import org.lcsim.hps.util.ClockSingleton;

/**
 * Class used to Readout HPS APV25's
 *
 * @author Omar Moreno <omoreno1@ucsc.edu>
 * @version $Id: HPSSiSensorReadout.java,v 1.12 2013/03/15 21:05:28 meeg Exp $
 */
public class HPSSiSensorReadout extends Driver {

    //
    boolean debug = true;
    String subdetectorName = "tracker";
    // Array to store the trigger time
    public static final List<Double> triggerTimeStamp = new ArrayList<Double>();
    //
    List<String> readouts = new ArrayList<String>();
    // FIFO queue to store trigger times
    public Queue<Integer> triggerQueue;
    private HPSAPV25 apv25;
    private SiSensorSim siSimulation;
    private HPSRTM rtm;
    private HPSDataProcessingModule dpm;
    private static Random random = new Random();
    private static BinomialDistribution binomial = new BinomialDistributionImpl(1, 1);
    private static NormalDistribution gaussian = new NormalDistributionImpl(0.0, 1.0);
    private double noiseThreshold = 4;  // e- RMS
    private boolean addNoise = false;
    // A map used to associate a sensor to the channels and analog pipelines 
    // of the APV25s being used to readout the sensor
    public Map<SiSensor /* sensor */, Map<Integer /* channel */, APV25AnalogPipeline>> sensorToPipelineMap;
    // A map used to associate an APV25 channel to its analog pipeline
    public Map<Integer /* channel */, APV25AnalogPipeline> analogPipelineMap;
    // A map used to associate a sensor to the output of the APV25s being 
    // used to readout the sensor
    public Map<SiSensor, Map<Integer /* chip # */, double[]>> sensorToAnalogDataMap;
    // A map used to associate a sensor to the digitized output of the APV25s
    // being used to readout the sensor
    public Map<SiSensor, Map<Integer /*chip # */, double[]>> sensorToDigitalDataMap;
    //
    public Map<Integer, double[]> analogData;
    public Map<Integer, double[]> digitalData;
    //
    protected AIDA aida = AIDA.defaultInstance();
    public Profile1D pipe;
    int n_events = 0;

    /**
     * Constructor
     */
    public HPSSiSensorReadout() {

        sensorToPipelineMap = new HashMap<SiSensor, Map<Integer, APV25AnalogPipeline>>();
        sensorToAnalogDataMap = new HashMap<SiSensor, Map<Integer, double[]>>();
        sensorToDigitalDataMap = new HashMap<SiSensor, Map<Integer, double[]>>();

        //--- Sensor Simulation ---//
        //-------------------------//
        siSimulation = new CDFSiSensorSim();

        //--- APV25 Simulation ---//
        //------------------------//
        apv25 = new HPSAPV25();
        // Set the APV25 Shaping time [ns]
        apv25.setShapingTime(35);
        // Set the APV25 operating mode
        apv25.setAPV25Mode("multi-peak");
        // Set the APV25 analog pipeline sampling time
        apv25.setSamplingTime(24);

        // 
        rtm = new HPSRTM(14);

        // Instantiate the DPM
        dpm = new HPSDataProcessingModule();
        dpm.setNoise(18);
        dpm.setNoiseThreshold(2);
        dpm.setSamplesAboveThresh(3);
        dpm.setPedestal(1638);
        dpm.enableThresholdCut();
        dpm.enableTailCut();

        add(dpm);

        // Instantiate trigger time queue
        triggerQueue = new LinkedList<Integer>();

        // Specify the readouts to process
        readouts.add("TrackerHits");

    }

    public void setAddNoise(boolean addNoise) {
        this.addNoise = addNoise;
    }

    /**
     * 
     */
    @Override
    public void detectorChanged(Detector detector) {
        // Call the sub-Drivfer's detectorChanged methods
        super.detectorChanged(detector);

        // Instantiate all maps
        for (SiSensor sensor : SvtUtils.getInstance().getSensors()) {

            sensorToPipelineMap.put(sensor, new HashMap<Integer, APV25AnalogPipeline>());

            // Instantiate all analog pipelines
            for (int channel = 0; channel < sensor.getReadoutElectrodes(ChargeCarrier.HOLE).getNCells(); channel++) {
                sensorToPipelineMap.get(sensor).put(channel, apv25.getChannel().new APV25AnalogPipeline());
            }

            if (debug) {
                System.out.println(this.getClass().getSimpleName() + ": Sensor: " + sensor.getName()
                        + ": Number of Analog Pipelines: " + sensorToPipelineMap.get(sensor).size());
            }

            sensorToAnalogDataMap.put(sensor, new HashMap<Integer, double[]>());
            sensorToDigitalDataMap.put(sensor, new HashMap<Integer, double[]>());
        }
    }

    /**
     * 
     */
    @Override
    public void startOfData() {
        // Set up readouts if they haven't been set
        if (!readouts.isEmpty()) {
            super.add(new SimTrackerHitReadoutDriver(readouts));
        }

        super.startOfData();
        readouts.clear();

    }

    /**
     * 
     */
    @Override
    public void process(EventHeader event) {


        super.process(event);

        if ((ClockSingleton.getTime() + ClockSingleton.getDt()) % 24 == 0) {
            for (Map.Entry<SiSensor, Map<Integer, APV25AnalogPipeline>> sensor : sensorToPipelineMap.entrySet()) {
                apv25.incrementAllPointerPositions(sensor.getValue());
            }
            apv25.stepAPV25Clock();
        }

        // Loop over all sensors
        for (SiSensor sensor : SvtUtils.getInstance().getSensors()) {
            // Readout the sensors
            readoutSensor(sensor);
        }

        // If a trigger is received readout the APV25 and digitize all hits
        if (HPSAPV25.readoutBit) {

            triggerTimeStamp.add(ClockSingleton.getTime());

            // Only add the trigger if there isn't another trigger being 
            // processed
            if (!triggerQueue.contains(apv25.apv25ClockCycle)) {
                // Add the time at which each of the six samples should be 
                // collected to the trigger queue
                for (int sample = 0; sample < 6; sample++) {
                    triggerQueue.offer(apv25.apv25ClockCycle + sample);
                }
            }
            // Reset the APV25 trigger bit
            HPSAPV25.readoutBit = false;
        }

        // Process any triggers in the queue
        if (triggerQueue.peek() != null) {

            // Remove any samples that might have already been processed
            if (triggerQueue.peek() < apv25.apv25ClockCycle) {
                for (int sample = 0; sample < 6; sample++) {
                    triggerQueue.remove();
                }
            } else if (triggerQueue.peek() == apv25.apv25ClockCycle) {
                readoutAPV25();
                triggerQueue.remove();
            }
        }
    }

    /**
     * Readout the electrodes of an HPS Si sensor and inject the charge into 
     * the APV25 readout chip.
     * 
     * @param sensor : HPS Si sensor
     */
    public void readoutSensor(SiSensor sensor) {
        // Set the sensor to be used in the charge deposition simulation
        siSimulation.setSensor(sensor);

        // Perform the charge deposition simulation
        Map<ChargeCarrier, SiElectrodeDataCollection> electrodeDataMap = siSimulation.computeElectrodeData();

        // Loop over each charge carrier (electron or hole)
        for (ChargeCarrier carrier : ChargeCarrier.values()) {

            // If the sensor is capable of collecting the given charge carrier
            // then obtain the electrode data for the sensor
            if (sensor.hasElectrodesOnSide(carrier)) {

                SiElectrodeDataCollection electrodeDataCol = electrodeDataMap.get(carrier);

                // If there is no electrode data available create a new instance of electrode data
                if (electrodeDataCol == null) {
                    electrodeDataCol = new SiElectrodeDataCollection();
                }

                // Get the readout electrodes
                SiSensorElectrodes readoutElectrodes = sensor.getReadoutElectrodes(carrier);

                // Add noise to the electrodes
                if (addNoise) {
                    addNoise(electrodeDataCol, readoutElectrodes);
                }

                // Get the analog pipeline map associated with this sensor
                analogPipelineMap = sensorToPipelineMap.get(sensor);

                // Loop over all channels
                for (Integer channel : electrodeDataCol.keySet()) {

                    // Get the electrode data for this channel
                    SiElectrodeData electrodeData = electrodeDataCol.get(channel);

                    // Get the charge in units of electrons
                    double charge = electrodeData.getCharge();

                    //====> Charge deposition on electrode 
                    aida.histogram1D("Charge", 100, 0, 200000).fill(charge);
                    //====>

                    // Get the RMS noise for this channel
                    double noise = apv25.getChannel().computeNoise(
                            readoutElectrodes.getCapacitance(channel));

                    //===>
                    aida.histogram1D(this.getClass().getName() + " - RMS Noise - All Channels", 1000, 3500, 4500).fill(noise);
                    //===>

                    // Check to see if an analog pipeline for this channel
                    // exist.  If it doesn't, create one.
                    if (!analogPipelineMap.containsKey(channel)) {
                        analogPipelineMap.put(channel,
                                apv25.getChannel().new APV25AnalogPipeline());
                    }

                    // Get the analog pipeline associated with this channel
                    APV25AnalogPipeline pipeline = analogPipelineMap.get(channel);

                    // Inject the charge into the APV25 amplifier chain
                    apv25.injectCharge(charge, noise, pipeline);
                }
            }
        }

        // Place the analog pipeline back into the sensor map
        sensorToPipelineMap.put(sensor, analogPipelineMap);

        // Clear the sensors of all deposited charge
        siSimulation.clearReadout();
    }

    /**
     * 
     */
    public void readoutAPV25() {
        // Readout all apv25s
        for (Map.Entry<SiSensor, Map<Integer, APV25AnalogPipeline>> sensor : sensorToPipelineMap.entrySet()) {
            sensorToAnalogDataMap.put(sensor.getKey(), apv25.APV25Multiplexer(sensor.getValue()));
        }

        // Digitize all signals
        for (Map.Entry<SiSensor, Map<Integer, double[]>> sensor : sensorToAnalogDataMap.entrySet()) {
            sensorToDigitalDataMap.put(sensor.getKey(), rtm.digitize(sensor.getValue()));
        }

        // Buffer the samples for further processing
        //---> Needs to change!
        dpm.addSample(sensorToDigitalDataMap);
        //--->
    }

    /**
     * 
     * @param electrodeDataCol
     * @param electrodes 
     */
    public void addNoise(SiElectrodeDataCollection electrodeDataCol,
            SiSensorElectrodes electrodes) {
        // First add readout noise to the strips in the 
        // SiElectrodeDataCollection.

        // Loop over the entries in the SiElectrodeDataCollection
        for (Entry electrodeDatum : electrodeDataCol.entrySet()) {

            // Get the channel number and electrode data for this entry
            int channel = (Integer) electrodeDatum.getKey();
            SiElectrodeData electrodeData = (SiElectrodeData) electrodeDatum.getValue();

            // Get the RMS noise for this channel in units of electrons
            double noise = apv25.getChannel().computeNoise(
                    electrodes.getCapacitance(channel));

            // Add readout noise to the deposited charge
            int noiseCharge = (int) Math.round(random.nextGaussian() * noise);
            electrodeData.addCharge(noiseCharge);
        }

        // Find the number of strips that are not currently hit
        int nElectrodes = electrodes.getNCells();
        int nElectrodesEmpty = nElectrodes - electrodeDataCol.size();

        // Get the noise threshold in units of the noise charge

        // Calculate how many channels should get noise hits
        double integral = Erf.phic(noiseThreshold);
        int nChannelsThrow = drawBinomial(nElectrodesEmpty, integral);

        // Now throw Gaussian randoms above the seed threshold and put signals
        // on unoccupied channels
        for (int ithrow = 0; ithrow < nChannelsThrow; ithrow++) {
            // Throw to get a channel number
            int channel = random.nextInt(nElectrodes);
            while (electrodeDataCol.keySet().contains(channel)) {
                channel = random.nextInt(nElectrodes);
            }

            // Calculate the noise for this channel in units of electrons
            double noise = apv25.getChannel().computeNoise(
                    electrodes.getCapacitance(channel));

            // Throw Gaussian above threshold
            int charge = (int) Math.round(drawGaussianAboveThreshold(integral) * noise);

            // Add the noise hit to the electrode data collection
            electrodeDataCol.add(channel, new SiElectrodeData(charge));
        }

        // Now throw to lower threshold on channels that neighbor hits until
        // we are exhausted

        nChannelsThrow = 1;
        while (nChannelsThrow > 0) {

            // Get neighbor channels
            Set<Integer> neighbors = new HashSet<Integer>();
            for (int channel : electrodeDataCol.keySet()) {
                neighbors.addAll(electrodes.getNearestNeighborCells(channel));
            }
            neighbors.removeAll(electrodeDataCol.keySet());

            nElectrodesEmpty = neighbors.size();

            integral = Erf.phic(noiseThreshold);
            nChannelsThrow = drawBinomial(nElectrodesEmpty, integral);

            // Now throw Gaussian randoms above a threshold and put signals on
            // unoccopied channels
            for (int ithrow = 0; ithrow < nChannelsThrow; ithrow++) {

                // Throw to get a channel number
                List<Integer> neighborList = new ArrayList<Integer>(neighbors);

                int channel = neighborList.get(random.nextInt(nElectrodesEmpty));

                while (electrodeDataCol.keySet().contains(channel)) {
                    channel = neighborList.get(random.nextInt(nElectrodesEmpty));

                }

                // Calculate the noise for this channel in units of electrons
                double noise = apv25.getChannel().computeNoise(
                        electrodes.getCapacitance(channel));

                // Throw Gaussian above threshold
                int charge = (int) Math.round(drawGaussianAboveThreshold(integral) * noise);

                // Add the noise hit to the electrode data collection
                electrodeDataCol.add(channel, new SiElectrodeData(charge));
            }
        }
    }

    /**
     * 
     */
    public static int drawBinomial(int ntrials, double probability) {
        binomial.setNumberOfTrials(ntrials);
        binomial.setProbabilityOfSuccess(probability);

        int nsuccess = 0;
        try {
            nsuccess = binomial.inverseCumulativeProbability(random.nextDouble());
        } catch (MathException exception) {
            throw new RuntimeException("APV25 failed to calculate inverse cumulative probability of binomial!");
        }
        return nsuccess;
    }

    /**
     * Return a random variable following normal distribution, but beyond
     * threshold provided during initialization.
     */
    public static double drawGaussianAboveThreshold(double prob_above_threshold) {
        double cumulative_probability;

        cumulative_probability = 1.0 + prob_above_threshold * (random.nextDouble() - 1.0);

        assert cumulative_probability < 1.0 : "cumulProb=" + cumulative_probability + ", probAboveThreshold=" + prob_above_threshold;
        assert cumulative_probability >= 0.0 : "cumulProb=" + cumulative_probability + ", probAboveThreshold=" + prob_above_threshold;

        double gaussian_random = 0;
        try {
            gaussian_random = gaussian.inverseCumulativeProbability(cumulative_probability);
        } catch (MathException e) {
            System.out.println("MathException caught: " + e);
        }

        return gaussian_random;
    }
}
