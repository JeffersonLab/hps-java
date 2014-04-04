package org.hps.evio;

//--- java ---//
import java.util.ArrayList;
import java.util.List;

import org.hps.conditions.deprecated.HPSSVTConstants;
import org.hps.conditions.deprecated.SvtUtils;
import org.hps.readout.svt.FpgaData;
import org.hps.readout.svt.SVTData;
//--- Coda ---//
import org.jlab.coda.jevio.BaseStructure;
import org.jlab.coda.jevio.EvioEvent;
//--- org.lcsim ---//
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
//--- hps-java ---//
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.base.BaseRawTrackerHit;
import org.hps.util.Pair;
import org.lcsim.lcio.LCIOUtil;


//--- Constants ---//
import static org.hps.evio.EventConstants.SVT_BANK_TAG;

/**
 *
 * @author Omar Moreno <omoreno1@ucsc.edu>
 * @version $Id: SVTEvioReader.java,v 1.10 2013/07/27 01:52:49 omoreno Exp $
 */
public class SVTEvioReader extends EvioReader {

    String fpgaDataCollectionName = "FPGAData";
    String readoutName = "TrackerHits";

    /**
     *
     */
    public SVTEvioReader() {
        hitCollectionName = "SVTRawTrackerHits";
        debug = false;
    }

    public void setReadoutName(String readoutName) {
        this.readoutName = readoutName;
    }

    /**
     *
     */
    @Override
    public boolean makeHits(EvioEvent event, EventHeader lcsimEvent) {
        // Create DAQ Maps
        if (!SvtUtils.getInstance().isSetup()) {
            SvtUtils.getInstance().setup(lcsimEvent.getDetector());
        }

        // Create a list to hold the temperatures 
        List<FpgaData> fpgaDataCollection = new ArrayList<FpgaData>();

        List<RawTrackerHit> hits = new ArrayList<RawTrackerHit>();

        boolean foundBank = false;
        for (BaseStructure crateBank : event.getChildren()) {
            int crateTag = crateBank.getHeader().getTag();

            // Process only events inside the SVT Bank
            if (crateTag == SVT_BANK_TAG) {
                foundBank = true;
                if (crateBank.getChildCount() == 0) {
                    throw new RuntimeException("No children found in SVT bank!");
                }

                // Loop over all FPGA banks
                for (BaseStructure fpgaBank : crateBank.getChildren()) {
                    int fpgaID = fpgaBank.getHeader().getTag();
                    if (fpgaID < 0 || fpgaID >= HPSSVTConstants.SVT_TOTAL_FPGAS) {
                        System.out.println("Unexpected FPGA bank tag " + fpgaID);
                    }

                    // The data contained in FPGA 7 is currently not used
                    if (fpgaBank.getHeader().getTag() == 7) {
                        continue;
                    }

                    // Get data
                    int[] data = fpgaBank.getIntData();

                    if (debug) {
                        System.out.println(this.getClass().getSimpleName() + ": The data size is " + data.length);
                    }

                    if (debug) {
                        for (int index = 0; index < data.length; index++) {
                            System.out.println("Data " + index + ": " + data[index]);
                        }
                    }

                    // Get the hybrid temperature data associated with this FPGA
                    int[] temperatureData = new int[6];
                    System.arraycopy(data, 1, temperatureData, 0, 6);
                    FpgaData fpgaData = new FpgaData(fpgaID, temperatureData, data[data.length - 1]);
                    fpgaDataCollection.add(fpgaData);


                    if (debug) {
                        System.out.println(this.getClass().getSimpleName() + ": The temperatures are: ");
                        double[] temps = SVTData.getTemperature(temperatureData);
                        for (int index = 0; index < temps.length; index++) {
                            System.out.println("Temp " + index + ": " + temps[index]);
                        }
                    }

                    // Get all of the samples
                    int sampleLength = data.length - temperatureData.length - 2; // Tail length
                    int[] allSamples = new int[sampleLength];
                    System.arraycopy(data, 7, allSamples, 0, sampleLength);

                    if (debug) {
                        for (int index = 0; index < allSamples.length; index++) {
                            System.out.println("Sample " + index + ": " + allSamples[index]);
                        }
                    }

                    // Check whether a complete set of samples exist
                    if (allSamples.length % 4 != 0) {
                        throw new RuntimeException("Size of samples array is not divisible by 4!");
                    }

                    // Loop over all samples and create HPSSVTData
                    for (int index = 0; index < allSamples.length; index += 4) {
                        int[] samples = new int[4];
                        System.arraycopy(allSamples, index, samples, 0, samples.length);
                        hits.add(makeHit(samples));
                    }
                }
            }
        }
        if (debug) {
            System.out.println("Adding RawTrackerHit Collection of Size " + hits.size());
        }

        lcsimEvent.put(fpgaDataCollectionName, fpgaDataCollection, GenericObject.class, 0);
        int flag = LCIOUtil.bitSet(0, 31, true); // Turn on 64-bit cell ID.
        lcsimEvent.put(hitCollectionName, hits, RawTrackerHit.class, flag, readoutName);

        return foundBank;
    }

    private static RawTrackerHit makeHit(int[] data) {
        int hitTime = 0;
        Pair<Integer, Integer> daqPair = new Pair<Integer, Integer>(SVTData.getFPGAAddress(data), SVTData.getHybridNumber(data));
        SiSensor sensor = SvtUtils.getInstance().getSensor(daqPair);

        int sensorChannel = SVTData.getSensorChannel(data);
        long cell_id = SvtUtils.makeCellID(sensor, sensorChannel);

        return new BaseRawTrackerHit(hitTime, cell_id, SVTData.getAllSamples(data), null, sensor);
    }
}
