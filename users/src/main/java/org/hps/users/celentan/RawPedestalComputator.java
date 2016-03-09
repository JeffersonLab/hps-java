package org.hps.users.celentan;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.ecal.EcalChannel;
import org.hps.conditions.ecal.EcalChannel.GeometryId;
import org.hps.conditions.ecal.EcalConditions;
import org.hps.recon.ecal.EcalUtils;
import org.lcsim.detector.identifier.IIdentifierHelper;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.hps.monitoring.ecal.plots.EcalMonitoringUtilities;



public class RawPedestalComputator extends Driver {

    String inputCollectionRaw = "EcalReadoutHits";
    int row, column;
    double energy;

    int[] windowRaw = new int[47 * 11];// in case we have the raw waveform, this is the window lenght (in samples)
    boolean[] isFirstRaw = new boolean[47 * 11];

    double[] pedestal = new double[47 * 11];
    double[] noise = new double[47 * 11];
    double[] result;

    int pedSamples = 50;
    int nEvents = 0;

    private EcalConditions conditions;
    private IIdentifierHelper helper;
    private int systemId;

    @Override
    public void detectorChanged(Detector detector) {

        DatabaseConditionsManager manager = DatabaseConditionsManager.getInstance();
        this.conditions = manager.getCachedConditions(EcalConditions.class, "ecal_conditions").getCachedData();
        this.helper = detector.getSubdetector("Ecal").getDetectorElement().getIdentifierHelper();
        this.systemId = detector.getSubdetector("Ecal").getSystemID();

        System.out.println("Pedestal computator: detector changed");
        for (int ii = 0; ii < 11 * 47; ii++) {
            isFirstRaw[ii] = true;
            pedestal[ii] = 0;
            noise[ii] = 0;
        }
    }

    @Override
    public void process(EventHeader event) {
        int ii = 0;
        if (event.hasCollection(RawTrackerHit.class, inputCollectionRaw)) {
            List<RawTrackerHit> hits = event.get(RawTrackerHit.class, inputCollectionRaw);
            for (RawTrackerHit hit : hits) {
                row = hit.getIdentifierFieldValue("iy");
                column = hit.getIdentifierFieldValue("ix");
                ii = EcalMonitoringUtilities.getHistoIDFromRowColumn(row, column);
                if ((row != 0) && (column != 0)) {
                    if (!EcalMonitoringUtilities.isInHole(row, column)) {
                        if (isFirstRaw[ii]) { // at the very first hit we read for this channel, we need to read the window length and save it
                            isFirstRaw[ii] = false;
                            windowRaw[ii] = hit.getADCValues().length;
                        }
                        result = EcalUtils.computeAmplitude(hit.getADCValues(), windowRaw[ii], pedSamples);
                        pedestal[ii] += result[1];
                        noise[ii] += result[2];
                    }
                }
            }
        }

        if (event.hasCollection(CalorimeterHit.class, "EcalCalHits")) {
            List<CalorimeterHit> hits = event.get(CalorimeterHit.class,"EcalCalHits");
            for (CalorimeterHit hit : hits) {
                column = hit.getIdentifierFieldValue("ix");
                row = hit.getIdentifierFieldValue("iy");
                energy = hit.getCorrectedEnergy();
                System.out.println("Row: "+row+" Column "+column+" Energy: "+energy);
            }
        }       


        nEvents++;
    }

    @Override
    public void endOfData() {
        try {
            PrintWriter writerTop = new PrintWriter("default01.ped", "UTF-8");
            PrintWriter writerBottom = new PrintWriter("default02.ped", "UTF-8");

            for (int ii = 0; ii < 11 * 47; ii++) {
                int row, column;
                row = EcalMonitoringUtilities.getRowFromHistoID(ii);
                column = EcalMonitoringUtilities.getColumnFromHistoID(ii);
                if (EcalMonitoringUtilities.isInHole(row, column))
                    continue;
                if ((row == 0) || (column == 0))
                    continue;
                pedestal[ii] /= nEvents;
                noise[ii] /= nEvents;

                // FIXME: Is this right? --JM
                EcalChannel ecalChannel = conditions.getChannelCollection().findChannel(new GeometryId(helper, new int[] {systemId, column, row}));
                int crate = ecalChannel.getCrate();
                int slot = ecalChannel.getSlot();
                int channel = ecalChannel.getChannel();

                System.out.println(column + " " + row + " " + crate + " " + slot + " " + channel + " " + pedestal[ii] + " " + noise[ii]);

                if (crate == 37) {
                    writerTop.print(slot + " " + channel + " " + (int) (Math.round(pedestal[ii])) + " " + (int) (Math.round(noise[ii])) + "\r\n");
                } else if (crate == 39) {
                    writerBottom.print(slot + " " + channel + " " + (int) (Math.round(pedestal[ii])) + " " + (int) (Math.round(noise[ii])) + "\r\n");
                }

            }

            writerTop.close();
            writerBottom.close();
        } catch (FileNotFoundException fnfe) {

            System.out.println(fnfe.getMessage());

        }

        catch (IOException ioe) {

            System.out.println(ioe.getMessage());

        }
    }
}
