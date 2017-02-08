/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hps.users.meeg;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.EventHeader;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.IDDecoder;
import org.lcsim.geometry.Subdetector;
import org.lcsim.util.Driver;

public class HPSEcalAnalogPrintDriver extends Driver {

    Subdetector ecal;
    IDDecoder dec;
    String ecalName;
    String ecalReadoutName = "EcalHits";
    String ecalCollectionName = null;
    String outputFileName;
    PrintWriter outputStream = null;
    int flags;

    public HPSEcalAnalogPrintDriver() {
    }

    public void setEcalCollectionName(String ecalCollectionName) {
        this.ecalCollectionName = ecalCollectionName;
    }

    public void setEcalName(String ecalName) {
        this.ecalName = ecalName;
    }

    public void setOutputFileName(String outputFileName) {
        this.outputFileName = outputFileName;
    }

    @Override
    public void startOfData() {
        if (ecalCollectionName == null) {
            throw new RuntimeException("The parameter ecalCollectionName was not set!");
        }

        if (ecalName == null) {
            throw new RuntimeException("The parameter ecalName was not set!");
        }

        if (outputFileName != null) {
            try {
                outputStream = new PrintWriter(outputFileName);
            } catch (IOException ex) {
                throw new RuntimeException("Invalid outputFilePath!");
            }
        } else {
            outputStream = new PrintWriter(System.out, true);
        }
    }

    public void detectorChanged(Detector detector) {
        // Get the Subdetector.
        ecal = (Subdetector) detector.getSubdetector(ecalName);
        dec = ecal.getIDDecoder();
    }

    @Override
    public void process(EventHeader event) {
        // Get the list of ECal hits.
        if (event.hasCollection(CalorimeterHit.class, ecalCollectionName)) {
            //outputStream.println("Reading RawTrackerHits from event " + event.getEventNumber());
            List<CalorimeterHit> hits = event.get(CalorimeterHit.class, ecalCollectionName);
            for (CalorimeterHit hit : hits) {
                dec.setID(hit.getCellID());
                outputStream.printf("%d\t%d\t%f\t%f\n", dec.getValue("ix"), dec.getValue("iy"), hit.getTime(), hit.getRawEnergy());
            }
        }
    }
}
