package org.hps.conditions.deprecated;

import java.io.BufferedReader;
import org.lcsim.geometry.compact.Subdetector;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.StringTokenizer;
import org.lcsim.conditions.ConditionsManager;
import org.lcsim.detector.identifier.ExpandedIdentifier;
import org.lcsim.detector.identifier.IExpandedIdentifier;
import org.lcsim.detector.identifier.IIdentifierHelper;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;

/**
 *
 * @author meeg
 * @version $Id: EcalConditions.java,v 1.2 2013/10/24 20:01:54 meeg Exp $
 */
public class EcalConditions extends Driver {

    //DAQ channel map
    private static HashMap<Long, Long> daqToPhysicalMap = new HashMap<Long, Long>();
    private static HashMap<Long, Long> physicalToDaqMap = new HashMap<Long, Long>();
    //pedestals
    private static HashMap<Long, Double> daqToPedestalMap = new HashMap<Long, Double>();
    private static HashMap<Long, Double> daqToNoiseMap = new HashMap<Long, Double>();
    //set of bad channels to ignore
    private static HashSet<Long> badChannelsSet = new HashSet<Long>();
    private static boolean badChannelsLoaded = false;
    private static IIdentifierHelper helper = null;
    //gain
    private static HashMap<Long, Double> physicalToGainMap = new HashMap<Long, Double>();
    //subdetector name (for when this is used as a driver)
    private String subdetectorName = "Ecal";
    private static Subdetector subdetector;
    private static boolean debug = false;
    private static boolean calibrationLoaded = false;
    private static String gainFilename = "default.gain";

    public EcalConditions() {
    }

    @Override
    public void detectorChanged(Detector detector) {
        detectorChanged(detector, subdetectorName);
    }

    public static void detectorChanged(Detector detector, String ecalName) {
        subdetector = detector.getSubdetector(ecalName);
        if (subdetector == null) {
            throw new RuntimeException("Subdetector " + ecalName + " not found");
        }
        helper = subdetector.getDetectorElement().getIdentifierHelper();
    }

    public static boolean calibrationLoaded() {
        return calibrationLoaded;
    }

    public static void loadDaqMap(Detector detector, String ecalName) {
        detectorChanged(detector, ecalName);
        fillDaqCellMap(subdetector);
    }

    public static void loadCalibration() {
        fillDaqCellMap(subdetector);
        loadBadChannels(subdetector);
        loadGains();
        loadPedestals();
        calibrationLoaded = true;
    }
    
    public static void setGainFilename(String name) {
    	gainFilename = name;
    }

    public void setSubdetectorName(String subdetectorName) {
        this.subdetectorName = subdetectorName;
    }

    public static IIdentifierHelper getHelper() {
        return helper;
    }

    public static Subdetector getSubdetector() {
        return subdetector;
    }

    public static boolean badChannelsLoaded() {
        return badChannelsLoaded;
    }

    public static void loadPedestals() {
        ConditionsManager conditions = ConditionsManager.defaultInstance();
        try {
            Reader pedestalsReader = conditions.getRawConditions("calibECal/default01.ped").getReader();
            loadPedestals(pedestalsReader, 1);
            pedestalsReader = conditions.getRawConditions("calibECal/default02.ped").getReader();
            loadPedestals(pedestalsReader, 2);
        } catch (IOException e) {
            throw new RuntimeException("couldn't get pedestals file", e);
        }
    }

