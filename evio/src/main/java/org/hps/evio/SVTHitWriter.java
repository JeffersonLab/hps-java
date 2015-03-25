package org.hps.evio;

import static org.hps.evio.EventConstants.SVT_BANK_NUMBER;
import static org.hps.evio.EventConstants.SVT_BANK_TAG;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//===> import org.hps.conditions.deprecated.SvtUtils;
import org.hps.readout.svt.FpgaData;
import org.hps.readout.svt.HPSSVTConstants;
import org.hps.readout.svt.SVTData;
import org.jlab.coda.jevio.DataType;
import org.jlab.coda.jevio.EventBuilder;
import org.jlab.coda.jevio.EvioBank;
import org.jlab.coda.jevio.EvioException;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.detector.tracker.silicon.HpsTestRunSiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.LCRelation;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.geometry.Subdetector;
import org.lcsim.lcio.LCIOConstants;

/**
 *
 * @author Sho Uemura <meeg@slac.stanford.edu>
 */
// TODO: Update this class so it works correctly with the database conditions system
public class SVTHitWriter implements HitWriter {

    private int verbosity = 1;

    // Subdetector name
    private static final String subdetectorName = "Tracker";

    // Collection names
    private String hitCollectionName = "SVTRawTrackerHits";
    private String fpgaDataCollectionName = "FPGAData";
    private String relationCollectionName = "SVTTrueHitRelations";
    private String readoutName = "TrackerHits";

    List<Integer> fpgaNumbers = new ArrayList<Integer>();

    public SVTHitWriter() {
    }

    public void setHitCollectionName(String hitCollectionName) {
        this.hitCollectionName = hitCollectionName;
    }

    @Override
    public boolean hasData(EventHeader event) {
        return event.hasCollection(RawTrackerHit.class, hitCollectionName);
    }

    //make some dummy FpgaData to use in case there isn't any real FpgaData
    private Map<Integer, FpgaData> makeFpgaData(Subdetector subdetector) {
        double[] temps = new double[HPSSVTConstants.TOTAL_HYBRIDS_PER_FPGA * HPSSVTConstants.TOTAL_TEMPS_PER_HYBRID];
        for (int i = 0; i < HPSSVTConstants.TOTAL_HYBRIDS_PER_FPGA * HPSSVTConstants.TOTAL_TEMPS_PER_HYBRID; i++) {
            temps[i] = 23.0;
        }

        Map<Integer, FpgaData> fpgaData = new HashMap<Integer, FpgaData>();
        List<HpsSiSensor> sensors = subdetector.getDetectorElement().findDescendants(HpsSiSensor.class);

        for (HpsSiSensor sensor : sensors) {
            if (sensor instanceof HpsTestRunSiSensor && !fpgaNumbers.contains(((HpsTestRunSiSensor) sensor).getFpgaID())) {
                fpgaNumbers.add(((HpsTestRunSiSensor) sensor).getFpgaID());
            }
        }
        //===> for (Integer fpgaNumber : SvtUtils.getInstance().getFpgaNumbers()) {
        for (Integer fpgaNumber : fpgaNumbers) {
            fpgaData.put(fpgaNumber, new FpgaData(fpgaNumber, temps, 0));
        }

        return fpgaData;
    }

