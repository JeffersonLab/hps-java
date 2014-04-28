package org.hps.users.gcharles;

import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IProfile2D;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
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
    IHistogram1D hSignalBuffer = aida.histogram1D("Signal buffer energy", 1000, 0.0, 10);

    IHistogram1D hEnergyPlot = aida.histogram1D("Energy Plot", 500, 0.0, 10);
    IHistogram1D htotEnergy = aida.histogram1D("Energy tot", 500, 0.0, 10);
    IHistogram1D hnHits = aida.histogram1D("nHits", 101, -0.5, 100.5);
    IHistogram1D hnHitsCor = aida.histogram1D("nHitsCor", 101, -0.5, 100.5);
    IHistogram1D hCorEnergy = aida.histogram1D("CorEnergy", 500, 0.0, 10);
    IHistogram1D hCorEnergyTot = aida.histogram1D("CorEnergyTot", 500, 0.0, 10);
    IHistogram2D h2d_ehit_ebuffer = aida.histogram2D("signal buffer energy vs. EcalHit energy", 1000, 0.0, 6.6, 1000, 0.0, 10.6);
    IProfile2D h2d_pos_vs_R = aida.profile2D("Pos vs Ratio", 49,-24,24,12,-5.5,5.55);
    IHistogram2D h2d_pos = aida.histogram2D("Pos",49,-24,24,12,-5.5,5.5);
    IProfile2D h3d_pos_vs_R = aida.profile2D("Pos vs Ratio hist",49,0,49,12,-0.5,10.5);

    
    double sumEner_cor =0;
            int nHitsCor=0;
    
    public int nFile=0;
        //on va chercher le chemin et le nom du fichier et on me tout ca dans un String
        String adressedufichier = "output.txt";


    @Override
    public void startOfData() {

        add(readoutDriver);
        readoutDriver.setCoincidenceWindow(1);
        readoutDriver.setEcalName("Ecal");
        readoutDriver.setEcalCollectionName("EcalHits");
        readoutDriver.setEcalRawCollectionName("EcalRawHits");
        readoutDriver.setConstantTriggerWindow(true);
        readoutDriver.setScaleFactor(1);
        readoutDriver.setFixedGain(1);

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

        double phi=-4;
        double cosTheta=-2;
        double max=0;
        int cx=-300;
        int cy=-300;


//--


        if (event.hasCollection(CalorimeterHit.class, "EcalHits")) {
            List<CalorimeterHit> hits = event.get(CalorimeterHit.class, "EcalHits");

//            double sumCrys_sig = 0;   // sum of the fADC values for all crystals
//            double sumCrys_en = 0;    // sum of the converted fADC values for all crystals
//            double max = 0;
              double totEnergy=0;
              int nHits=0;
            for (CalorimeterHit hit : hits) {
                hEnergyPlot.fill(hit.getRawEnergy());
                if(0.05<hit.getRawEnergy()){
                        totEnergy+=hit.getRawEnergy();
                    nHits++;
                }


                RingBuffer signalBuffer = readoutDriver.getSignalMap().get(hit.getCellID());

//                double sumBuffer_sig = 0;
                double sumBuffer_en = 0;  // sum of the converted fADC values contained in the buffer

                for (int i = 0; i < signalBuffer.getLength(); i++) {

//                    sumBuffer_sig += signalBuffer.getValue(i);
//                    if (0.1 < signalBuffer.getValue(i)) {
//                        h1d_volt.fill(signalBuffer.getValue(i));
//                    }
                        hSignalBuffer.fill(signalBuffer.getValue(i));
                    int temp = Math.min((int) (Math.round(signalBuffer.getValue(i) * (Math.pow(2, ECalUtils.nBit) - 1) / ECalUtils.maxVolt)), (int) Math.pow(2, ECalUtils.nBit));

                    sumBuffer_en += temp * ECalUtils.gainFactor * ECalUtils.ecalReadoutPeriod;

//       if(23<temp) System.out.println(pipeBuffer.getValue(i)-pedestal + "   " + signalBuffer.getValue(i) + "    " + temp);

//                      if(event.getEventNumber()==293) System.out.println(hit.getIdentifierFieldValue("ix") + "  " + hit.getIdentifierFieldValue("iy") + "    " + hit.getRawEnergy() + "    " + pipeBuffer.getValue(i));
//                      if(293==event.getEventNumber()) System.out.println(pipeBuffer.getValue(i));
                }
//              h1d_sumbuffer_en.fill(signalBuffer.getValue(i));
                h2d_ehit_ebuffer.fill(hit.getRawEnergy(), sumBuffer_en);

            }
            htotEnergy.fill(totEnergy);
            if(0<nHits) hnHits.fill(nHits);
        }

//--Corrected hits
        if (event.hasCollection(CalorimeterHit.class, "EcalCorrectedHits")) {

            List<CalorimeterHit> hits_cor = event.get(CalorimeterHit.class, "EcalCorrectedHits");

            for (CalorimeterHit hit : hits_cor) {

                double[] temp = new double[2];
                temp = hit.getPosition();
 //             System.out.println(event.getEventNumber() + "   " + temp[0] + "   " + temp[1]);



                hCorEnergy.fill(hit.getRawEnergy());
                //if(0.010<hit.getRawEnergy() && hit.getRawEnergy()<4.5){
                        sumEner_cor+=hit.getRawEnergy();
                        nHitsCor++;
                    if(max<hit.getRawEnergy()){
                            max=hit.getRawEnergy();
                            cx=hit.getIdentifierFieldValue("ix");
                            cy=hit.getIdentifierFieldValue("iy");
                    }
                //}
            }
}
        if (event.hasCollection(CalorimeterHit.class, "EcalHits")) {

            if(0<nHitsCor) hnHitsCor.fill(nHitsCor);
//            if(0.02<sumEner_cor) hCorEnergyTot.fill(sumEner_cor);
            if(0<nHitsCor && -300<cx && 0.2<sumEner_cor){
                h2d_pos_vs_R.fill(cx,cy,100.0*sumEner_cor/6.0);
                int tempDec=0;
                if(cx>0) tempDec=-1;
//                ECalUtils.avEcrys[cx+ECalUtils.decX+tempDec][cy+ECalUtils.decY]+=100.0*sumEner_cor/6.0;
//                ECalUtils.nEcrys[cx+ECalUtils.decX+tempDec][cy+ECalUtils.decY]++;
            }

            hCorEnergyTot.fill(sumEner_cor);
            sumEner_cor = 0;
            nHitsCor = 0;
        }
        if(-300<cy) h2d_pos.fill(cx,cy);
//--


//        if(event.getEventNumber()>2998 && nFile>20){
//            for(int cxx=0;cxx<ECalUtils.ecal_NLayX;cxx++){
//                    for(int cyy=0;cyy<ECalUtils.ecal_NLayY;cyy++){
//                            if(40<ECalUtils.avEcrys[cxx][cyy]/ECalUtils.nEcrys[cxx][cyy] && ECalUtils.avEcrys[cxx][cyy]/ECalUtils.nEcrys[cxx][cyy]<80)h3d_pos_vs_R.fill(cxx,cyy,ECalUtils.avEcrys[cxx][cyy]/ECalUtils.nEcrys[cxx][cyy]);
//                    }
//            }
//        }

    if(0<event.getEventNumber() && event.getEventNumber()<2){
        nFile++;
    }

    } // event


}