    public static void loadPedestals(Reader reader, int crate) {

        System.out.println("reading pedestals for ECal");

        BufferedReader bufferedReader = new BufferedReader(reader);
        String line;
        while (true) {
            try {
                line = bufferedReader.readLine();
            } catch (IOException e) {
                throw new RuntimeException("couldn't parse pedestals file", e);
            }
            if (line == null) {
                break;
            }

            if (line.indexOf("#") != -1) {
                line = line.substring(0, line.indexOf("#"));
            }

            StringTokenizer lineTok = new StringTokenizer(line);

            if (lineTok.countTokens() != 0) {
                if (lineTok.countTokens() != 4) {
                    throw new RuntimeException("Invalid line in pedestals file: " + line);
                } else {
                    short slot = Short.valueOf(lineTok.nextToken());
                    short channel = Short.valueOf(lineTok.nextToken());
                    double pedestal = Double.valueOf(lineTok.nextToken());
                    double noise = Double.valueOf(lineTok.nextToken());
                    long daqid = getDaqID(crate, slot, channel);
                    daqToPedestalMap.put(daqid, pedestal);
                    daqToNoiseMap.put(daqid, noise);
                    if (debug) {
                        System.out.printf("Channel %d: pede %.2f noise %.2f (crate %d slot %d channel %d)\n", daqid,pedestal,noise,crate,slot,channel);
                    }
                }
            }
        }
    }

    private static void loadBadChannels(Subdetector ecal) {

        System.out.println("reading ECal bad channels");

        IExpandedIdentifier expId = new ExpandedIdentifier(helper.getIdentifierDictionary().getNumberOfFields());
        expId.setValue(helper.getFieldIndex("system"), ecal.getSystemID());
        ConditionsManager conditions = ConditionsManager.defaultInstance();
        BufferedReader bufferedReader;
        try {
            bufferedReader = new BufferedReader(conditions.getRawConditions("daqmap/ecal.badchannels").getReader());
        } catch (IOException e) {
            throw new RuntimeException("couldn't get ECal bad channels from conditions manager", e);
        }
        String line;
        while (true) {
            try {
                line = bufferedReader.readLine();
            } catch (IOException e) {
                throw new RuntimeException("couldn't parse ECal bad channels", e);
            }
            if (line == null) {
                break;
            }

            if (line.indexOf("#") != -1) {
                line = line.substring(0, line.indexOf("#"));
            }

            StringTokenizer lineTok = new StringTokenizer(line);

            if (lineTok.countTokens() != 0) {
                if (lineTok.countTokens() != 2) {
                    throw new RuntimeException("Invalid line in ECal bad channels: " + line);
                } else {
                    int x = Integer.valueOf(lineTok.nextToken());
                    int y = Integer.valueOf(lineTok.nextToken());
                    expId.setValue(helper.getFieldIndex("ix"), x);
                    expId.setValue(helper.getFieldIndex("iy"), y);
                    badChannelsSet.add(helper.pack(expId).getValue());
                    if (debug) {
                        System.out.printf("Channel %d is bad (x=%d y=%d)\n", helper.pack(expId).getValue(),x,y);
                    }
                }
            }
        }
        badChannelsLoaded = true;
    }

    public static void loadGains() {
        if (debug) {
            System.out.println("Loading gains");
        }
        BufferedReader bufferedReader;
        ConditionsManager conditions = ConditionsManager.defaultInstance();
        try {
        	bufferedReader = new BufferedReader(conditions.getRawConditions("calibECal/"+EcalConditions.gainFilename).getReader());
        } catch (IOException e) {
            throw new RuntimeException("couldn't get gain file("+gainFilename+") ", e);
        }

        String line;
        while (true) {
            try {
                line = bufferedReader.readLine();
            } catch (IOException e) {
                throw new RuntimeException("couldn't parse gain file", e);
            }
            if (line == null) {
                break;
            }

            if (line.indexOf("#") != -1) {
                line = line.substring(0, line.indexOf("#"));
            }

            StringTokenizer lineTok = new StringTokenizer(line);

            if (lineTok.countTokens() != 0) {
                if (lineTok.countTokens() != 3) {
                    throw new RuntimeException("Invalid line in gain file: " + line);
                } else {
                    int x = Integer.valueOf(lineTok.nextToken());
                    int y = Integer.valueOf(lineTok.nextToken());
                    double gain = Double.valueOf(lineTok.nextToken());
                    physicalToGainMap.put(makePhysicalID(x, y), gain);
                    if (debug) {
                        System.out.printf("Channel %d: gain %.2f (x=%d y=%d)\n", makePhysicalID(x, y),gain,x,y);
                    }
                }
            }
        }
    }

