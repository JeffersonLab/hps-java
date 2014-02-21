package org.lcsim.hps.users.gcharles;

import hep.aida.IHistogram1D;

import java.util.List;

import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.EventHeader;
import org.lcsim.hps.readout.ecal.FADCEcalReadoutDriver;
import org.lcsim.hps.readout.ecal.FADCEcalReadoutDriver.FADCPipeline;
import org.lcsim.hps.recon.ecal.EcalConditions;
import org.lcsim.hps.util.RingBuffer;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 * Saves histograms of FADC signal buffers before and after hits.
 *
 * @author Sho Uemura <meeg@slac.stanford.edu>
 * @version $Id: $
 */
public class FADCSignalAnalysis extends Driver {

    private AIDA aida = AIDA.defaultInstance();
    FADCEcalReadoutDriver readoutDriver = new FADCEcalReadoutDriver();
    IHistogram1D h1d = aida.histogram1D("Pipeline", 4096, -0.5, 4096 - 0.5);
    IHistogram1D h1d_2 = aida.histogram1D("ECrys", 4096 * 100, 0.0, 4096 * 100);
    IHistogram1D h1d_3 = aida.histogram1D("Sum", 200, 0.0, 102400);
    IHistogram1D h1d_4 = aida.histogram1D("Max", 500, 0.0, 4096);
    IHistogram1D h1d_5 = aida.histogram1D("Sum_sig", 200, 0.0, 50);
    final int ecal_NLayX = 46; // number of crystal layers in X direction 
    final int ecal_NLayY = 11; // number of crystal layers in Y direction
    final int decX = 23;       // for crystal index to start at 0
    final int decY = 5;
    private boolean plotPulseShapes = false;

    public void setPlotPulseShapes(boolean plotPulseShapes) {
        this.plotPulseShapes = plotPulseShapes;
    }

    @Override
    public void startOfData() {

        add(readoutDriver);
        readoutDriver.setCoincidenceWindow(2);
        readoutDriver.setEcalName("Ecal");
        readoutDriver.setEcalCollectionName("EcalHits");
        readoutDriver.setEcalRawCollectionName("EcalRawHits");
        readoutDriver.setConstantTriggerWindow(true);
        readoutDriver.setScaleFactor(1);
        readoutDriver.setFixedGain(1);
        readoutDriver.setUseCRRCShape(false);
        super.startOfData();
    }