    @Override
    public void writeData(EventHeader event, EventBuilder builder) {

        List<RawTrackerHit> hits = event.get(RawTrackerHit.class, hitCollectionName);
        Map<Integer, FpgaData> fpgaData = makeFpgaData(event.getDetector().getSubdetector(subdetectorName));

        if (verbosity >= 1) {
            System.out.println("Writing " + hits.size() + " SVT hits");
            System.out.println("Writing " + fpgaData.size() + " FPGA data");
        }

        Map<Integer, List<int[]>> fpgaHits = new HashMap<Integer, List<int[]>>();

        //===> for (Integer fpgaNumber : SvtUtils.getInstance().getFpgaNumbers()) {
        for (Integer fpgaNumber : fpgaNumbers) {
            fpgaHits.put(fpgaNumber, new ArrayList<int[]>());
        }

        for (RawTrackerHit hit : hits) {
            //===> int fpgaAddress = SvtUtils.getInstance().getFPGA((SiSensor) hit.getDetectorElement());
            int fpgaAddress = ((HpsTestRunSiSensor) hit.getDetectorElement()).getFpgaID();
            //int hybridNumber = SvtUtils.getInstance().getHybrid((SiSensor) hit.getDetectorElement());
            int hybridNumber = ((HpsTestRunSiSensor) hit.getDetectorElement()).getFpgaID();
            int sensorChannel = hit.getIdentifierFieldValue("strip");
            int apvNumber = SVTData.getAPV(sensorChannel);
            int channelNumber = SVTData.getAPVChannel(sensorChannel);

            int[] data = new int[4];
            SVTData.createSVTDataPacket(hybridNumber, apvNumber, channelNumber, fpgaAddress, hit.getADCValues(), data);
            fpgaHits.get(fpgaAddress).add(data);
        }

        // SVT container bank.
        EvioBank svtBank = new EvioBank(SVT_BANK_TAG, DataType.BANK, SVT_BANK_NUMBER);

        // Iterate over FPGA's 0 - 6
//        for (int fpgaNumber = 0; fpgaNumber < SVT_TOTAL_NUMBER_FPGAS; fpgaNumber++) {
        //===> for (Integer fpgaNumber : SvtUtils.getInstance().getFpgaNumbers()) {
        for (Integer fpgaNumber : fpgaNumbers) {
            FpgaData fpgaDatum = fpgaData.get(fpgaNumber);
            int[] header = fpgaDatum.extractData();

            // Get the raw int data buffer for this FPGA.
            int[] dataBuffer = new int[header.length + 4 * fpgaHits.get(fpgaNumber).size() + 2];
            int ptr = 0;

            dataBuffer[ptr++] = 0;

            System.arraycopy(header, 0, dataBuffer, ptr, header.length);
            ptr += header.length;

            for (int[] data : fpgaHits.get(fpgaNumber)) {
                System.arraycopy(data, 0, dataBuffer, ptr, data.length);
                ptr += data.length;
            }

            dataBuffer[ptr++] = fpgaDatum.getTail();

            if (ptr != dataBuffer.length) {
                throw new RuntimeException("tried to fill SVT buffer of length " + dataBuffer.length + " with " + ptr + " ints");
            }

            if (verbosity >= 2) {
                System.out.println(this.getClass().getSimpleName() + ": FPGA " + fpgaNumber + " : Data size: " + dataBuffer.length);
            }

            // Bank for this FPGA's frame data.
            EvioBank frameBank = new EvioBank(fpgaNumber, DataType.UINT32, fpgaNumber);
            try {
                // Add the FPGA bank to the SVT bank
                builder.addChild(svtBank, frameBank);
                // Add the SVT data to the FPGA bank
                frameBank.appendIntData(dataBuffer);
            } catch (EvioException e) {
                throw new RuntimeException(e);
            }
        }
        // Add the SVT bank to the Main EVIO bank
        try {
            builder.addChild(builder.getEvent(), svtBank);
        } catch (EvioException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void writeData(EventHeader event, EventHeader toEvent) {
        List<RawTrackerHit> rawTrackerHits = event.get(RawTrackerHit.class, hitCollectionName);
        if (verbosity >= 1) {
            System.out.println("Writing " + rawTrackerHits.size() + " SVT hits");
        }
        int flags = 1 << LCIOConstants.TRAWBIT_ID1;
        toEvent.put(hitCollectionName, rawTrackerHits, RawTrackerHit.class, flags, readoutName);

        List<LCRelation> trueHitRelations = event.get(LCRelation.class, relationCollectionName);
        toEvent.put(relationCollectionName, trueHitRelations, LCRelation.class, 0);

        List<FpgaData> fpgaData = new ArrayList(makeFpgaData(event.getDetector().getSubdetector(subdetectorName)).values());
        if (verbosity >= 1) {
            System.out.println("Writing " + fpgaData.size() + " FPGA data");
        }
        toEvent.put(fpgaDataCollectionName, fpgaData, FpgaData.class, 0);
    }

    @Override
    public void setVerbosity(int verbosity) {
        this.verbosity = verbosity;
    }
}
