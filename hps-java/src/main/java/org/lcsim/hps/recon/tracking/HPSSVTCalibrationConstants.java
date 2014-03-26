package org.lcsim.hps.recon.tracking;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.lcsim.conditions.ConditionsManager;
import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.detector.tracker.silicon.SiTrackerIdentifierHelper;
import org.lcsim.hps.conditions.CalibrationDriver;
import org.hps.util.Pair;

/**
 *
 * @author Mathew Graham <mgraham@slac.stanford.edu> $Id:
 * HPSSVTCalibrationConstants.java,v 1.23 2013/02/25 22:39:26 meeg Exp $
 */
public class HPSSVTCalibrationConstants {

    // TODO: Change all map keys to type SiSensor?
    // TODO: Update everything to make it compatible new version of SVT Utils
    private static Map<Pair<Integer /*
             * FPGA
             */, Integer /*
             * Hybrid
             */>, double[] /*
             * constants
             */> noiseMap = new HashMap<Pair<Integer, Integer>, double[]>();
    private static Map<Pair<Integer /*
             * FPGA
             */, Integer /*
             * Hybrid
             */>, double[] /*
             * constants
             */> pedestalMap = new HashMap<Pair<Integer, Integer>, double[]>();
    private static Map<Pair<Integer /*
             * FPGA
             */, Integer /*
             * Hybrid
             */>, double[] /*
             * constants
             */> tpMap = new HashMap<Pair<Integer, Integer>, double[]>();
    private static Map<Pair<Integer /*
             * FPGA
             */, Integer /*
             * Hybrid
             */>, Set<Integer> /*
             * Bad channels
             */> badChannelMap = new HashMap<Pair<Integer, Integer>, Set<Integer>>();
    private static Map<Pair<Integer /*
             * FPGA
             */, Integer /*
             * Hybrid
             */>, List<Double> /*
             * channels
             */> gainMap = new HashMap<Pair<Integer, Integer>, List<Double>>();
    private static Map<Pair<Integer /*
             * FPGA
             */, Integer /*
             * Hybrid
             */>, List<Double> /*
             * channels
             */> offsetMap = new HashMap<Pair<Integer, Integer>, List<Double>>();
    private static Map<Pair<Integer, Integer>, Double> t0ShiftMap = new HashMap<Pair<Integer, Integer>, Double>();
    private static boolean pedestalLoaded = false;
    private static boolean tpLoaded = false;
    private static int totalBadChannels = 0;

    /**
     * Default Ctor
     */
    private HPSSVTCalibrationConstants() {
    }

    public static void loadCalibration(int runNumber) {
        loadCalibrationConstants(runNumber);
        loadBadChannels(runNumber);
        loadGain();
        loadT0Shifts();
    }

    public static void loadCalibrationConstants(int run) {
        //write something here to read in constants from calibration file
        ConditionsManager conditions = ConditionsManager.defaultInstance();
        String filePath = null;

        filePath = CalibrationDriver.getCalibForRun("calibSVT/base", run);

        if (filePath == null) {
            filePath = "calibSVT/default.base";
        }

        try {
            Reader baselineReader = conditions.getRawConditions(filePath).getReader();
            loadBaseline(baselineReader);
        } catch (IOException e) {
            throw new RuntimeException("couldn't get baseline file", e);
        }


        filePath = CalibrationDriver.getCalibForRun("calibSVT/tp", run);

        if (filePath == null) {
            filePath = "calibSVT/default.tp";
        }

        try {
            Reader tpReader = conditions.getRawConditions(filePath).getReader();
            loadTp(tpReader);
        } catch (IOException e) {
            throw new RuntimeException("couldn't get Tp file", e);
        }

    }

