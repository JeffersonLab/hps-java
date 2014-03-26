package org.lcsim.hps.users.gcharles;

import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;

import java.util.List;

import org.hps.readout.ecal.FADCEcalReadoutDriver;
import org.hps.readout.ecal.RingBuffer;
import org.hps.recon.ecal.ECalUtils;
import org.hps.recon.ecal.EcalRawConverterDriver;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.EventHeader;
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
    EcalRawConverterDriver converterDriver = new EcalRawConverterDriver();
//    IHistogram1D h1d_volt = aida.histogram1D("Volt", 100, 0.0, 2.2);
    IHistogram1D h1d_sumbuffer_en = aida.histogram1D("signal buffer energy", 1000, 0.0, 10);
//    IHistogram1D h1d_volt_tot = aida.histogram1D("Sum_volt", 100, 0.0, 100);
//    IHistogram1D h1d_en_tot = aida.histogram1D("Sum_en", 1000, 0.0, 100.0 * 4096.0 / 2.0/* * ECalUtils.ecalReadoutPeriod * ECalUtils.gainFactor*/);
//    IHistogram1D h1d_max = aida.histogram1D("Max", 100, 0.0, 6.0);
    IHistogram1D hitEnergyPlot = aida.histogram1D("Energy Plot", 1000, 0.0, 10);
    IHistogram1D hitCorEner = aida.histogram1D("CorEnergy", 1000, 0.0, 10);
    IHistogram2D h2d_ehit_ebuffer = aida.histogram2D("signal buffer energy vs. EcalHit energy", 1000, 0.0, 2.2, 1000, 0.0, 2.2);
