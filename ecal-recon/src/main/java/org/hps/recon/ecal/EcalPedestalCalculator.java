package org.hps.recon.ecal;

import hep.aida.IHistogram1D;

import java.io.Console;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.hps.conditions.api.ConditionsObjectException;
import org.hps.conditions.api.ConditionsRecord;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.database.TableMetaData;
import org.hps.conditions.ecal.EcalCalibration;
import org.hps.conditions.ecal.EcalCalibration.EcalCalibrationCollection;
import org.hps.conditions.ecal.EcalChannel;
import org.hps.conditions.ecal.EcalConditions;
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
 * To be used online with org.hps.monitoring.ecal.plots.EcalPedestalViewer and
 * ET-ring to create config files for DAQ and/or conditions DB.
 * 
 * When user clicks "Disconnect" in monitoring app (after confirming sufficient
 * statistics), endOfData is called and config files will be written. Copying
 * files to proper location for DAQ must be done manually. gg TODO: Merge with
 * EcalCalibrationDriver (which works on Mode-1).
 * 
 * @version $Id: EcalPedestalCalculator.java,v 0.1 2015/02/20 00:00:00
 * @author <baltzell@jlab.org>
 */
public class EcalPedestalCalculator extends Driver {

    private static final String dbTag = "online";
    private static final String dbTableName = "ecal_calibrations";
    private static final String rawCollectionName = "EcalReadoutHits";
    private static final String extraDataRelationsName = "EcalReadoutExtraDataRelations";

    private String histoNameFormat = "Ecal/Pedestals/Mode7/ped%3d";

    private static final int minimumStats = 1000;
    private static final DecimalFormat dbNumberFormat=new DecimalFormat("#.####");
    
    private static final String[] filenamesDAQ = { "fadc37.ped", "fadc39.ped" };
    private static final String filenameDB = "hpsDB.txt";

    // The number of samples used by FADCs to report an event's pedestal.
    private int nSamples = 4;

    private int runNumber = 0;
    private static final int runNumberMax = 9999;
    
    private boolean writeFileForDB = true;
    private boolean writeFileForDAQ = true;

    private DatabaseConditionsManager conditionsManager = null;
    private EcalConditions ecalConditions = null;

    AIDA aida = AIDA.defaultInstance();

    private int nDetectorChanges = 0;

    public EcalPedestalCalculator() {
    }

    @Override
    protected void startOfData() {
    }

    @Override
    public void endOfData() {
        Console cc = System.console();
        if (cc == null) {
            System.err.println("No console.");
            System.exit(1);
        }
      
        System.err.println("\n\n\n***************************************************************\n");
        String userInput="";
        String outputFilePrefix="";
        userInput=cc.readLine("Enter filename prefix, or just press RETURN ...");
        if (userInput==null || userInput.length()==0 || userInput=="") {
            String home=System.getenv().get("HOME");
            outputFilePrefix = home+"/EcalPedestals/EcalPedestalCalculator_"+runNumber+"_";
        } else {
            outputFilePrefix = userInput;
        }
        
        String date = new SimpleDateFormat("yyyy-MM-dd-hh-mm").format(new Date());
        outputFilePrefix += date+"_";

        if (writeFileForDAQ) writeFileForDAQ(outputFilePrefix);
        if (writeFileForDB)  writeFileForDB(outputFilePrefix);
        
        System.out.println("\n\n***************************************************************\n");
        userInput=cc.readLine(String.format("Enter 'YES' to write conditions database for run range [%s,%s] ...",runNumber,runNumberMax));
        System.out.println("***********"+userInput+"********");
        boolean uploadToDB=false;
        if (userInput!=null && userInput.equals("YES")) {
            userInput=cc.readLine("Really?");
            if (userInput!=null && userInput.equals("YES")) {
                uploadToDB=true;
            }
        }
        if (uploadToDB) {
            uploadToDB();
        } else {
            System.out.println("!!!!!!!!!!!!!!!!!!!!!!! Not Writing Database !!!!!!!!!!!!!!!!!!!!!!!!!!");
        }
    }

    @Override
    public void detectorChanged(Detector detector) {

        if (nDetectorChanges++ > 1) {
            throw new RuntimeException("No Detector Change Allowed.");
        }
        
        conditionsManager = DatabaseConditionsManager.getInstance();
        ecalConditions = conditionsManager.getEcalConditions();

        aida.tree().cd("/");
        for (EcalChannel cc : ecalConditions.getChannelCollection()) {
            aida.histogram1D(getHistoName(cc),181,19.5,200.5);
        }

    }

    private String getHistoName(EcalChannel cc) {
        return String.format(histoNameFormat,cc.getChannelId());
    }

    private void uploadToDB() {
        System.out.println(String.format("Uploading new pedestals to the database, runMin=%d, runMax=%d, tag=%s ....",
                runNumber,runNumberMax,dbTag));
      
        EcalCalibrationCollection calibrations = new EcalCalibrationCollection();
        TableMetaData tableMetaData = conditionsManager.findTableMetaData(dbTableName);
        calibrations.setTableMetaData(tableMetaData);
        
        for (int cid = 1; cid <= 442; cid++) {
            EcalChannel cc = findChannel(cid);
            IHistogram1D hh = aida.histogram1D(getHistoName(cc));
            if (hh.entries() < minimumStats) {
                System.err.println("Insufficient Statistics, Not writing to database.  (channel_id="+cid+").");
                return;
            }
            calibrations.add(new EcalCalibration(cid,
                    Double.valueOf(dbNumberFormat.format(hh.mean())),
                    Double.valueOf(dbNumberFormat.format(hh.rms()))));
        }

        int collectionId = conditionsManager.getNextCollectionID(dbTableName);
        try {
            calibrations.setCollectionId(collectionId);
            
            System.err.println("CollectionID:  "+collectionId);

            calibrations.insert();
            ConditionsRecord conditionsRecord = new ConditionsRecord(
                    calibrations.getCollectionId(), runNumber, runNumberMax, dbTableName, dbTableName, 
                    "Generated by EcalPedestalCalculator from Run #"+runNumber, dbTag);
            conditionsRecord.insert();
            
        } catch (ConditionsObjectException | SQLException e) {
            throw new RuntimeException(e);
        }
        
    }
    
    private void writeFileForDB(String outputFilePrefix) {
        System.out.println("\nWriting pedestal file for HPS Conditions Database:\n"
                + outputFilePrefix + filenameDB);
        FileWriter wout = null;
        try {
            wout = new FileWriter(outputFilePrefix + filenameDB);
            wout.write("ecal_channel_id pedestal noise\n");
            for (int cid = 1; cid <= 442; cid++) {
                EcalChannel cc = findChannel(cid);
                IHistogram1D hh = aida.histogram1D(getHistoName(cc));
                wout.write(String.format("%3d %7.3f %7.3f\n",cid,hh.mean(),
                        hh.rms() * Math.sqrt(nSamples)));
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

    private void writeFileForDAQ(String outputFilePrefix) {
        System.out.println("\nWriting pedestal files for Hall-B DAQ:\n" + outputFilePrefix
                + filenamesDAQ[0] + "\n" + outputFilePrefix + filenamesDAQ[1]);
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
                                hh.mean(),hh.rms() * Math.sqrt(nSamples)));
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
        runNumber = event.getRunNumber();
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
        if (max <= 0)
            return;
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
            if (crate == cc.getCrate() && slot == cc.getSlot() && chan == cc.getChannel()) {
                return cc;
            }
        }
        throw new RuntimeException(String.format(
                "Could not find channel:  (crate,slot,channel)=(%d,%d,%d)",crate,slot,chan));
    }

}