    private static void loadBaseline(Reader baselineReader) {
        BufferedReader bufferedBaseline = new BufferedReader(baselineReader);
        String line;
        while (true) {
            try {
                line = bufferedBaseline.readLine();
            } catch (IOException e) {
                throw new RuntimeException("couldn't parse baseline file", e);
            }
            if (line == null) {
                break;
            }

            if (line.indexOf("#") != -1) {
                line = line.substring(0, line.indexOf("#"));
            }

            StringTokenizer lineTok = new StringTokenizer(line);

            if (lineTok.countTokens() != 0) {
                if (lineTok.countTokens() != 5) {
                    throw new RuntimeException("Invalid line in baseline file: " + line);
                } else {
                    int fpga = Integer.valueOf(lineTok.nextToken());
                    int hybrid = Integer.valueOf(lineTok.nextToken());
                    int channel = Integer.valueOf(lineTok.nextToken());
                    double pedestal = Double.valueOf(lineTok.nextToken());
                    double noise = Double.valueOf(lineTok.nextToken());

                    Pair<Integer, Integer> daqPair = new Pair<Integer, Integer>(fpga, hybrid);

                    double[] pedestals = pedestalMap.get(daqPair);
                    if (pedestals == null) {
                        pedestals = new double[HPSSVTConstants.TOTAL_APV25_CHANNELS * HPSSVTConstants.TOTAL_APV25_PER_HYBRID];
                        pedestalMap.put(daqPair, pedestals);
                    }
                    pedestals[channel] = pedestal;

                    double[] noises = noiseMap.get(daqPair);
                    if (noises == null) {
                        noises = new double[HPSSVTConstants.TOTAL_APV25_CHANNELS * HPSSVTConstants.TOTAL_APV25_PER_HYBRID];
                        noiseMap.put(daqPair, noises);
                    }
                    noises[channel] = noise;
                }
            }
        }
        pedestalLoaded = true;
    }

    public static boolean pedestalLoaded() {
        return pedestalLoaded;
    }

    private static void loadTp(Reader baselineReader) {
        BufferedReader bufferedTp = new BufferedReader(baselineReader);
        String line;
        while (true) {
            try {
                line = bufferedTp.readLine();
            } catch (IOException e) {
                throw new RuntimeException("couldn't parse baseline file", e);
            }
            if (line == null) {
                break;
            }

            if (line.indexOf("#") != -1) {
                line = line.substring(0, line.indexOf("#"));
            }

            StringTokenizer lineTok = new StringTokenizer(line);

            if (lineTok.countTokens() != 0) {
                if (lineTok.countTokens() != 7) {
                    throw new RuntimeException("Invalid line in baseline file: " + line);
                } else {
                    int fpga = Integer.valueOf(lineTok.nextToken());
                    int hybrid = Integer.valueOf(lineTok.nextToken());
                    int channel = Integer.valueOf(lineTok.nextToken());
                    double amplitude = Double.valueOf(lineTok.nextToken());
                    double t0 = Double.valueOf(lineTok.nextToken());
                    double tp = Double.valueOf(lineTok.nextToken()) * 24.0 / 25.0; //FIXME: this is a hack to compensate for the calibrations we have that assumed 25 ns APV clock
                    double chisq = Double.valueOf(lineTok.nextToken());

                    Pair<Integer, Integer> daqPair = new Pair<Integer, Integer>(fpga, hybrid);

                    double[] tps = tpMap.get(daqPair);
                    if (tps == null) {
                        tps = new double[HPSSVTConstants.TOTAL_APV25_CHANNELS * HPSSVTConstants.TOTAL_APV25_PER_HYBRID];
                        tpMap.put(daqPair, tps);
                    }
                    tps[channel] = tp;
                }
            }
        }
        tpLoaded = true;
    }

    public static boolean tpLoaded() {
        return tpLoaded;
    }

    /**
     * Load the bad channels for the specified run. If there are no bad channels
     * specified, just load those found from QA. Channels found to be bad from
     * QA will always be loaded.
     *
     * @param run number
     */
    public static void loadBadChannels(int runNumber) {

        // Clear the previously loaded bad channels
        badChannelMap.clear();
        System.out.println("Loading the SVT bad channels for run " + runNumber);

        // If the calibrations are ideal (run < 0) don't load any bad channels
        if (runNumber < 0) {
            System.out.println("Running using ideal SVT calibrations. Bad channels will not be loaded.");
            return;
        }

        ConditionsManager conditions = ConditionsManager.defaultInstance();
        BufferedReader reader;

        // First load the bad channels found during QA (not run dependent)
        String filePath = "daqmap/svt_qa.badchannels";
        try {
            reader = new BufferedReader(conditions.getRawConditions(filePath).getReader());
            loadBadChannels(reader);
        } catch (IOException exception) {
            throw new RuntimeException("Unable to load bad channels for list " + filePath, exception);
        }

        // Load the bad channels for the specified run number
        filePath = "daqmap/svt" + runNumber + ".badchannels";
        try {
            reader = new BufferedReader(conditions.getRawConditions(filePath).getReader());
            loadBadChannels(reader);
        } catch (IOException exception) {
            // If the file isn't found, continue on with just the QA bad channels
            System.out.println("File " + filePath + " was not found! Continuing with only QA bad channels");
        }
    }

