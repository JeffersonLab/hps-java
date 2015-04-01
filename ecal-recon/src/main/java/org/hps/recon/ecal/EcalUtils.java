package org.hps.recon.ecal;

import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;

/**
 *
 * @author Sho Uemura <meeg@slac.stanford.edu>
 * @version $Id: ECalUtils.java,v 1.1 2013/02/25 22:39:24 meeg Exp $
 */
public class EcalUtils {

    public static final double GeV = 1.0;
    public static final double MeV = 0.001;
    //parameters for 2014 APDs and preamp
    public static final double riseTime = 10.0; //10 pulse rise time in ns
    public static final double fallTime = 17.0; //17 pulse fall time in ns
    public static final double lightYield = 120. / MeV; // number of photons per GeV
    public static final double quantumEff = 0.7;  // quantum efficiency of the APD
    public static final double surfRatio = (10. * 10.) / (16 * 16); // surface ratio between APD and crystals
    public static final double gainAPD = 150.; // Gain of the APD
    public static final double elemCharge = 1.60217657e-19;
    public static final double gainPreAmpl = 525e12; // Gain of the preamplifier in pC/pC, true value is higher but does not take into account losses
    public static final int nBit = 12;  //number of bits used by the fADC to code a value
    public static final double maxVolt = 2.0;   //maximum volt intput of the fADC
    public static final double Req = 1.0 / 27.5; // equivalent resistance of the amplification chain
    public static final double adcResolution = 2.0 / (Math.pow(2, nBit) - 1); //volts per ADC count
    public static final double readoutGain = Req * lightYield * quantumEff * surfRatio * gainAPD * gainPreAmpl * elemCharge;// = 15.0545 volt-seconds/GeV
    public static final double gainFactor = adcResolution / readoutGain;
    public static final double ecalReadoutPeriod = 4.0; // readout period in ns, it is hardcoded in the public declaration of EcalReadoutDriver. 

    /**
     * Returns the quadrant which contains the ECal cluster
     *
     * @param ecalCluster : ECal cluster
     * @return Quadrant number
     */
    public static int getQuadrant(Cluster ecalCluster) {
        return getQuadrant(ecalCluster.getCalorimeterHits().get(0));
    }

    public static int getQuadrant(CalorimeterHit hit) {
        int ix = hit.getIdentifierFieldValue("ix");
        int iy = hit.getIdentifierFieldValue("iy");
        return getQuadrant(ix, iy);
    }

    public static int getQuadrant(int x, int y) {
        if (x > 0) {
            if (y > 0) {
                return 1;
            } else {
                return 4;
            }
        } else {
            if (y > 0) {
                return 2;
            } else {
                return 3;
            }
        }
    }

    public static int getHVGroup(int x, int y) {
        int absy = Math.abs(y);
        if (x > 0 || x <= -8) {
            return (23 - Math.abs(x)) / 2 + 1;
        } else {
            if (x == -7 && absy == 5) {
                return 8;
            } else if (x >= -4) {
                return 12 - Math.max(x + 4, absy - 2);
            } else {
                return 12 - Math.max(-5 - x, absy - 2);
            }
        }
    }

    /**
     * This is a very basic method that, given an array with the raw-waveform (in FADC units), returns the amplitude (in mV)
     * @param data Array with data from FADC, in fadc units
     * @param lenght The array lenght
     * @param pedestalSamples How many samples at the beginning of the array to use for the pedestal. Must be < lenght
     * @return double[], 0 is the amplitude in mV, 1 is the offest in ADC counts, 2 is the RMS in adc counts
     */
    public static double[] computeAmplitude(short [] data, int lenght, int pedestalSamples){
        double amplitude,pedestal,noise;
        pedestal=0;
        noise=0;
        amplitude=data[0];
        double[] ret={0.,0.,0.};
        if (pedestalSamples>lenght){
            return ret;
        }
        for (int jj = 0; jj < lenght; jj++){
            if (jj<pedestalSamples){
                pedestal+=data[jj];
                noise+=data[jj]*data[jj];
            }
            if (data[jj]>amplitude) amplitude=data[jj];
        }
        pedestal/=pedestalSamples;
        noise/=pedestalSamples;
        noise=Math.sqrt(noise-pedestal*pedestal);
        amplitude-=pedestal;

        amplitude*=adcResolution*1000;
        ret[0]=amplitude;
        ret[1]=pedestal;
        ret[2]=noise;
        return ret;

    }

}