    public static boolean isBadChannel(long id) {
        return badChannelsSet.contains(id);
    }

    private static void fillDaqCellMap(Subdetector ecal) {

        System.out.println("reading ECal DAQ map");

        IExpandedIdentifier expId = new ExpandedIdentifier(helper.getIdentifierDictionary().getNumberOfFields());
        expId.setValue(helper.getFieldIndex("system"), ecal.getSystemID());

        ConditionsManager conditions = ConditionsManager.defaultInstance();
        BufferedReader bufferedReader;
        try {
            bufferedReader = new BufferedReader(conditions.getRawConditions("daqmap/ecal.txt").getReader());
        } catch (IOException e) {
            throw new RuntimeException("couldn't get DAQ map from conditions manager", e);
        }
        String line;
        while (true) {
            try {
                line = bufferedReader.readLine();
            } catch (IOException e) {
                throw new RuntimeException("couldn't parse ECal DAQ map", e);
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
                    throw new RuntimeException("Invalid line in ECal DAQ map: " + line);
                } else {
                    int x = Integer.valueOf(lineTok.nextToken());
                    int y = Integer.valueOf(lineTok.nextToken());
//                    if (x>0 && y>0) x = 24-x;
                    expId.setValue(helper.getFieldIndex("ix"), x);
                    expId.setValue(helper.getFieldIndex("iy"), y);
                    int crate = Integer.valueOf(lineTok.nextToken());
                    short slot = Short.valueOf(lineTok.nextToken());
                    short channel = Short.valueOf(lineTok.nextToken());
                    addMapEntry(helper.pack(expId).getValue(), getDaqID(crate, slot, channel));
                }
            }
        }
    }

    public static long makePhysicalID(int ix, int iy) {
        IExpandedIdentifier expId = new ExpandedIdentifier(helper.getIdentifierDictionary().getNumberOfFields());
        expId.setValue(helper.getFieldIndex("system"), subdetector.getSystemID());
        expId.setValue(helper.getFieldIndex("ix"), ix);
        expId.setValue(helper.getFieldIndex("iy"), iy);
        return helper.pack(expId).getValue();
    }

    private static void addMapEntry(long physicalID, long daqID) {
        daqToPhysicalMap.put(daqID, physicalID);
        physicalToDaqMap.put(physicalID, daqID);
    }

    public static long getDaqID(int crate, short slot, short channel) {
        return (((long) crate) << 32) | ((long) slot << 16) | (long) channel;
    }

    public static Long daqToPhysicalID(int crate, short slot, short channel) {
        return daqToPhysicalMap.get(getDaqID(crate, slot, channel));
    }

    public static int getCrate(long daqID) {
        return (int) (daqID >>> 32);
    }

    public static short getSlot(long daqID) {
        return (short) ((daqID >>> 16) & 0xFFFF);
    }

    public static short getChannel(long daqID) {
        return (short) (daqID & 0xFFFF);
    }

    public static Long physicalToDaqID(long physicalID) {
        return physicalToDaqMap.get(physicalID);
    }

    public static Long daqToPhysicalID(long daqID) {
        return daqToPhysicalMap.get(daqID);
    }

    public static Double daqToPedestal(long daqID) {
        return daqToPedestalMap.get(daqID);
    }

    public static Double physicalToPedestal(long physicalID) {
        return daqToPedestalMap.get(physicalToDaqMap.get(physicalID));
    }

    public static Double physicalToNoise(long physicalID) {
        return daqToNoiseMap.get(physicalToDaqMap.get(physicalID));
    }

    public static Double physicalToGain(long physicalID) {
        return physicalToGainMap.get(physicalID);
    }
}