    /**
     * Load the bad channels from a file using the specified character stream.
     *
     * @param reader : character stream of type Reader
     */
    private static void loadBadChannels(Reader reader) {
        BufferedReader badChannelReader = new BufferedReader(reader);

        String line = null;
        try {
            while ((line = badChannelReader.readLine()) != null) {
                // If the line is a comment, skip it
                if (line.indexOf("#") != -1) {
                    continue;
                }
                totalBadChannels++;
                StringTokenizer stringTok = new StringTokenizer(line);
                while (stringTok.hasMoreTokens()) {
                    int badChannel = Integer.valueOf(stringTok.nextToken());
                    int fpga = Integer.valueOf(stringTok.nextToken());
                    int hybrid = Integer.valueOf(stringTok.nextToken());
                    Pair<Integer, Integer> daqPair = new Pair<Integer, Integer>(fpga, hybrid);
                    if (!badChannelMap.containsKey(daqPair)) {
                        badChannelMap.put(daqPair, new HashSet<Integer>());
                    }
                    badChannelMap.get(daqPair).add(badChannel);
                }
            }
        } catch (IOException exception) {
            throw new RuntimeException("Unable to parse SVT bad channel list", exception);
        }
    }

    /**
     *
     */
    private static void loadGain() {

        System.out.println("Loading SVT gains ...");

        ConditionsManager conditions = ConditionsManager.defaultInstance();
        BufferedReader gainReader;

        String filePath = "calibSVT/svt_default.gain";
        String line = null;
        try {
            gainReader = new BufferedReader(conditions.getRawConditions(filePath).getReader());
            while ((line = gainReader.readLine()) != null) {
                // If the line is a comment, skip it
                if (line.indexOf("#") != -1) {
                    continue;
                }
                StringTokenizer stringTok = new StringTokenizer(line);
                while (stringTok.hasMoreTokens()) {
                    int fpga = Integer.valueOf(stringTok.nextToken());
                    int hybrid = Integer.valueOf(stringTok.nextToken());
                    int channel = Integer.valueOf(stringTok.nextToken());
                    double gain = Double.valueOf(stringTok.nextToken());
                    double offset = Double.valueOf(stringTok.nextToken());
                    Pair<Integer, Integer> daqPair = new Pair<Integer, Integer>(fpga, hybrid);
                    if (!gainMap.containsKey(daqPair)) {
                        gainMap.put(daqPair, new ArrayList<Double>());
                    }
                    gainMap.get(daqPair).add(channel, gain);
                    if (!offsetMap.containsKey(daqPair)) {
                        offsetMap.put(daqPair, new ArrayList<Double>());
                    }
                    offsetMap.get(daqPair).add(channel, offset);
                }
            }
        } catch (IOException exception) {
            throw new RuntimeException("Unable to load gains!", exception);
        }
    }

    private static void loadT0Shifts() {
        System.out.println("Loading SVT t0 shifts ...");

        ConditionsManager conditions = ConditionsManager.defaultInstance();
        BufferedReader reader;

        String filePath = "calibSVT/default.t0shift";
        String line;
        try {
            reader = new BufferedReader(conditions.getRawConditions(filePath).getReader());
            while ((line = reader.readLine()) != null) {
                // If the line is a comment, skip it
                if (line.indexOf("#") != -1) {
                    line = line.substring(0, line.indexOf("#"));
                }
                StringTokenizer lineTok = new StringTokenizer(line);
                if (lineTok.countTokens() == 0) {
                    continue;
                }
                if (lineTok.countTokens() != 3) {
                    throw new RuntimeException("Invalid line in t0shift file: " + line);
                }
                int layer = Integer.valueOf(lineTok.nextToken());
                int module = Integer.valueOf(lineTok.nextToken());
                double t0shift = Double.valueOf(lineTok.nextToken());

                t0ShiftMap.put(new Pair(layer, module), t0shift);
            }
        } catch (IOException exception) {
            throw new RuntimeException("Unable to load t0 shifts!", exception);
        }
    }

    public static Double getNoise(SiSensor sensor, int channel) {
        Pair<Integer, Integer> daqPair = SvtUtils.getInstance().getDaqPair(sensor);
        double[] noises = noiseMap.get(daqPair);
        if (noises == null) {
            return null;
        } else {
            return noises[channel];
        }
    }

