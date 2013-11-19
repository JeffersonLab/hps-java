package org.lcsim.hps.evio;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.lcsim.hps.evio.EventConstants.SVT_BANK_NUMBER;
import static org.lcsim.hps.evio.EventConstants.SVT_BANK_TAG;

import org.jlab.coda.jevio.DataType;
import org.jlab.coda.jevio.EventBuilder;
import org.jlab.coda.jevio.EvioBank;
import org.jlab.coda.jevio.EvioException;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.LCRelation;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.hps.recon.tracking.FpgaData;
import org.lcsim.hps.recon.tracking.HPSSVTConstants;
import org.lcsim.hps.recon.tracking.HPSSVTData;
import org.lcsim.hps.recon.tracking.SvtUtils;
import org.lcsim.lcio.LCIOConstants;

/**
 *
 * @author Sho Uemura <meeg@slac.stanford.edu>
 * @version $Id: SVTHitWriter.java,v 1.5 2013/05/22 18:45:33 jeremy Exp $
 */
public class SVTHitWriter implements HitWriter {

    boolean debug = false;
    private String hitCollectionName = "SVTRawTrackerHits";
    private String fpgaDataCollectionName = "FPGAData";
    private String relationCollectionName = "SVTTrueHitRelations";
    String readoutName = "TrackerHits";

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
    private static Map<Integer, FpgaData> makeFpgaData() {
        double[] temps = new double[HPSSVTConstants.TOTAL_HYBRIDS_PER_FPGA * HPSSVTConstants.TOTAL_TEMPS_PER_HYBRID];
        for (int i = 0; i < HPSSVTConstants.TOTAL_HYBRIDS_PER_FPGA * HPSSVTConstants.TOTAL_TEMPS_PER_HYBRID; i++) {
            temps[i] = 23.0;
        }
        Map<Integer, FpgaData> fpgaData = new HashMap<Integer, FpgaData>();
        for (Integer fpgaNumber : SvtUtils.getInstance().getFpgaNumbers()) {
            fpgaData.put(fpgaNumber, new FpgaData(fpgaNumber, temps, 0));
        }

        return fpgaData;
    }

    @Override
    public void writeData(EventHeader event, EventBuilder builder) {

        List<RawTrackerHit> hits = event.get(RawTrackerHit.class, hitCollectionName);
        Map<Integer, FpgaData> fpgaData = makeFpgaData();

        System.out.println("Writing " + hits.size() + " SVT hits");
        System.out.println("Writing " + fpgaData.size() + " FPGA data");

        Map<Integer, List<int[]>> fpgaHits = new HashMap<Integer, List<int[]>>();

        for (Integer fpgaNumber : SvtUtils.getInstance().getFpgaNumbers()) {
            fpgaHits.put(fpgaNumber, new ArrayList<int[]>());
        }

        for (RawTrackerHit hit : hits) {
            int fpgaAddress = SvtUtils.getInstance().getFPGA((SiSensor) hit.getDetectorElement());
            int hybridNumber = SvtUtils.getInstance().getHybrid((SiSensor) hit.getDetectorElement());
            int sensorChannel = hit.getIdentifierFieldValue("strip");
            int apvNumber = HPSSVTData.getAPV(sensorChannel);
            int channelNumber = HPSSVTData.getAPVChannel(sensorChannel);

            int[] data = new int[4];
            HPSSVTData.createSVTDataPacket(hybridNumber, apvNumber, channelNumber, fpgaAddress, hit.getADCValues(), data);
            fpgaHits.get(fpgaAddress).add(data);
        }

        // SVT container bank.
        EvioBank svtBank = new EvioBank(SVT_BANK_TAG, DataType.BANK, SVT_BANK_NUMBER);

        // Iterate over FPGA's 0 - 6
//        for (int fpgaNumber = 0; fpgaNumber < SVT_TOTAL_NUMBER_FPGAS; fpgaNumber++) {
        for (Integer fpgaNumber : SvtUtils.getInstance().getFpgaNumbers()) {
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

            if (debug) {
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
        System.out.println("Writing " + rawTrackerHits.size() + " SVT hits");
        int flags = 1 << LCIOConstants.TRAWBIT_ID1;
        toEvent.put(hitCollectionName, rawTrackerHits, RawTrackerHit.class, flags, readoutName);

        List<LCRelation> trueHitRelations = event.get(LCRelation.class, relationCollectionName);
        toEvent.put(relationCollectionName, trueHitRelations, LCRelation.class, 0);

        List<FpgaData> fpgaData = new ArrayList(makeFpgaData().values());
        System.out.println("Writing " + fpgaData.size() + " FPGA data");

        toEvent.put(fpgaDataCollectionName, fpgaData, FpgaData.class, 0);
    }
}