    @Override
    public void process(EventHeader event) {
        if (plotPulseShapes && event.hasCollection(CalorimeterHit.class, "EcalHits")) {
            List<CalorimeterHit> hits = event.get(CalorimeterHit.class, "EcalHits");

            for (CalorimeterHit hit : hits) {
                hit.getCellID();
                RingBuffer signalBuffer = readoutDriver.getSignalMap().get(hit.getCellID());
                String name = String.format("pipeline x=%d, y=%d before hit in event %d, time %f, energy %f", hit.getIdentifierFieldValue("ix"), hit.getIdentifierFieldValue("iy"), event.getEventNumber(), hit.getTime(), hit.getRawEnergy());
                IHistogram1D hist = aida.histogram1D(name, signalBuffer.getLength(), -0.5, signalBuffer.getLength() - 0.5);
                for (int i = 0; i < signalBuffer.getLength(); i++) {
                    hist.fill(i, signalBuffer.getValue(i));
                }
            }
        }

        super.process(event);
//        System.out.println("Coucou"+event.getEventNumber());


        if (event.hasCollection(CalorimeterHit.class, "EcalHits")) {
            List<CalorimeterHit> hits = event.get(CalorimeterHit.class, "EcalHits");

            int temp_dec = 0;   // to take into account the fact that there is a line of 0 in the X direction
            int ecal_Ecrys[][] = new int[ecal_NLayX][ecal_NLayY];
            // Initialization
            for (int j = 0; j < ecal_NLayX; j++) {
                for (int k = 0; k < ecal_NLayY; k++) {
                    ecal_Ecrys[j][k] = 0;
                }
            }
            float ecal_Ecrys_sig[][] = new float[ecal_NLayX][ecal_NLayY];
            // Initialization
            for (int j = 0; j < ecal_NLayX; j++) {
                for (int k = 0; k < ecal_NLayY; k++) {
                    ecal_Ecrys_sig[j][k] = 0;
                }
            }
            int sumCrys = 0;    // sum of the fADC values for all crystals 
            float sumCrys_sig = 0;
            int max = 0;

            for (CalorimeterHit hit : hits) {

                FADCPipeline pipeBuffer = readoutDriver.getPipelineMap().get(hit.getCellID());
                int pedestal = (int) Math.round(EcalConditions.physicalToPedestal(hit.getCellID()));
                RingBuffer signalBuffer = readoutDriver.getSignalMap().get(hit.getCellID());
//   System.out.println(pipeBuffer.getLength());
                int sumBuffer = 0;  // sum of the fADC values contained in the buffer
                float sumBuffer_sig = 0;
                int temp;
                for (int i = 0; i < signalBuffer.getLength(); i++) {

                    sumBuffer += pipeBuffer.getValue(i) - pedestal;
                    sumBuffer_sig += signalBuffer.getValue(i);
                    temp = Math.min((int) Math.round(signalBuffer.getValue(i) * (int) ((Math.pow(2, 12) - 1) / 2.0)), (int) Math.pow(2, 12));
                    if (23 < temp) {
                        System.out.println(pipeBuffer.getValue(i) - pedestal + "   " + signalBuffer.getValue(i) + "    " + temp);
                    }

                    if (max < pipeBuffer.getValue(i) - pedestal) {
                        max = pipeBuffer.getValue(i) - pedestal;
                    }
//                	if(event.getEventNumber()==293) System.out.println(hit.getIdentifierFieldValue("ix") + "  " + hit.getIdentifierFieldValue("iy") + "    " + hit.getRawEnergy() + "    " + pipeBuffer.getValue(i));
                    h1d.fill(pipeBuffer.getValue(i) - pedestal);
                    if (4096 < pipeBuffer.getValue(i) - pedestal) {
                        System.out.println("pipeBuffer value too high.");
                    }
//                	if(293==event.getEventNumber()) System.out.println(pipeBuffer.getValue(i));
                }

                if (hit.getIdentifierFieldValue("ix") > 0) {
                    temp_dec = -1; // 0 is not included in the x values
                }
                ecal_Ecrys[hit.getIdentifierFieldValue("ix") + decX + temp_dec][hit.getIdentifierFieldValue("iy") + decY] += sumBuffer;
                ecal_Ecrys_sig[hit.getIdentifierFieldValue("ix") + decX + temp_dec][hit.getIdentifierFieldValue("iy") + decY] += sumBuffer_sig;
                if (4096 * 100 < sumBuffer) {
                    System.out.println("sumBuffer too high " + sumBuffer + "  " + event.getEventNumber());
                }
                temp_dec = 0;
//                if(29<event.getEventNumber() && event.getEventNumber()<31) System.out.println(hit.getIdentifierFieldValue("ix")+decX+temp_dec+ " " + hit.getIdentifierFieldValue("iy")+decY + " " + sumBuffer);
            } // hits

            for (int j = 0; j < ecal_NLayX; j++) {
                for (int k = 0; k < ecal_NLayY; k++) {
                    if (150 < ecal_Ecrys[j][k]) {
                        sumCrys += ecal_Ecrys[j][k];
                        sumCrys_sig += ecal_Ecrys_sig[j][k];
                    }
                    h1d_2.fill(ecal_Ecrys[j][k]);
                    if (4096 * 100 < ecal_Ecrys[j][k]) {
                        System.out.println("C'est quoi ce bazard ?");
                    }
//        		    if(30<event.getEventNumber() && event.getEventNumber()<32) System.out.println(j+ " " + k + " " + ecal_Ecrys[j][k] + " " + sumCrys);
                }
            }

            if (0 < sumCrys && 250 < max) {
                h1d_3.fill(sumCrys); // 250<max for 2 GeV, 1000 for 6 GeV
                h1d_5.fill(sumCrys_sig);
            }
            h1d_4.fill(max);
//            System.out.println(event.getEventNumber() + " " + sumCrys); 


        } // if(event.hasCollection)        

        if (plotPulseShapes && event.hasCollection(CalorimeterHit.class, "EcalHits")) {
            List<CalorimeterHit> hits = event.get(CalorimeterHit.class, "EcalHits");

            for (CalorimeterHit hit : hits) {
                hit.getCellID();
                RingBuffer signalBuffer = readoutDriver.getSignalMap().get(hit.getCellID());
                String name = String.format("pipeline x=%d, y=%d after hit in event %d, time %f, energy %f", hit.getIdentifierFieldValue("ix"), hit.getIdentifierFieldValue("iy"), event.getEventNumber(), hit.getTime(), hit.getRawEnergy());
                IHistogram1D hist = aida.histogram1D(name, signalBuffer.getLength(), -0.5, signalBuffer.getLength() - 0.5);
                for (int i = 0; i < signalBuffer.getLength(); i++) {
                    hist.fill(i, signalBuffer.getValue(i));
                }
            }
        }
    } // event
}