    public static Double getPedestal(SiSensor sensor, int channel) {
        Pair<Integer, Integer> daqPair = SvtUtils.getInstance().getDaqPair(sensor);
        double[] pedestals = pedestalMap.get(daqPair);
        if (pedestals == null) {
            return null;
        } else {
            return pedestals[channel];
        }
    }

    public static Double getTShaping(SiSensor sensor, int channel) {
        Pair<Integer, Integer> daqPair = SvtUtils.getInstance().getDaqPair(sensor);
        double[] tps = tpMap.get(daqPair);
        if (tps == null) {
            return null;
        } else {
            return tps[channel];
        }
    }

    public static Double getGain(SiSensor sensor, int channel) {
        Pair<Integer, Integer> daqPair = SvtUtils.getInstance().getDaqPair(sensor);
        List<Double> gain = gainMap.get(daqPair);
        if (gain == null) {
            return null;
        } else {
            return gain.get(channel);
        }
    }

    public static Double getOffset(SiSensor sensor, int channel) {
        Pair<Integer, Integer> daqPair = SvtUtils.getInstance().getDaqPair(sensor);
        List<Double> offset = offsetMap.get(daqPair);
        if (offset == null) {
            return null;
        } else {
            return offset.get(channel);
        }
    }

    public static ChannelConstants getChannelConstants(SiSensor sensor, int channel) {
        ChannelConstants constants = new ChannelConstants();

        Double value;
        value = getNoise(sensor, channel);
        if (value != null) {
            constants.setNoise(value);
        } else {
            System.out.println("Couldn't get noise for sensor " + sensor.getName() + ", channel " + channel);
            constants.setNoise(20.0);
        }
        value = getPedestal(sensor, channel);
        if (value != null) {
            constants.setPedestal(value);
        } else {
            System.out.println("Couldn't get pedestal for sensor " + sensor.getName() + ", channel " + channel);
            constants.setPedestal(1638.0);
        }
        value = getTShaping(sensor, channel);
        if (value != null) {
            constants.setTp(value);
        } else {
            System.out.println("Couldn't get Tp for sensor " + sensor.getName() + ", channel " + channel);
            constants.setTp(53.0);
        }

        value = lookupT0Shift(sensor);
        constants.setT0Shift(value);

        return constants;
    }

    /**
     * Checks if a channel has been tagged as bad
     *
     * @param daqPair : a FPGA/Hybrid pair defining which sensor the channels in
     * located on
     * @param channel : The channel to be checked
     * @return true if the channel is bad, false otherwise
     */
    public static boolean isBadChannel(SiSensor sensor, int channel) {
        Pair<Integer, Integer> daqPair = SvtUtils.getInstance().getDaqPair(sensor);
        if (badChannelMap.get(daqPair) != null && badChannelMap.get(daqPair).contains(channel)) {
            return true;
        } else {
            return false;
        }
    }

    public static int getTotalBadChannels() {
        return totalBadChannels;
    }

    //class to hold calibration constants for a channel; leave fields NaN or null if not known
    public static class ChannelConstants {

        private double pedestal = Double.NaN;
        private double tp = Double.NaN;
        private double noise = Double.NaN;
        private double t0Shift = Double.NaN;
        private double[][] pulseShape = null;

        public double getNoise() {
            return noise;
        }

        public void setNoise(double noise) {
            this.noise = noise;
        }

        public double getPedestal() {
            return pedestal;
        }

        public void setPedestal(double pedestal) {
            this.pedestal = pedestal;
        }

        public double[][] getPulseShape() {
            return pulseShape;
        }

        public void setPulseShape(double[][] pulseShape) {
            this.pulseShape = pulseShape;
        }

        public double getTp() {
            return tp;
        }

        public void setTp(double tp) {
            this.tp = tp;
        }

        public double getT0Shift() {
            return t0Shift;
        }

        public void setT0Shift(double t0Shift) {
            this.t0Shift = t0Shift;
        }
    }

    static private double lookupT0Shift(SiSensor sensor) {
        SiTrackerIdentifierHelper helper = (SiTrackerIdentifierHelper) sensor.getIdentifierHelper();
        IIdentifier id = sensor.getIdentifier();
        int layer = helper.getLayerValue(id);
        int module = helper.getModuleValue(id);
        return t0ShiftMap.get(new Pair(layer, module));
    }
}
