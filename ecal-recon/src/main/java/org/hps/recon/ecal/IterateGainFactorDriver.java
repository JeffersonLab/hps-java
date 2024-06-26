package org.hps.recon.ecal;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.ecal.EcalChannelConstants;
import org.hps.conditions.ecal.EcalConditions;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.EventHeader;
import org.lcsim.geometry.Detector;
import org.lcsim.lcio.LCIOConstants;
import org.lcsim.util.Driver;

/**
 * This driver reads in a hit collection and outputs a new hit collection with
 * energies that have been multiplied by an improved gain correction factor.
 * This is primarily used for the elastic energy calibration.
 */
public class IterateGainFactorDriver extends Driver {

    private boolean isGainFileRead = false;
    private EcalConditions ecalConditions = null;

    /**
     * Set the input collection name (source).
     *
     * @param inputCollectionName
     *            the input collection name
     */
    private String inputCollectionName = "EcalCalHits";

    public void setInputCollectionName(final String inputCollectionName) {
        this.inputCollectionName = inputCollectionName;
    }

    /**
     * Set the output collection name (target).
     *
     * @param outputCollectionName
     *            the output collection name
     */
    private String outputCollectionName = "IterHits1";

    public void setOutputCollectionName(final String outputCollectionName) {
        this.outputCollectionName = outputCollectionName;
    }

    private final String ecalReadoutName = "EcalHits";
    private int calibYear=0;
    private ArrayList<Integer> badChannels = null;
    /*
     * Basic no argument constructor.
       A.C.: I add a year-specific set of crystals that are broken, with the corresponding energy to be set to zero for both data and MC.
       For the moment, this is hard-coded
     */
    public IterateGainFactorDriver() {
        badChannels=new ArrayList<Integer>();
    }

    public void setGainFile(String filename) {
        this.gainFileName = filename;
    }
    
    public void setCalibYear(int year) {
        this.calibYear=year;
        System.out.println("IterateGainFactorDriver: setting calib year to "+this.calibYear);  
      
    }
    
    
    public void setBadChannels() {
        if (this.calibYear==2021) {
            System.out.println("Adding the 2021 bad channels");
            badChannels.add(6);
            badChannels.add(15);
            badChannels.add(26);     
            badChannels.add(153);
            badChannels.add(192);
            badChannels.add(198);
            badChannels.add(275);
            badChannels.add(334);
            badChannels.add(419);       
        }
        System.out.println("IterateGainFactorDriver: bad channels are "+badChannels);    
    }

    /**
     * Read in a text file that has multiplicative factors on the original gain
     * values. These correction factors can be multiplied directly onto the energy
     * of the reconstructed hits and saved to a new list.
     */
    private Map<Integer, Double> gainFileGains = new HashMap<Integer, Double>();
    public String gainFileName = null;

    private void readGainFile() {
        if (isGainFileRead == true)
            return;
        gainFileGains.clear();
        // System.out.println("read the file");
        System.out.println("Reading ECal Gain Factors from:  " + gainFileName);
        File file = new File(gainFileName);
        try {
            FileReader reader = new FileReader(file);
            char[] chars = new char[(int) file.length()];
            reader.read(chars);
            String content = new String(chars);
            reader.close();
            String lines[] = content.split("\n");
            int nlines = 0;
            for (String line : lines) {
                String columns[] = line.split(",");
                if (nlines++ > 0) {
                    final int channelid = Integer.valueOf(columns[0]);
                    final double gain = Double.valueOf(columns[1]);
                    if (gainFileGains.containsKey(channelid)) {
                        throw new RuntimeException("Duplicate Entries in ECal Gain File.");
                    }
                    gainFileGains.put(channelid, gain);
                }
            }
            if (nlines != 442 + 1) {
                System.err.println("Invalid Gain File.");
                System.exit(3);
            }
            isGainFileRead = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("ECAL Gain Factors were read");
    }

    @Override
    public void detectorChanged(Detector detector) {
        // ECAL combined conditions object.
        ecalConditions = DatabaseConditionsManager.getInstance().getEcalConditions();
        readGainFile();
        setBadChannels();
    }

    /**
     * Copy hits to a new collection (list) while multiplying the energies by the
     * new gain factors.
     *
     * @param hits
     *            the input hit list
     * @return the output hit collection with gain corrected energies
     */
    public List<CalorimeterHit> iterateHits(final List<CalorimeterHit> hits) {
        ArrayList<CalorimeterHit> newHits = new ArrayList<CalorimeterHit>();
        for (final CalorimeterHit hit : hits) {
            double time = hit.getTime();
            long cellID = hit.getCellID();
            /*If the channels is not flagged as "bad", re-compute the energy and create a new CalorimterHit
             * Otherwise ignore the hit
             * */    
            if (this.badChannels.contains(findChannelId(cellID))==false) {
                double energy = hit.getCorrectedEnergy() * gainFileGains.get(findChannelId(cellID));
                CalorimeterHit newHit = CalorimeterHitUtilities.create(energy, time, cellID);
                newHits.add(newHit);
            }
        }
        return newHits;
    }

    /**
     * Process an event, copying the input collection to the output collection.
     *
     * @param event
     *            the LCSim event
     */
    @Override
    public void process(final EventHeader event) {

        // Check if output collection already exists in event which is an error.
        if (event.hasItem(outputCollectionName)) {
            throw new RuntimeException("collection " + outputCollectionName + " already exists in event");
        }

        // Get the input collection.
        if (event.hasCollection(CalorimeterHit.class, inputCollectionName)) {

            final List<CalorimeterHit> inputHitCollection = event.get(CalorimeterHit.class, inputCollectionName);

            // Iterate the gain correction coefficient on each hit.
            final List<CalorimeterHit> outputHitCollection = this.iterateHits(inputHitCollection);

            int flags = 0;
            flags += 1 << LCIOConstants.RCHBIT_TIME; // store hit time
            flags += 1 << LCIOConstants.RCHBIT_LONG; // store hit position; this flag has no effect for
                                                     // RawCalorimeterHits

            // Put the collection into the event.
            event.put(outputCollectionName, outputHitCollection, CalorimeterHit.class, flags, ecalReadoutName);
        }
    }

    /**
     * Start of data hook which will make sure required arguments are set properly.
     */
    @Override
    public void startOfData() {
        if (inputCollectionName == null) {
            throw new RuntimeException("inputCollectionName was never set");
        }
        if (outputCollectionName == null) {
            throw new RuntimeException("outputCollectionName was never set");
        }
        if (inputCollectionName.equals(outputCollectionName)) {
            throw new IllegalArgumentException("inputCollectionName and outputCollectionName are the same");
        }
    }

    /**
     * Convert physical ID to gain value.
     *
     * @param cellID
     *            (long)
     * @return channel constants (EcalChannelConstants)
     */
    public EcalChannelConstants findChannel(long cellID) {
        return ecalConditions.getChannelConstants(ecalConditions.getChannelCollection().findGeometric(cellID));
    }

    public Integer findChannelId(long cellID) {
        return ecalConditions.getChannelCollection().findGeometric(cellID).getChannelId();
    }
}