//    final int ecal_NLayX = 46; // number of crystal layers in X direction 
//    final int ecal_NLayY = 11; // number of crystal layers in Y direction
//    final int decX = 23;       // for crystal index to start at 0
//    final int decY = 5;
//    public int temp_dec = 0;   // to take into account the fact that there is a line of 0 in the X direction;

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

        add(converterDriver);
        converterDriver.setRawCollectionName("EcalRawHits");
        converterDriver.setEcalCollectionName("EcalCorrectedHits");
        converterDriver.setGain(1.0);
        converterDriver.setUse2014Gain(true);

        super.startOfData();
    }

    @Override
    public void process(EventHeader event) {
        super.process(event);
        if (event.hasCollection(CalorimeterHit.class, "EcalHits")) {
            List<CalorimeterHit> hits = event.get(CalorimeterHit.class, "EcalHits");

//            double sumCrys_sig = 0;   // sum of the fADC values for all crystals  
//            double sumCrys_en = 0;    // sum of the converted fADC values for all crystals 
//            double max = 0;

            for (CalorimeterHit hit : hits) {
                hitEnergyPlot.fill(hit.getRawEnergy());

                RingBuffer signalBuffer = readoutDriver.getSignalMap().get(hit.getCellID());

//                double sumBuffer_sig = 0;
                double sumBuffer_en = 0;  // sum of the converted fADC values contained in the buffer

                for (int i = 0; i < signalBuffer.getLength(); i++) {

//                    sumBuffer_sig += signalBuffer.getValue(i);
//                    if (0.1 < signalBuffer.getValue(i)) {
//                        h1d_volt.fill(signalBuffer.getValue(i));
//                    }
                    int temp = Math.min((int) (Math.round(signalBuffer.getValue(i) * (Math.pow(2, ECalUtils.nBit) - 1) / ECalUtils.maxVolt)), (int) Math.pow(2, ECalUtils.nBit));

                    sumBuffer_en += temp * ECalUtils.gainFactor * ECalUtils.ecalReadoutPeriod;

//       if(23<temp) System.out.println(pipeBuffer.getValue(i)-pedestal + "   " + signalBuffer.getValue(i) + "    " + temp);         	

//                	if(event.getEventNumber()==293) System.out.println(hit.getIdentifierFieldValue("ix") + "  " + hit.getIdentifierFieldValue("iy") + "    " + hit.getRawEnergy() + "    " + pipeBuffer.getValue(i));
//                	if(293==event.getEventNumber()) System.out.println(pipeBuffer.getValue(i));
                }
                /*if(5<temp) */ h1d_sumbuffer_en.fill(sumBuffer_en);
                h2d_ehit_ebuffer.fill(hit.getRawEnergy(), sumBuffer_en);
            }
        }

        if (event.hasCollection(CalorimeterHit.class, "EcalCorrectedHits")) {
            List<CalorimeterHit> hits_cor = event.get(CalorimeterHit.class, "EcalCorrectedHits");

            for (CalorimeterHit hit : hits_cor) {
                hitCorEner.fill(hit.getRawEnergy());
            }
        }

//        System.out.println("Coucou "+event.getEventNumber());   
//
//        if (event.hasCollection(CalorimeterHit.class, "EcalCorrectedHits")) {
//            List<CalorimeterHit> hits_cor = event.get(CalorimeterHit.class, "EcalCorrectedHits");
//
//            int next = 0;
//            int ind = 0;
//            int indTot = 0;
//            for (CalorimeterHit hit : hits_cor) {
//                if (timePrev - 2 < hit.getTime() && hit.getTime() < timePrev + 2) {
//                    ind++;
//                }
//                indTot++;
//            }
//
//            if (event.getEventNumber() % 2 == 1 && ind == 0) { // it's a new event and all the hits in the event are at the same time
//
//
//                //--A new event starts
//                //--Now sums everything and fills histograms 
//                if (veryFirst == false) {
//                    for (int j = 0; j < ecal_NLayX; j++) {
//                        for (int k = 0; k < ecal_NLayY; k++) {
//                            if (0.005 < ecal_Ecrys_fin[j][k]) {
//                                sumCrys_fin += ecal_Ecrys_fin[j][k];
//                            }
//                        }
//                    }
//
//                    if (0.01 < sumCrys_fin /*&& 250<max*/) { // 250<max for 2 GeV, 1000 for 6 GeV
//                        hitEnergyPlot.fill(sumCrys_fin);
//                    }
//                }
//                //--
//
//                //--Can now start a new event and initialize
//                veryFirst = false;
//                sumCrys_fin = 0;
//                for (int j = 0; j < ecal_NLayX; j++) {
//                    for (int k = 0; k < ecal_NLayY; k++) {
//                        ecal_Ecrys_fin[j][k] = 0;
//                    }
//                }
//                next = 0;
//                for (CalorimeterHit hit : hits_cor) {
//                    if (next > ind) {
//                        if (hit.getIdentifierFieldValue("ix") > 0) {
//                            temp_dec = -1; // 0 is not included in the x values
//                        }
//                        if (0.01 < hit.getRawEnergy()) {
//                            ecal_Ecrys_fin[hit.getIdentifierFieldValue("ix") + decX + temp_dec][hit.getIdentifierFieldValue("iy") + decY] += hit.getRawEnergy();
//                        }
//                        timePrev = hit.getTime();
//                        temp_dec = 0;
//                    }
//                    next++;
//                }
//                //--
//
//            } // if odd && ind==0
//            else if (event.getEventNumber() % 2 == 1 && ind == indTot) { // All the hits in this event are at the same time than the previous event               
//
//                // it's still the same event, add it to the previous one            
//                for (CalorimeterHit hit : hits_cor) {
//                    if (hit.getIdentifierFieldValue("ix") > 0) {
//                        temp_dec = -1; // 0 is not included in the x values
//                    }
//                    if (0.01 < hit.getRawEnergy()) {
//                        ecal_Ecrys_fin[hit.getIdentifierFieldValue("ix") + decX + temp_dec][hit.getIdentifierFieldValue("iy") + decY] += hit.getRawEnergy();
//                    }
//                    temp_dec = 0;
//                }
//            } // else if(event.getEventNumber()%2==1 && ind==indTot   
//            else if (event.getEventNumber() % 2 == 1 && ind < indTot) { // ind<indTot means some hits are with the previous event but there is also another event
//
//                next = 0;
//                for (CalorimeterHit hit : hits_cor) {
//                    if (next < ind) {
//                        if (hit.getIdentifierFieldValue("ix") > 0) {
//                            temp_dec = -1; // 0 is not included in the x values
//                        }
//                        if (0.01 < hit.getRawEnergy()) {
//                            ecal_Ecrys_fin[hit.getIdentifierFieldValue("ix") + decX + temp_dec][hit.getIdentifierFieldValue("iy") + decY] += hit.getRawEnergy();
//                        }
//                        timePrev = hit.getTime();
//                        temp_dec = 0;
//                    }
//                    next++;
//                }
//
//                //--A new event starts
//                //--Now sums everything, fills histograms 
//                for (int j = 0; j < ecal_NLayX; j++) {
//                    for (int k = 0; k < ecal_NLayY; k++) {
//                        if (0.03 < ecal_Ecrys_fin[j][k]) {
//                            sumCrys_fin += ecal_Ecrys_fin[j][k];
//                        }
//                    }
//                }
//
//                if (0.01 < sumCrys_fin /*&& 250<max*/) { // 250<max for 2 GeV, 1000 for 6 GeV
//                    hitEnergyPlot.fill(sumCrys_fin);
//                }
//                //--
//
//                //--Can now start a new event and initialize
//                sumCrys_fin = 0;
//                for (int j = 0; j < ecal_NLayX; j++) {
//                    for (int k = 0; k < ecal_NLayY; k++) {
//                        ecal_Ecrys_fin[j][k] = 0;
//                    }
//                }
//                next = 0;
//                for (CalorimeterHit hit : hits_cor) {
//                    if (next > ind) {
//                        if (hit.getIdentifierFieldValue("ix") > 0) {
//                            temp_dec = -1; // 0 is not included in the x values
//                        }
//                        if (0.01 < hit.getRawEnergy()) {
//                            ecal_Ecrys_fin[hit.getIdentifierFieldValue("ix") + decX + temp_dec][hit.getIdentifierFieldValue("iy") + decY] += hit.getRawEnergy();
//                        }
//                        timePrev = hit.getTime();
//                        temp_dec = 0;
//                    }
//                    next++;
//                }
//                //--
//
//
//
//            } // event.getEventNumber()%2==1 && ind<indTot    	
//
//        } // event.hasCollection(CalorimeterHit.class, "EcalCorrectedHits")
//
//
//
//
//
//        if (event.hasCollection(CalorimeterHit.class, "EcalHits")) {
//            List<CalorimeterHit> hits = event.get(CalorimeterHit.class, "EcalHits");
//
//            double ecal_Ecrys_sig[][] = new double[ecal_NLayX][ecal_NLayY];
//            double ecal_Ecrys_en[][] = new double[ecal_NLayX][ecal_NLayY];
//
//            // Initialization
//            for (int j = 0; j < ecal_NLayX; j++) {
//                for (int k = 0; k < ecal_NLayY; k++) {
//                    ecal_Ecrys_sig[j][k] = 0;
//                    ecal_Ecrys_en[j][k] = 0;
//                }
//            }
//
//            double sumCrys_sig = 0;   // sum of the fADC values for all crystals  
//            double sumCrys_en = 0;    // sum of the converted fADC values for all crystals 
//            double max = 0;
//
//            for (CalorimeterHit hit : hits) {
//
////                int pedestal = (int) Math.round(EcalConditions.physicalToPedestal(hit.getCellID()));
//                RingBuffer signalBuffer = readoutDriver.getSignalMap().get(hit.getCellID());
////   System.out.println(pipeBuffer.getLength());
//
//                double sumBuffer_sig = 0;
//                double sumBuffer_en = 0;  // sum of the converted fADC values contained in the buffer
//                int temp = 0;
//
//                for (int i = 0; i < signalBuffer.getLength(); i++) {
//
//                    sumBuffer_sig += signalBuffer.getValue(i);
//                    if (0.1 < signalBuffer.getValue(i)) {
//                        h1d_volt.fill(signalBuffer.getValue(i));
//                    }
//                    temp = Math.min((int) (Math.round(signalBuffer.getValue(i) * (Math.pow(2, ECalUtils.nBit) - 1) / ECalUtils.maxVolt)), (int) Math.pow(2, ECalUtils.nBit));
//
//                    sumBuffer_en += temp * ECalUtils.gainFactor * ECalUtils.ecalReadoutPeriod;
//
////       if(23<temp) System.out.println(pipeBuffer.getValue(i)-pedestal + "   " + signalBuffer.getValue(i) + "    " + temp);         	
//
////                	if(event.getEventNumber()==293) System.out.println(hit.getIdentifierFieldValue("ix") + "  " + hit.getIdentifierFieldValue("iy") + "    " + hit.getRawEnergy() + "    " + pipeBuffer.getValue(i));
////                	if(293==event.getEventNumber()) System.out.println(pipeBuffer.getValue(i));
//                }
//System.out.println(sumBuffer_en);
//                /*if(5<temp) */                h1d_adc.fill(sumBuffer_en);
//
//                if (hit.getIdentifierFieldValue("ix") > 0) {
//                    temp_dec = -1; // 0 is not included in the x values
//                }
//                ecal_Ecrys_sig[hit.getIdentifierFieldValue("ix") + decX + temp_dec][hit.getIdentifierFieldValue("iy") + decY] += sumBuffer_sig;
//                ecal_Ecrys_en[hit.getIdentifierFieldValue("ix") + decX + temp_dec][hit.getIdentifierFieldValue("iy") + decY] += sumBuffer_en;
//                temp_dec = 0;
////                if(29<event.getEventNumber() && event.getEventNumber()<31) System.out.println(hit.getIdentifierFieldValue("ix")+decX+temp_dec+ " " + hit.getIdentifierFieldValue("iy")+decY + " " + sumBuffer);
//            } // hits
//
//            for (int j = 0; j < ecal_NLayX; j++) {
//                for (int k = 0; k < ecal_NLayY; k++) {
//                    if (0.1 < ecal_Ecrys_sig[j][k]) {
//                        sumCrys_sig += ecal_Ecrys_sig[j][k];
//                        sumCrys_en += ecal_Ecrys_en[j][k];
//                        if (max < ecal_Ecrys_en[j][k]) {
//                            max = ecal_Ecrys_en[j][k];
//                        }
//                    }
//
////        		    if(30<event.getEventNumber() && event.getEventNumber()<32) System.out.println(j+ " " + k + " " + ecal_Ecrys[j][k] + " " + sumCrys);
//                }
//            }
//
//            if (0.1 < sumCrys_sig /*&& 250<max*/) { // 250<max for 2 GeV, 1000 for 6 GeV
//                h1d_volt_tot.fill(sumCrys_sig);
//                h1d_en_tot.fill(sumCrys_en);
//            }
//
//            if (0.1 < max) {
//                h1d_max.fill(max);
//            }
//
//
//        } // if(event.hasCollection)        


    } // event
}