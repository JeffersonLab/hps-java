package org.hps.recon.ecal;

import hep.aida.IHistogram1D;

import java.io.FileWriter;
import java.io.IOException;

import org.hps.conditions.database.TableConstants;
import org.hps.conditions.ecal.EcalChannel;
import org.hps.conditions.ecal.EcalConditions;
import org.lcsim.conditions.ConditionsManager;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.event.LCRelation;
import org.lcsim.event.RawCalorimeterHit;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 * Calculate pedestals from Mode-7 Data.
 * 
 * To be used online with org.hps.monitoring.ecal.plots.EcalPedestalViewer
 * and ET-ring to create config files for DAQ and/or conditions DB.
 * 
 * When user clicks "Disconnect" in monitoring app (after confirming sufficient
 * statistics), endOfData is called and config files will be written.  Copying
 * files to proper location for DAQ must be done manually.
 *gg
 * TODO: Merge with EcalCalibrationDriver (which works on Mode-1).
 * 
 * @version $Id: EcalPedestalCalculator.java,v 0.1 2015/02/20 00:00:00
 * @author <baltzell@jlab.org>
 */
public class EcalPedestalCalculator extends Driver {

    private static final String rawCollectionName = "EcalReadoutHits";
    private static final String extraDataRelationsName = "EcalReadoutExtraDataRelations";

    private String histoNameFormat = "Ecal/Pedestals/Mode7/ped%3d";
    
    private String outputFilePrefix = "";
    private static final String[] filenamesDAQ = { "fadc37.ped", "fadc39.ped" };
    private static final String filenameDB = "EcalPedsForDB.txt";
    
    // The number of samples used by FADCs to report an event's pedestal.
    private int nSamples = 4;
    
    private boolean writeFileForDB=true;
    private boolean writeFileForDAQ=true;
    
    private EcalConditions ecalConditions = null;

    AIDA aida = AIDA.defaultInstance();

    private int nDetectorChanges = 0;

    public EcalPedestalCalculator() {
    }

    public void setOutputFilePrefix(String prefix) {
        outputFilePrefix = "_"+prefix;
    }

    @Override
    protected void startOfData() {
    }

    @Override
    public void endOfData() {
        if (writeFileForDAQ) writeFileForDAQ();
        if (writeFileForDB)  writeFileForDB();
    }

    @Override
    public void detectorChanged(Detector detector) {

        if (nDetectorChanges++ > 1) {
            throw new RuntimeException("No Detector Change Allowed.");
        }
        ecalConditions = ConditionsManager.defaultInstance()
                .getCachedConditions(EcalConditions.class,TableConstants.ECAL_CONDITIONS)
                .getCachedData();

        aida.tree().cd("/");
        for (EcalChannel cc : ecalConditions.getChannelCollection()) {
            aida.histogram1D(getHistoName(cc),181,19.5,200.5);
        }

    }

    private String getHistoName(EcalChannel cc) {
        return String.format(histoNameFormat,cc.getChannelId());
    }

    private void writeFileForDB() {
        FileWriter wout = null;
        try {
            wout = new FileWriter(outputFilePrefix + filenameDB);
            for (int cid=1; cid<=442; cid++) {
                EcalChannel cc= findChannel(cid);
                IHistogram1D hh = aida.histogram1D(getHistoName(cc));
                wout.write(String.format("%3d %7.3f %7.3f\n",cid,
                           hh.mean(),hh.rms()*Math.sqrt(nSamples)));
            }
        } catch (IOException ee) {
            throw new RuntimeException("Error writing file.",ee);
        } finally {
            if (wout != null) {
                try {
                    wout.close();
                } catch (IOException ee) {
                    ee.printStackTrace();
                }
            }
        }
    }
    private void writeFileForDAQ() {
        FileWriter[] wout = { null, null };
        try {
            for (int crate = 1; crate <= 2; crate++) {
                wout[crate - 1] = new FileWriter(outputFilePrefix + filenamesDAQ[crate - 1]);
                for (int slot = 3; slot <= 20; slot++) {
                    for (int chan = 0; chan < 16; chan++) {
                        if (slot > 9 && slot < 14)
                            continue;
                        if (slot == 20 && chan > 12)
                            continue;
                        EcalChannel cc = findChannel(crate,slot,chan);
                        IHistogram1D hh = aida.histogram1D(getHistoName(cc));
                        wout[crate - 1].write(String.format("%2d %2d %7.3f %7.3f\n",slot,chan,
                                hh.mean(),hh.rms()*Math.sqrt(nSamples)));
                    }
                }
            }
        } catch (IOException ee) {
            throw new RuntimeException("Error writing file.",ee);
        } finally {
            if (wout[0] != null || wout[1] != null) {
                try {
                    if (wout[0] != null)
                        wout[0].close();
                    if (wout[1] != null)
                        wout[1].close();
                } catch (IOException ee) {
                    ee.printStackTrace();
                }
            }
        }
    }

    @Override
    protected void process(EventHeader event) {
        if (event.hasCollection(RawCalorimeterHit.class,rawCollectionName)) {
            if (event.hasCollection(LCRelation.class,extraDataRelationsName)) {
                for (LCRelation rel : event.get(LCRelation.class,extraDataRelationsName)) {
                    RawCalorimeterHit hit = (RawCalorimeterHit) rel.getFrom();
                    GenericObject extraData = (GenericObject) rel.getTo();
                    fillHisto(event,hit,extraData);
                }
            }
        }
    }

    private void fillHisto(EventHeader event, RawCalorimeterHit hit, GenericObject mode7data) {
        final int min = ((HitExtraData.Mode7Data) mode7data).getAmplLow();
        final int max = ((HitExtraData.Mode7Data) mode7data).getAmplHigh();
        // ignore if pulse at beginning of window:
        if (max <= 0) return;
        EcalChannel cc = findChannel(hit);
        if (cc == null) {
            System.err.println("Hit doesn't correspond to ecalchannel.");
            return;
        }
        aida.histogram1D(getHistoName(cc)).fill(min);
    }

    public EcalChannel findChannel(int channel_id) {
        return ecalConditions.getChannelCollection().findChannel(channel_id);
    }

    public EcalChannel findChannel(RawCalorimeterHit hit) {
        return ecalConditions.getChannelCollection().findGeometric(hit.getCellID());
    }

    public EcalChannel findChannel(int crate, int slot, int chan) {
        for (EcalChannel cc : ecalConditions.getChannelCollection()) {
            if (crate == cc.getCrate() && 
                 slot == cc.getSlot()  && 
                 chan == cc.getChannel()) {
                return cc;
            }
        }
        throw new RuntimeException(String.format(
                "Could not find channel:  (crate,slot,channel)=(%d,%d,%d)",crate,slot,chan));
    }

}
