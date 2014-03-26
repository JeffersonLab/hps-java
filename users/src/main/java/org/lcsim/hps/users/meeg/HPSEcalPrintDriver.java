package org.lcsim.hps.users.meeg;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import org.hps.readout.ecal.ClockSingleton;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.EventHeader;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.IDDecoder;
import org.lcsim.geometry.subdetector.HPSEcal3;
import org.lcsim.util.Driver;

/**
 * Performs readout of ECal hits.
 *
 * @author Sho Uemura <meeg@slac.stanford.edu>
 * @version $Id: HPSEcalPrintDriver.java,v 1.1 2012/04/29 02:10:05 meeg Exp $
 */
public class HPSEcalPrintDriver extends Driver {

    HPSEcal3 ecal;
    IDDecoder dec;
    String ecalCollectionName;
    String ecalName;
    String ecalReadoutName = "EcalHits";
    String outputFilePath;
    PrintWriter outputStream = null;
    int nx, ny;

    public void setEcalReadoutName(String ecalReadoutName) {
        this.ecalReadoutName = ecalReadoutName;
    }

    public void setEcalCollectionName(String ecalCollectionName) {
        this.ecalCollectionName = ecalCollectionName;
    }

    public void setEcalName(String ecalName) {
        this.ecalName = ecalName;
    }

    public void setOutputFilePath(String outputFilePath) {
        this.outputFilePath = outputFilePath;
    }

    public void startOfData() {
        if (ecalCollectionName == null)
            throw new RuntimeException("The parameter ecalCollectionName was not set!");

        if (ecalName == null)
            throw new RuntimeException("The parameter ecalName was not set!");

        try {
            outputStream = new PrintWriter(outputFilePath);
        } catch (IOException ex) {
            throw new RuntimeException("Invalid outputFilePath!");
        }
    }

    public void endOfData() {
        if (outputStream != null)
            outputStream.close();
    }

    public void detectorChanged(Detector detector) {
        // Get the Subdetector.
        ecal = (HPSEcal3) detector.getSubdetector(ecalName);
        dec = ecal.getIDDecoder();
        nx = (int) ecal.nx();
        ny = (int) ecal.ny();
    }

    public void process(EventHeader event) {
        //System.out.println(this.getClass().getCanonicalName() + " - process");
        // Get the list of ECal hits.
        List<CalorimeterHit> hits = event.get(CalorimeterHit.class, ecalCollectionName);

        if (hits == null)
            throw new RuntimeException("Event is missing ECal hits collection!");

        if (!hits.isEmpty()) {
            double hitArray[][] = new double[nx][2 * ny];
            for (CalorimeterHit hit : hits) {
                dec.setID(hit.getCellID());
                int ix = dec.getValue("ix");
                int iy = dec.getValue("iy");
                if (nx % 2 == 0) {
                    hitArray[ix > 0 ? nx / 2 + ix - 1 : nx / 2 + ix][iy > 0 ? ny + iy - 1 : ny + iy] += hit.getRawEnergy();
                } else {
                    hitArray[ix > 0 ? nx / 2 + ix : nx / 2 + ix][iy > 0 ? ny + iy - 1 : ny + iy] += hit.getRawEnergy();
                }
            }

            outputStream.printf("Event %d\n", ClockSingleton.getClock());
            for (int x = 0; x < nx; x++) {
                for (int y = 0; y < 2 * ny; y++) {
                    if (hitArray[x][y] == 0)
                        outputStream.printf("0\t");
                    else
                        outputStream.printf("%4.3f\t", hitArray[x][y]);
                }
                outputStream.println();
            }
        }
    }
}